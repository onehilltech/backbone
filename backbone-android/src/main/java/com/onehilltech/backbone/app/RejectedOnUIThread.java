package com.onehilltech.backbone.app;

import android.support.annotation.NonNull;

public class RejectedOnUIThread extends OnUIThread
    implements Promise.OnRejected
{
  private final Promise.OnRejected onRejected_;

  private Throwable reason_;

  public RejectedOnUIThread (@NonNull Promise.OnRejected onRejected)
  {
    this.onRejected_ = onRejected;
  }

  @Override
  public void onRejected (Throwable reason)
  {
    this.reason_ = reason;
    this.runOnUiThread ();
  }

  @Override
  protected void run ()
  {
    this.onRejected_.onRejected (this.reason_);
  }
}
