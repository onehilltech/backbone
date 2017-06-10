package com.onehilltech.backbone.firebase;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class MessagingService extends FirebaseMessagingService
{
  private final Logger logger_ = LoggerFactory.getLogger (MessagingService.class);

  public interface MessageHandler
  {
    void handleMessage (RemoteMessage msg);
  }

  private final HashMap <String, MessageHandler> handlers_ = new HashMap<> ();

  @Override
  public void onCreate ()
  {
    super.onCreate ();
  }

  @Override
  public void onMessageReceived (RemoteMessage msg)
  {
    if (!msg.getData ().isEmpty ())
    {
      this.logger_.info ("Received message {} from {}", msg.getMessageId (), msg.getFrom ());

      String type = msg.getData ().get ("type");
      MessageHandler handler = this.handlers_.get (type);

      if (handler != null)
        handler.handleMessage (msg);
    }
  }
}