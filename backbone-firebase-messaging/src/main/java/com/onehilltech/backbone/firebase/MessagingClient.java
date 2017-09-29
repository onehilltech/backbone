package com.onehilltech.backbone.firebase;

import android.content.Context;

import com.onehilltech.gatekeeper.android.GatekeeperSessionClient;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.http.Body;

public class MessagingClient
{
  private static MessagingClient instance_;

  private Retrofit retrofit_;

  interface ClientMethods
  {
    Call <ClaimTicket> refreshToken (@Body CloudToken cloudToken);
  }

  interface UserMethods
  {
    Call <Boolean> claimDevice (@Body ClaimTicket claimTicket);

    Call <Boolean> unclaimDevice ();
  }

  public static MessagingClient getInstance (Context context)
  {
    if (instance_ != null)
      return instance_;

    instance_ = new MessagingClient (context);
    return instance_;
  }

  private MessagingClient (Context context)
  {
    GatekeeperSessionClient sessionClient = GatekeeperSessionClient.getInstance (context);

    this.retrofit_ =
        new Retrofit.Builder ()
            .build ();
  }
}
