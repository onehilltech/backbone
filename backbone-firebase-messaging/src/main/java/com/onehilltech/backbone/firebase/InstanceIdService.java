package com.onehilltech.backbone.firebase;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.onehilltech.promises.Promise.rejected;
import static com.onehilltech.promises.Promise.resolved;

public class InstanceIdService extends FirebaseInstanceIdService
{
  private final Logger logger_ = LoggerFactory.getLogger (InstanceIdService.class);

  @Override
  public void onTokenRefresh ()
  {
    this.logger_.info ("Refreshing cloud token; sending cloud token to server");

    CloudToken cloudToken = new CloudToken ();
    FirebaseInstanceId instanceId = FirebaseInstanceId.getInstance ();

    cloudToken.device = instanceId.getId ();
    cloudToken.token = instanceId.getToken ();

    MessagingClient.getInstance (this)
                   .refreshToken (cloudToken)
                   .then (resolved (this::saveClaimTicket))
                   ._catch (rejected (reason -> this.logger_.error ("Failed to refresh cloud token", reason)));
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
}
