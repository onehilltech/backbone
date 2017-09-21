package com.onehilltech.backbone.data;

import okhttp3.Request;

/**
 * Adapter for the DataStore. The DataStoreAdapter allows clients to customize
 * a request before it is processed.
 */
public interface DataStoreAdapter
{
  Request handleRequest (Request request);
}
