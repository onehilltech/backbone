package com.onehilltech.backbone.firebase;

import android.content.Context;
import android.content.pm.PackageManager;

import com.onehilltech.backbone.gatekeeper.GatekeeperSessionClient;
import com.onehilltech.metadata.ManifestMetadata;
import com.onehilltech.metadata.MetadataProperty;
import com.onehilltech.promises.Promise;

import java.lang.reflect.InvocationTargetException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.POST;
import retrofit2.http.Path;

public class MessagingClient
{
  /**
   * Gatekeeper client configuration. The configuration can be initialized manually
   * or loaded from the meta-data section in AndroidManifest.xml.
   */
  public static final class Configuration
  {
    private static final String BASE_URI = "com.onehilltech.backbone.cloud-messaging.base_uri";

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

  private static MessagingClient instance_;

  private ClientMethods clientMethods_;

  private UserMethods userMethods_;

  private final GatekeeperSessionClient sessionClient_;

  public static MessagingClient getInstance (Context context)
  {
    if (instance_ != null)
      return instance_;

    try
    {
      instance_ = new MessagingClient (context);
      return instance_;
    }
    catch (Exception e)
    {
      throw new IllegalStateException ("Failed to load configuration.", e);
    }
  }

  private MessagingClient (Context context)
      throws PackageManager.NameNotFoundException, IllegalAccessException, InvocationTargetException, ClassNotFoundException
  {
    this.sessionClient_ = GatekeeperSessionClient.getInstance (context);
    Configuration configuration = Configuration.loadFromMetadata (context);

    this.clientMethods_ =
        new Retrofit.Builder ()
            .baseUrl (configuration.baseUri)
            .client (this.sessionClient_.getClient ().getHttpClient ())
            .build ().create (ClientMethods.class);

    this.userMethods_ =
        new Retrofit.Builder ()
            .baseUrl (configuration.baseUri)
            .client (this.sessionClient_.getHttpClient ())
            .build ().create (UserMethods.class);
  }

  public Promise <ClaimTicket> refreshToken (CloudToken cloudToken)
  {
    Call <ClaimTicket> call =
        this.sessionClient_.isSignedIn () ?
            this.userMethods_.refreshToken (cloudToken) :
            this.clientMethods_.refreshToken (cloudToken);

    return this.executeCall (call);
  }

  public Promise <Boolean> claimDevice (String deviceId, ClaimTicket claimTicket)
  {
    return this.executeCall (this.userMethods_.claimDevice (deviceId, claimTicket));
  }

  public Promise <Boolean> releaseDevice (String instanceId)
  {
    return this.executeCall (this.userMethods_.releaseDevice (instanceId));
  }

  private <T> Promise <T> executeCall (Call <T> call)
  {
    return new Promise<> (
        settlement -> call.enqueue (new Callback<T> () {
          @Override
          public void onResponse (Call<T> call, Response<T> response)
          {
            if (response.isSuccessful ())
            {
              settlement.resolve (response.body ());
            }
            else
            {
              settlement.reject (new IllegalStateException ("Failed to execute"));
            }
          }

          @Override
          public void onFailure (Call<T> call, Throwable t)
          {
            settlement.reject (t);
          }
        }));
  }

  interface ClientMethods
  {
    @POST("cloud-tokens")
    Call <ClaimTicket> refreshToken (@Body CloudToken cloudToken);
  }

  interface UserMethods
  {
    @POST("cloud-tokens/{id}/claim")
    Call <Boolean> claimDevice (@Path("id") String instanceId, @Body ClaimTicket claimTicket);

    Call <ClaimTicket> refreshToken (@Body CloudToken cloudToken);

    @DELETE("cloud-tokens/{id}/claim")
    Call <Boolean> releaseDevice (@Path("id") String instanceId);
  }
}
