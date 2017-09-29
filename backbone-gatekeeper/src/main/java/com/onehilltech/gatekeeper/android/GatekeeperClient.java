package com.onehilltech.gatekeeper.android;

import android.content.Context;
import android.content.pm.PackageManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import com.onehilltech.backbone.data.HttpError;
import com.onehilltech.backbone.data.Resource;
import com.onehilltech.backbone.data.ResourceEndpoint;
import com.onehilltech.backbone.data.ResourceSerializer;
import com.onehilltech.gatekeeper.android.http.JsonAccount;
import com.onehilltech.gatekeeper.android.http.JsonBearerToken;
import com.onehilltech.gatekeeper.android.http.JsonClientCredentials;
import com.onehilltech.gatekeeper.android.http.JsonGrant;
import com.onehilltech.gatekeeper.android.model.ClientToken;
import com.onehilltech.metadata.ManifestMetadata;
import com.onehilltech.metadata.MetadataProperty;
import com.onehilltech.promises.Promise;
import com.raizlabs.android.dbflow.config.FlowManager;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

import static com.onehilltech.promises.Promise.rejected;
import static com.onehilltech.promises.Promise.resolved;

/**
 * Client interface for communicating with a Gatekeeper service.
 */
public class GatekeeperClient
{
  /**
   * Gatekeeper client configuration. The configuration can be initialized manually
   * or loaded from the meta-data section in AndroidManifest.xml.
   */
  public static final class Configuration
  {
    private static final String CLIENT_ID = "com.onehilltech.gatekeeper.android.client_id";
    private static final String CLIENT_SECRET = "com.onehilltech.gatekeeper.android.client_secret";
    private static final String BASE_URI = "com.onehilltech.gatekeeper.android.baseuri";

    @MetadataProperty(name=CLIENT_ID, fromResource=true)
    public String clientId;

    @MetadataProperty(name=CLIENT_SECRET, fromResource=true)
    public String clientSecret;

    @MetadataProperty(name=BASE_URI, fromResource=true)
    public String baseUri;

    /**
     * Load the configuration from metadata.
     */
    public static Configuration loadFromMetadata (Context context)
        throws PackageManager.NameNotFoundException, InvocationTargetException, IllegalAccessException, ClassNotFoundException
    {
      Configuration configuration = new Configuration ();
      ManifestMetadata.get (context).initFromMetadata (configuration);

      return configuration;
    }
  }

  /**
   * @class Builder
   *
   * Builder for creating GatekeeperClient objects.
   */
  public static final class Builder
  {
    private OkHttpClient httpClient_;

    private Configuration config_;

    private Context context_;

    public Builder (Context context)
    {
      this.context_ = context;
    }

    public Builder setClient (OkHttpClient httpClient)
    {
      this.httpClient_ = httpClient;
      return this;
    }

    public Builder setConfiguration (Configuration config)
    {
      this.config_ = config;
      return this;
    }

    public GatekeeperClient build ()
    {
      try
      {
        Configuration config = this.config_;

        if (this.config_ == null)
          this.config_ = Configuration.loadFromMetadata (this.context_);

        if (this.httpClient_ == null)
          this.httpClient_ = new OkHttpClient.Builder ().build ();

        return new GatekeeperClient (this);
      }
      catch (PackageManager.NameNotFoundException | IllegalAccessException | ClassNotFoundException | InvocationTargetException e)
      {
        throw new IllegalStateException ("Failed to load default configuration", e);
      }
    }
  }

  public static final int VERSION = 1;

  private final Context context_;

  private final OkHttpClient httpClient_;

  private final ClientMethods methods_;

  private Gson gson_;

  private Configuration config_;

  private final Converter<ResponseBody, Resource> resourceConverter_;

  private ClientToken clientToken_;

  private ResourceEndpoint <JsonAccount> accountEndpoint_;

  GatekeeperClient (Builder builder)
  {
    this.context_ = builder.context_;
    this.config_ = builder.config_;

    this.httpClient_ =
        builder.httpClient_
            .newBuilder ()
            .addInterceptor (this.authorizationHeader_)
            .build ();

    // Initialize the type factories for Gson.
    RuntimeTypeAdapterFactory <JsonGrant> grantTypes =
        RuntimeTypeAdapterFactory.of (JsonGrant.class, "grant_type")
                                 .registerSubtype (JsonClientCredentials.class, "client_credentials");

    ResourceSerializer serializer = new ResourceSerializer ();
    serializer.put ("account", JsonAccount.class);
    serializer.put ("token", JsonBearerToken.class);
    serializer.put ("errors", HttpError.class);

    this.gson_ =
        new GsonBuilder ()
            .registerTypeAdapter (Resource.class, serializer)
            .registerTypeAdapterFactory (grantTypes)
            .create ();

    serializer.setGson (this.gson_);

    // Initialize the Retrofit related properties.
    Retrofit retrofit =
        new Retrofit.Builder ()
            .baseUrl (this.getBaseUrlWithVersion ())
            .addConverterFactory (GsonConverterFactory.create (this.gson_))
            .client (this.httpClient_)
            .build ();

    this.resourceConverter_ = retrofit.responseBodyConverter (Resource.class, new Annotation[0]);

    // Create the remoting endpoints.
    this.methods_ = retrofit.create (ClientMethods.class);
    this.accountEndpoint_ = ResourceEndpoint.create (retrofit, "account", "accounts");
  }

  /**
   * Get the http client for the client.
   *
   * @return        OkHttpClient object
   */
  public OkHttpClient getHttpClient ()
  {
    return this.httpClient_;
  }

  public Configuration getConfig ()
  {
    return this.config_;
  }

  public String getBaseUrlWithVersion ()
  {
    return this.config_.baseUri + "v" + VERSION + "/";
  }

  /**
   * Create a new account.
   *
   * @param username
   * @param password
   * @param email
   */
  public Promise<Resource> createAccount (String username, String password, String email)
  {
    return new Promise<> (settlement -> {
      JsonAccount account = new JsonAccount ();
      account.username = username;
      account.password = password;
      account.email = email;

      this.requestClientToken ()
          .then (result -> this.accountEndpoint_.create (account))
          .then (resolved (settlement::resolve))
          ._catch (rejected (settlement::reject));
    });
  }

  /**
   * Create a new account, and login the user.
   *
   * @param username
   * @param password
   * @param email
   * @param autoSignIn
   */
  public Promise <Resource> createAccount (String username, String password, String email, boolean autoSignIn)
  {
    return new Promise<> (settlement -> {
      // Make a call to create the account.
      JsonAccount account = new JsonAccount ();
      account.username = username;
      account.password = password;
      account.email = email;

      HashMap<String, Object> options = new HashMap<> ();
      options.put ("login", autoSignIn);

      this.requestClientToken ()
          .then (result -> this.accountEndpoint_.create (account, options))
          .then (resolved (settlement::resolve))
          ._catch (rejected (settlement::reject));

      /*
      this.client_.getClientToken ()
                  .then (token -> {
                    // Use the client token to create a new account. We are going to login
                    // with the newly created account.
                    this.clientToken_ = ClientToken.fromToken (this.client_.getClientId (), token);
                    FlowManager.getModelAdapter (ClientToken.class).save (this.clientToken_);



                    return this.getCreateAccountEndpoint ().create (account, options);
                  })
                  .then (resolved (r -> {
                    // Complete the sign in process.
                    JsonAccount account = r.get ("account");
                    JsonBearerToken userToken = r.get ("token");

                    this.completeSignIn (username, userToken)
                        .then (resolved (value -> settlement.resolve (account)))
                        ._catch (rejected (settlement::reject));
                  }))
                  ._catch (rejected (settlement::reject))*/
    });
  }

  private Promise <Void> requestClientToken ()
  {
    if (this.clientToken_ != null)
      return Promise.resolve (null);

    JsonClientCredentials credentials = new JsonClientCredentials ();

    return this.requestToken (credentials)
               .then (resolved (token -> {
                 this.clientToken_ = ClientToken.fromToken (this.config_.clientId, token);
                 FlowManager.getModelAdapter (ClientToken.class).save (this.clientToken_);
               }));
  }

  /**
   * Helper method for requesting an access token.
   *
   * @param grantType     JsonGrant object
   */
  private Promise<JsonBearerToken> requestToken (JsonGrant grantType)
  {
    return new Promise<> ((settlement) -> {
      grantType.clientId = this.config_.clientId;
      grantType.clientSecret = this.config_.clientSecret;
      grantType.packageName = this.context_.getPackageName ();

      this.methods_.getBearerToken (grantType)
                   .enqueue (new Callback<JsonBearerToken> () {
                     @Override
                     public void onResponse (Call<JsonBearerToken> call, Response<JsonBearerToken> response)
                     {
                       if (response.isSuccessful ())
                       {
                         settlement.resolve (response.body ());
                       }
                       else
                       {
                         try
                         {
                           HttpError error = getError (response.errorBody ());
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
                     public void onFailure (Call<JsonBearerToken> call, Throwable t)
                     {
                       settlement.reject (t);
                     }
                   });
    });
  }

  /**
   * Get the HttpError from the ResponseBody.
   *
   * @param errorBody       Error body
   */
  public HttpError getError (ResponseBody errorBody)
      throws IOException
  {
    Resource resource = this.resourceConverter_.convert (errorBody);
    return resource.get ("errors");
  }

  private interface ClientMethods
  {
    @POST("oauth2/token")
    Call<JsonBearerToken> getBearerToken (@Body JsonGrant grant);
  }

  /**
   * Interceptor that add the client token as the Authorization header to the request.
   */
  private final Interceptor authorizationHeader_ = (chain)-> {
    okhttp3.Request original = chain.request ();
    Request.Builder builder = original.newBuilder ();

    if (clientToken_ != null)
      builder.header ("Authorization", "Bearer " + clientToken_.accessToken);

    builder.method (original.method (), original.body ())
           .build ();

    return chain.proceed (builder.build ());
  };
}
