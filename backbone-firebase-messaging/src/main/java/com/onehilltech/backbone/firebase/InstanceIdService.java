package com.onehilltech.backbone.firebase;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstanceIdService extends FirebaseInstanceIdService
{
  private final Logger logger_ = LoggerFactory.getLogger (InstanceIdService.class);

  @Override
  public void onCreate ()
  {
    super.onCreate ();

    FirebaseInstanceId instance = FirebaseInstanceId.getInstance ();

    this.persistInstance (instance);
    this.uploadInstance (instance);
  }

  @Override
  public void onTokenRefresh ()
  {
    FirebaseInstanceId instance = FirebaseInstanceId.getInstance ();

    this.persistInstance (instance);
    this.uploadInstance (instance);
  }

  private void persistInstance (FirebaseInstanceId instanceId)
  {
    this.logger_.info ("Persisting Firebase instance information");
    
    FirebaseSharedPreferences.open (this)
                             .edit ()
                             .putInstance (instanceId)
                             .commit ();
  }

  /**
   * Upload the token to our server.
   */
  private void uploadInstance (FirebaseInstanceId instance)
  {
  }
}
