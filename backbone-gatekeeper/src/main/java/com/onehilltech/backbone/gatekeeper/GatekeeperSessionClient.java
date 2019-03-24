package com.onehilltech.backbone.gatekeeper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import com.onehilltech.backbone.data.DataStore;
import com.onehilltech.backbone.data.HttpError;
import com.onehilltech.backbone.data.Resource;
import com.onehilltech.backbone.data.ResourceSerializer;
import com.onehilltech.backbone.gatekeeper.http.JsonAccount;
import com.onehilltech.backbone.gatekeeper.http.JsonAuthenticate;
import com.onehilltech.backbone.gatekeeper.http.JsonBearerToken;
import com.onehilltech.backbone.gatekeeper.http.JsonChangePassword;
import com.onehilltech.backbone.gatekeeper.http.JsonGrant;
import com.onehilltech.backbone.gatekeeper.http.JsonPassword;
import com.onehilltech.backbone.gatekeeper.http.JsonRefreshToken;
import com.onehilltech.backbone.gatekeeper.model.Account;
import com.onehilltech.backbone.gatekeeper.model.GatekeeperStore;
import com.onehilltech.backbone.gatekeeper.model.UserToken;
import com.onehilltech.backbone.gatekeeper.model.UserToken$Table;
import com.onehilltech.promises.Promise;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.runtime.FlowContentObserver;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

import static com.onehilltech.promises.Promise.rejected;
import static com.onehilltech.promises.Promise.resolved;

/**
 * A single session client.
 *
 * This client only supports one user being logged in at a time.
 */
public class GatekeeperSessionClient
{
  /**
   * Listener that receives notifications about changes to the signIn state.
   */
  public interface Listener
  {
    void onSignedIn (GatekeeperSessionClient client);
    void onSignedOut (GatekeeperSessionClient client);
    void onReauthenticate (GatekeeperSessionClient client, HttpError reason);
  }

  /**
   * Get a new GatekeeperSessionClient for the context.
   *
   * @param context       Target context
   * @return              GatekeeperSessionClient object.
   */
  public static GatekeeperSessionClient get (Context context)
  {
    return new GatekeeperSessionClient (context, false);
  }

  public static GatekeeperSessionClient get (Context context, boolean local)
  {
    return new GatekeeperSessionClient (context, local);
  }

  private OkHttpClient httpClient_;

  /// The user token for the current session.
  private UserToken userToken_;

  private final LinkedList <Listener> listeners_ = new LinkedList<> ();

  private final GatekeeperClient client_;

  private FlowContentObserver userTokenObserver_;

  private GatekeeperSession session_;

  private final UserMethods userMethods_;

  private final Methods methods_;

  private String userAgent_;

  private static final Logger LOG = LoggerFactory.getLogger (GatekeeperSessionClient.class);

  private final Converter<ResponseBody, Resource> resourceConverter_;

  private static final ArrayList <String> AUTHENTICATE_ERROR_CODES = new ArrayList<> ();

  private Gson gson_;

  private final String packageName_;

  private DataStore store_;

  private boolean isLocal_;

  private Account account_;

  /**
   * Initializing constructor.
   *
   * @param context         Target context
   */
  private GatekeeperSessionClient (Context context, boolean isLocal)
  {
    this.packageName_ = context.getPackageName ();
    this.client_ = new GatekeeperClient.Builder (context).build ();
    this.isLocal_ = isLocal;

    if (!this.isLocal_)
    {
      this.session_ = GatekeeperSession.getCurrent (context);
      this.userTokenObserver_ = new FlowContentObserver (context.getPackageName ());
    }

    // Build a new HttpClient for the user session. This client is responsible for
    // adding the authentication header to each request.
    this.httpClient_ =
        new OkHttpClient.Builder ()
            .addInterceptor (this.requestInterceptor_)
            .addInterceptor (this.responseInterceptor_)
            .build ();

    // Initialize the type factories for Gson.
    RuntimeTypeAdapterFactory<JsonGrant> grantTypes =
        RuntimeTypeAdapterFactory.of (JsonGrant.class, "grant_type")
                                 .registerSubtype (JsonPassword.class, "password")
                                 .registerSubtype (JsonRefreshToken.class, "refresh_token");

    // Initialize the Retrofit.
    ResourceSerializer serializer = new ResourceSerializer ();
    serializer.put ("account", JsonAccount.class);
    serializer.put ("accounts", JsonAccount.class);
    serializer.put ("change-password", JsonChangePassword.class);
    serializer.put ("token", JsonBearerToken.class);
    serializer.put ("errors", HttpError.class);

    this.gson_ =
        new GsonBuilder ()
            .registerTypeAdapter (Resource.class, serializer)
            .registerTypeAdapterFactory (grantTypes)
            .create ();

    // Build the Retrofit object for this client.
    Retrofit userRetrofit = new Retrofit.Builder ()
        .baseUrl (this.client_.getConfig ().baseUri)
        .addConverterFactory (GsonConverterFactory.create (this.gson_))
        .client (this.httpClient_)
        .build ();

    this.resourceConverter_ = userRetrofit.responseBodyConverter (Resource.class, new Annotation[0]);

    this.userMethods_ = userRetrofit.create (UserMethods.class);
    this.methods_ = new Retrofit.Builder ()
        .baseUrl (this.client_.getConfig ().baseUri)
        .addConverterFactory (GsonConverterFactory.create (this.gson_))
        .build ().create (Methods.class);

    if (!this.isLocal_)
      this.initUserToken (context);

    this.store_ = GatekeeperStore.open (context, this);
  }

  public Promise <JsonAccount> createAccount (String username, String password, String email, boolean autoSignIn)
  {
    if (this.isLocal_)
      return Promise.reject (new IllegalAccessException ("Temporary sessions cannot create an account."));

    LOG.info ("Creating user account for {}", username);

    return this.client_.createAccount (username, password, email, autoSignIn)
                       .then (r -> new Promise<> (settlement -> {
                         // Complete the sign in process.
                         JsonAccount account = r.get ("account");

                         if (autoSignIn) {
                           JsonBearerToken userToken = r.get ("token");

                           this.completeSignIn (username, userToken)
                               .then (resolved (value -> settlement.resolve (account)))
                               ._catch (rejected (settlement::reject));
                         }
                         else {
                           settlement.resolve (account);
                         }
                       }));
  }

  private void initUserToken (Context context)
  {
    LOG.info ("Initializing the session token");

    String username = GatekeeperSession.getCurrent (context).getUsername ();

    if (username != null)
      this.userToken_ =
          SQLite.select ()
                .from (UserToken.class)
                .where (UserToken$Table.username.eq (username))
                .querySingle ();

    // Load the one and only user token from the database. We also want to
    // observe the user token table for changes. These changes could be logging
    // out or refreshing the user token.
    this.userTokenObserver_.registerForContentChanges (context, UserToken.class);
    this.userTokenObserver_.addModelChangeListener ((table, action, primaryKeyValues) -> {
      if (table == null || !table.equals (UserToken.class))
        return;

      if (action == BaseModel.Action.DELETE)
      {
        if (primaryKeyValues.length == 1)
          onTokenDeleted ((String)primaryKeyValues[0].value ());
      }
      else if (action == BaseModel.Action.INSERT)
      {
        onTokenInserted ((String) primaryKeyValues[0].value ());
      }
    });
  }

  public boolean isLocal ()
  {
    return this.isLocal_;
  }

  public GatekeeperClient getClient ()
  {
    return this.client_;
  }

  public GatekeeperSession getSession ()
  {
    return this.session_;
  }

  public Account getAccount ()
  {
    return this.account_;
  }

  /**
   * A new user token was inserted into the database.
   *
   * @param username          Username of the inserted token.
   */
  private void onTokenInserted (String username)
  {
    // Load the token for the user that was logged in.
    LOG.info ("Retrieving user token for {}", username);

    this.userToken_ =
        SQLite.select ()
              .from (UserToken.class)
              .where (UserToken$Table.username.eq (username))
              .querySingle ();

    LOG.info ("Notifying all the user has logged in.");

    // The token is inserted into the database when the user is logged in. The
    // token is updated in the database when the it is refreshed from the server.

    Message msg = this.uiHandler_.obtainMessage (MSG_ON_LOGIN);
    msg.sendToTarget ();
  }

  /**
   * A user token was deleted from the database.
   *
   * @param username        Username for the deleted token.
   */
  private void onTokenDeleted (String username)
  {
    LOG.info ("An user token has been deleted from the database.");

    if (this.userToken_ != null)
    {
      // Check if the username matches the one in our session. If it does, then we need
      // to let this session know it has been logged out.

      if (this.userToken_.username.equals (username))
      {
        LOG.info ("The token for this session has been deleted.");

        Message msg = this.uiHandler_.obtainMessage (MSG_ON_LOGOUT);
        msg.sendToTarget ();

        // Reset the user token.
        this.userToken_ = null;
      }
      else
      {
        LOG.info ("This user token in this session does not match the deleted username.");
      }
    }
    else
    {
      LOG.info ("This session does not have a user token.");
    }
  }

  /**
   * Set the User-Agent for the client.
   *
   * @param userAgent
   */
  void setUserAgent (String userAgent)
  {
    this.userAgent_ = userAgent;
  }

  /**
   * Get the User-Agent value.
   *
   * @return
   */
  public String getUserAgent ()
  {
    return this.userAgent_;
  }

  /**
   * Set the listener object
   *
   * @param listener      Listener object
   */
  public void addListener (Listener listener)
  {
    this.listeners_.add (listener);
  }

  public void removeListener (Listener listener)
  {
    this.listeners_.remove (listener);
  }

  /**
   * Get the access token for the current session.
   *
   * @return
   */
  public String getAccessToken ()
  {
    return this.userToken_.accessToken;
  }

  /**
   * Ensure the user is signed in to the session. If not, then we show the sign in
   * activity that prompts the user to sign in.
   *
   * @param activity        Parent activity
   * @param signIn          Sign in activity class
   * @return                True if signed in; otherwise false
   */
  public boolean ensureSignedIn (Activity activity, Class <? extends Activity> signIn)
  {
    return this.ensureSignedIn (activity, new Intent (activity, signIn));
  }

  /**
   * Ensure the user is signed in to the session. If not, then we show the sign in
   * activity that prompts the user to sign in.
   *
   * @param activity            Parent activity
   * @param signInIntent        Sign in activity intent
   * @return
   */
  public boolean ensureSignedIn (Activity activity, Intent signInIntent)
  {
    LOG.info ("Ensuring the user is signed in");

    if (this.isSignedIn ())
      return true;

    this.forceSignIn (activity, signInIntent);
    return false;
  }

  /**
   * Force the session client to sign in the current user.
   *
   * @param activity
   * @param signIn
   */
  public void forceSignIn (Activity activity, Class <? extends Activity> signIn)
  {
    this.forceSignIn (activity, new Intent (activity, signIn));
  }

  /**
   * Force the session client to sign in the current user.
   *
   * @param activity
   * @param signInIntent
   */
  public void forceSignIn (Activity activity, Intent signInIntent)
  {
    LOG.info ("Forcing the user to sign in");

    // Force the user to sign out.
    this.completeSignOut ();

    signInIntent.putExtra (GatekeeperSignInActivity.ARG_REDIRECT_INTENT, activity.getIntent ());
    activity.startActivity (signInIntent);

    // Finish the current activity.
    activity.finish ();
  }

  /**
   * Cleanup the object. This is to be called in the onDestroy() method of the
   * Context (e.g., Activity, Fragment, or Service) that created it.
   */
  public void onDestroy (Context context)
  {
    this.userTokenObserver_.unregisterForContentChanges (context);
  }

  /**
   * Get the underlying HTTP client.
   *
   * @return
   */
  public OkHttpClient getHttpClient ()
  {
    return this.httpClient_;
  }

  /**
   * Test is client has a user signed in.
   */
  public boolean isSignedIn ()
  {
    LOG.info ("Checking if the current user is signed in.");

    return this.userToken_ != null;
  }

  /**
   * Complete the signOut process.
   */
  private void completeSignOut ()
  {
    LOG.info ("Completing the sign out process");

    // Delete the current session information.
    this.account_ = null;
    this.session_.edit ().delete ();

    // Delete the token from the database. This will cause all session clients
    // listening for changes to be notified of the change.
    if (this.userToken_ != null)
      FlowManager.getModelAdapter (UserToken.class).delete (this.userToken_);

    // Clear the data store cache.
    this.store_.clearCache ();
  }

  public ArrayList<HttpError> getErrors (ResponseBody errorBody)
      throws IOException
  {
    return this.client_.getErrors (errorBody);
  }

  public HttpError getFirstError (ResponseBody errorBody)
      throws IOException
  {
    return this.client_.getFirstError (errorBody);
  }

  /**
   * Sign in a user.
   *
   * @param username          Username for the user
   * @param password          Password for the user
   */
  public Promise <Void> signIn (String username, String password)
  {
    return this.signIn (username, password, null);
  }

  public Promise <Void> signIn (String username, String password, @Nullable List <String> scope)
  {
    if (this.isSignedIn ())
      return Promise.reject (new IllegalStateException ("User is already signed in"));

    return this.getUserToken (username, password, scope)
               .then (token -> this.completeSignIn (username, token));
  }

  /**
   * Force the session client to use an existing access and refresh token.
   *
   * @param username
   * @param accessToken
   * @param refreshToken
   * @return
   */
  public Promise <Void> beginSession (String username, String accessToken, String refreshToken)
  {
    LOG.info ("Begin session for {}", username);

    JsonBearerToken token = new JsonBearerToken (accessToken, refreshToken);
    return this.completeSignIn (username, token);
  }

  public Promise <Boolean> authenticate (String password)
  {
    JsonAuthenticate authenticate = new JsonAuthenticate (password);
    return this.executeCall (this.userMethods_.authenticate (authenticate));
  }

  /**
   * Complete the signIn process by storing the information in the database, and
   * notifying all parties that the signIn is complete.
   *
   * @param username            Username that signed in
   * @param jsonToken           Access token for the user
   */
  private Promise <Void> completeSignIn (String username, JsonBearerToken jsonToken)
  {
    LOG.info ("Saving user access token to the database");

    this.userToken_ = UserToken.fromToken (username, jsonToken);

    return this.store_.get (Account.class, "me")
                      .then (resolved (this::persistSession))
                      ._catch (reason -> {
                        // Clear the user token.
                        this.userToken_ = null;

                        // Still reject the promise.
                        return Promise.reject (reason);
                      });
  }

  private void persistSession (Account account)
  {
    this.account_ = account;

    if (this.isLocal_)
      return;

    this.session_.edit ()
                 .setUsername (account.username)
                 .setUserId (account._id.toString ())
                 .commit ();

    FlowManager.getModelAdapter (UserToken.class).save (this.userToken_);
  }

  public Promise <Boolean> signOut ()
  {
    return this.signOut (true);
  }

  /**
   * Sign out the current user
   */
  public Promise <Boolean> signOut (boolean forceSignOut)
  {
    if (this.userToken_ == null)
      return Promise.resolve (true);

    LOG.info ("Signing out the current user.");

    return new Promise<> ("gatekeeper:signOut", settlement -> {
      LOG.info ("Signing out current user");

      this.userMethods_.logout ().enqueue (new Callback<Boolean> ()
      {
        @Override
        @ParametersAreNonnullByDefault
        public void onResponse (Call<Boolean> call, retrofit2.Response<Boolean> response)
        {
          if (response.isSuccessful () || forceSignOut)
          {
            Boolean result = response.body ();
            boolean complete = (result != null && result) || forceSignOut;

            if (complete)
              completeSignOut ();

            settlement.resolve (complete);
          }
          else
          {
            settlement.reject (new IllegalStateException ("Failed to sign out user"));
          }
        }

        @Override
        @ParametersAreNonnullByDefault
        public void onFailure (Call<Boolean> call, Throwable t)
        {
          if (forceSignOut)
          {
            completeSignOut ();
            settlement.resolve (true);
          }
          else
          {
            settlement.reject (t);
          }
        }
      });
    });
  }

  /**
   * Change the users current password.
   */
  public Promise <Boolean> changePassword (String currentPassword, String newPassword)
  {
    LOG.info ("Changing the user's password.");

    return new Promise<> ("gatekeeper:changePassword", settlement -> {
      JsonChangePassword change = new JsonChangePassword ();
      change.currentPassword = currentPassword;
      change.newPassword = newPassword;

      Resource r = new Resource ("change-password", change);

      this.userMethods_.changePassword (r).enqueue (new Callback<Boolean> ()
      {
        @Override
        @ParametersAreNonnullByDefault
        public void onResponse (Call<Boolean> call, retrofit2.Response<Boolean> response)
        {
          if (response.isSuccessful ())
          {
            settlement.resolve (response.body ());
          }
          else
          {
            settlement.reject (new IllegalStateException (response.message ()));
          }
        }

        @Override
        @ParametersAreNonnullByDefault
        public void onFailure (Call<Boolean> call, Throwable t)
        {
          settlement.reject (t);
        }
      });
    });
  }

  /**
   * Get an access token for the user.
   *
   * @param username        Username
   * @param password        Password
   */
  private Promise<JsonBearerToken> getUserToken (String username, String password, List <String> scope)
  {
    LOG.info ("Getting an access token for the user.");

    JsonPassword grant = new JsonPassword ();
    grant.username = username;
    grant.password = password;
    grant.clientId = this.client_.getConfig ().clientId;
    grant.clientSecret = this.client_.getConfig ().clientSecret;
    grant.packageName = this.packageName_;
    grant.scope = scope;

    return this.executeCall (this.methods_.getUserToken (grant));
  }

  private <T> Promise <T> executeCall (Call <T> call)
  {
    return new Promise<> ("gatekeeper:executeCall", settlement ->
      call.enqueue (new Callback<T> () {
        @Override
        @ParametersAreNonnullByDefault
        public void onResponse (Call<T> call, retrofit2.Response<T> response)
        {
          if (response.isSuccessful ())
          {
            settlement.resolve (response.body ());
          }
          else
          {
            try
            {
              HttpError error = getFirstError (response.errorBody ());
              error.setStatusCode (response.code ());

              settlement.reject (error);
            }
            catch (IOException e)
            {
              settlement.reject (e);
            }
          }
        }

        @Override
        @ParametersAreNonnullByDefault
        public void onFailure (Call<T> call, Throwable t)
        {
          settlement.reject (t);
        }
      }));
  }

  // The messaging handler for this client. This handlers notifies interested
  // parties when a user is signed in and when a user is signed out.
  private static final int MSG_ON_LOGIN = 0;
  private static final int MSG_ON_LOGOUT = 1;
  private static final int MSG_ON_REAUTHENTICATE = 2;

  private final Handler uiHandler_ = new Handler (Looper.getMainLooper ()) {
    @Override
    public void handleMessage (Message msg)
    {
      switch (msg.what)
      {
        case MSG_ON_LOGIN:
          for (Listener listener: listeners_)
            listener.onSignedIn (GatekeeperSessionClient.this);
          break;

        case MSG_ON_LOGOUT:
          for (Listener listener: listeners_)
            listener.onSignedOut (GatekeeperSessionClient.this);
          break;

        case MSG_ON_REAUTHENTICATE:
          HttpError httpError = (HttpError)msg.obj;

          for (Listener listener: listeners_)
            listener.onReauthenticate (GatekeeperSessionClient.this, httpError);
          break;
      }
    }
  };

  private boolean refreshTokenSync ()
  {
    try
    {
      JsonRefreshToken grant = new JsonRefreshToken ();
      grant.refreshToken = this.userToken_.refreshToken;
      grant.clientId = this.client_.getConfig ().clientId;
      grant.clientSecret = this.client_.getConfig ().clientSecret;
      grant.packageName = this.packageName_;

      retrofit2.Response<JsonBearerToken> response = this.userMethods_.refreshToken (grant).execute ();

      if (response.isSuccessful ())
      {
        JsonBearerToken token = response.body ();

        this.userToken_.accessToken = token.accessToken;
        this.userToken_.refreshToken = token.refreshToken;
        FlowManager.getModelAdapter (UserToken.class).save (this.userToken_);

        return true;
      }
      else
      {
        return false;
      }
    }
    catch (IOException e)
    {
      LOG.error (e.getLocalizedMessage (), e);
      return false;
    }
  }

  /**
   * The interceptor that added header information to the request.
   */
  private final Interceptor requestInterceptor_ = new Interceptor ()
  {
    @Override
    public Response intercept (Chain chain)
        throws IOException
    {
      return prepareRequest (chain);
    }
  };

  /**
   * Prepare the request.
   *
   * @param chain
   * @return
   * @throws IOException
   */
  private Response prepareRequest (Interceptor.Chain chain)
      throws IOException
  {
    okhttp3.Request original = chain.request ();
    okhttp3.Request.Builder builder = original.newBuilder ();

    if (this.userToken_ != null)
      builder.header ("Authorization", "Bearer " + this.userToken_.accessToken);

    if (this.userAgent_ != null)
      builder.header ("User-Agent", this.userAgent_);

    builder.method (original.method (), original.body ());

    return chain.proceed (builder.build ());
  }

  /**
   * Interceptor that handles special cases for a response, such a refreshing
   * the token or for ending a session.
   */
  private final Interceptor responseInterceptor_ = new Interceptor ()
  {
    @Override
    public Response intercept (Chain chain) throws IOException
    {
      // Proceed with the original request. Check the status code for the response.
      // If the status code is 401, then we need to refresh the token. Otherwise,
      // we return control to the next interceptor.

      Request origRequest = chain.request ();
      Response origResponse = chain.proceed (origRequest);

      if (origResponse.isSuccessful ())
        return origResponse;

      int statusCode = origResponse.code ();

      if (statusCode == 401) {
        // Let's try to update the original token. If the response is not successful,
        // the return the original response. Otherwise, retry the same request.
        if (refreshTokenSync ())
          return chain.proceed (origRequest);
      }
      else if (statusCode == 403) {
        // Let's see what kind of error message we received. We may be able to handle
        // it here in the interceptor if it related to the token.
        Resource resource = resourceConverter_.convert (origResponse.body ());
        ArrayList <HttpError> errors = resource.get ("errors");
        HttpError error = errors.get (0);

        // Since we can only consume a ResponseBody once, we need to replace the original
        // one with a new one.
        String origBody = gson_.toJson (resource);
        ResponseBody responseBody = ResponseBody.create (origResponse.body ().contentType (), origBody);

        origResponse =
            origResponse.newBuilder ()
                        .body (responseBody)
                        .build();

        if (AUTHENTICATE_ERROR_CODES.contains (error.getCode ()))
        {
          // Notify the client to authenticate. This is optional. If the client
          // does not authenticate, then all calls will continue to fail.
          Message msg = uiHandler_.obtainMessage (MSG_ON_REAUTHENTICATE, error);
          msg.sendToTarget ();
        }
      }

      return origResponse;
    }
  };

  interface Methods
  {
    @POST("oauth2/token")
    Call <JsonBearerToken> getUserToken (@Body JsonGrant password);
  }

  interface UserMethods
  {
    @POST("oauth2/logout")
    Call <Boolean> logout ();

    @POST("accounts/me/password")
    Call <Boolean> changePassword (@Body Resource r);

    @POST("accounts/authenticate")
    Call <Boolean> authenticate (@Body JsonAuthenticate body);

    @POST("oauth2/token")
    Call <JsonBearerToken> refreshToken (@Body JsonGrant refreshToken);
  }

  static
  {
    AUTHENTICATE_ERROR_CODES.add ("unknown_token");
    AUTHENTICATE_ERROR_CODES.add ("invalid_token");
    AUTHENTICATE_ERROR_CODES.add ("token_disabled");
    AUTHENTICATE_ERROR_CODES.add ("unknown_client");
    AUTHENTICATE_ERROR_CODES.add ("client_disabled");
    AUTHENTICATE_ERROR_CODES.add ("unknown_account");
    AUTHENTICATE_ERROR_CODES.add ("account_disabled");
  }
}
