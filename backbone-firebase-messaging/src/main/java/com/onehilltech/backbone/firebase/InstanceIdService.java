package com.onehilltech.backbone.firebase;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.onehilltech.backbone.data.HttpError;
import com.onehilltech.gatekeeper.android.GatekeeperSessionClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstanceIdService extends FirebaseInstanceIdService
  implements GatekeeperSessionClient.Listener
{
  private final Logger logger_ = LoggerFactory.getLogger (InstanceIdService.class);

  @Override
  public void onTokenRefresh ()
  {

  }

  /**
   * Upload the token to our server.
   */
  private void uploadInstance (FirebaseInstanceId instance)
  {
  }

  @Override
  public void onSignedIn (GatekeeperSessionClient client)
  {

  }

  @Override
  public void onSignedOut (GatekeeperSessionClient client)
  {

  }

  @Override
  public void onReauthenticate (GatekeeperSessionClient client, HttpError reason)
  {

  }
}
