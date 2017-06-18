package com.onehilltech.backbone.app;

import android.support.annotation.NonNull;

public class RejectedOnUIThread extends OnUIThread
    implements Promise.OnRejected
{
  private final Promise.OnRejected onRejected_;

  private final ContinuationPromise cont_ = new ContinuationPromise ();

  private Throwable reason_;

  public RejectedOnUIThread (@NonNull Promise.OnRejected onRejected)
  {
    this.onRejected_ = onRejected;
  }

  @Override
  public Promise onRejected (Throwable reason)
  {
    this.reason_ = reason;

    this.runOnUiThread ();

    return this.cont_;
  }

  @Override
  protected void run ()
  {
    Promise promise = this.onRejected_.onRejected (this.reason_);

    if (promise != null)
      this.cont_.settle (promise);
    else
      this.cont_.settle (Promise.resolve (null));
  }
}
