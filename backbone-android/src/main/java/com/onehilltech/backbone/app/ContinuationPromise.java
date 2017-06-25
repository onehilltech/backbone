package com.onehilltech.backbone.app;

import android.support.annotation.Nullable;

class ContinuationPromise <T> extends Promise <T>
{
  public ContinuationPromise ()
  {
    super (null);
  }

  @SuppressWarnings ("unchecked")
  public void continueWith (@Nullable Promise <T> promise)
  {
    if (promise != null)
      promise.then (resolved (this::onResolve), rejected (this::onReject));
    else
      onResolve (null);
  }

  public void continueWith (Throwable t)
  {
    onReject (t);
  }
}
