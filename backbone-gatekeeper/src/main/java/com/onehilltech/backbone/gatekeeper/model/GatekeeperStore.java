package com.onehilltech.backbone.gatekeeper.model;

import android.content.Context;

import com.onehilltech.backbone.data.DataStore;
import com.onehilltech.backbone.data.DataStoreAdapter;
import com.onehilltech.backbone.data.serializers.ObjectIdSerializer;
import com.onehilltech.backbone.gatekeeper.GatekeeperClient;
import com.onehilltech.backbone.gatekeeper.GatekeeperSessionClient;
import com.onehilltech.backbone.objectid.ObjectId;

import okhttp3.CacheControl;

public class GatekeeperStore
{
  private static DataStoreAdapter dataStoreAdapter_ = request ->
      request.url ().encodedPath ().endsWith ("/accounts/me") && request.method ().equals ("GET") ?
          request.newBuilder ().cacheControl (CacheControl.FORCE_NETWORK).build () :
          request;

  public static DataStore open (Context context, GatekeeperSessionClient sessionClient)
  {
    return new DataStore.Builder (context, GatekeeperDatabase.class)
        .setBaseUrl (sessionClient.getClient ().getConfig ().baseUri)
        .setApplicationAdapter (dataStoreAdapter_)
        .setHttpClient (sessionClient.getHttpClient ())
        .addTypeAdapter (ObjectId.class, new ObjectIdSerializer ())
        .build ();
  }

  public static DataStore open (Context context, GatekeeperClient client)
  {
    return new DataStore.Builder (context, GatekeeperDatabase.class)
        .setBaseUrl (client.getConfig ().baseUri)
        .setApplicationAdapter (dataStoreAdapter_)
        .setHttpClient (client.getHttpClient ())
        .addTypeAdapter (ObjectId.class, new ObjectIdSerializer ())
        .build ();
  }
}
