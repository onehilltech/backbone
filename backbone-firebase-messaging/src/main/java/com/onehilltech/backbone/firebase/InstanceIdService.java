package com.onehilltech.backbone.firebase;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.onehilltech.backbone.data.HttpError;
import com.onehilltech.backbone.gatekeeper.GatekeeperSessionClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.onehilltech.promises.Promise.rejected;
import static com.onehilltech.promises.Promise.resolved;

public class InstanceIdService extends FirebaseInstanceIdService
  implements GatekeeperSessionClient.Listener
{
  private final Logger logger_ = LoggerFactory.getLogger (InstanceIdService.class);

  private GatekeeperSessionClient sessionClient_;

  @Override
  public void onCreate ()
  {
    super.onCreate ();

    this.sessionClient_ = GatekeeperSessionClient.getInstance (this);
    this.sessionClient_.addListener (this);
  }

  @Override
  public void onDestroy ()
  {
    super.onDestroy ();

    if (this.sessionClient_ != null)
      this.sessionClient_.removeListener (this);
  }

  @Override
  public void onTokenRefresh ()
  {
    CloudToken cloudToken = new CloudToken ();
    FirebaseInstanceId instanceId = FirebaseInstanceId.getInstance ();

    cloudToken.device = instanceId.getId ();
    cloudToken.token = instanceId.getToken ();

    MessagingClient.getInstance (this)
                   .refreshToken (cloudToken)
                   .then (resolved (this::saveClaimTicket))
                   ._catch (rejected (reason -> this.logger_.error ("Failed to refresh cloud token", reason.getLocalizedMessage ())));
  }

  private void saveClaimTicket (ClaimTicket ticket)
  {
    if (ticket != null && ticket.claimTicket != null)
    {
      this.logger_.info ("Saving claim ticket for the cloud token");
      CloudMessagingPreferences.open (this).edit ()
          .putClaimTicket (ticket.claimTicket)
          .commit ();
    }
    else
    {
      this.logger_.info ("Deleting claim ticket for the cloud token");

      CloudMessagingPreferences.open (this).edit ()
          .removeClaimTicket ()
          .commit ();
    }
  }

  @Override
  public void onSignedIn (GatekeeperSessionClient client)
  {
    ClaimTicket claimTicket = new ClaimTicket ();
    claimTicket.claimTicket = CloudMessagingPreferences.open (this).getClaimTicket ();
    String deviceId = FirebaseInstanceId.getInstance ().getId ();

    this.logger_.info ("Claiming cloud token for this device.");

    MessagingClient.getInstance (this)
                   .claimDevice (deviceId, claimTicket)
                   .then (resolved (result -> {
                      if (result)
                        this.logger_.info ("Successfully claim cloud token for this device.");
                      else
                        this.logger_.error ("Failed to cloud token for this device.");
                   }))
                   ._catch (rejected (reason -> this.logger_.error ("Failed to claim device.", reason)));
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
