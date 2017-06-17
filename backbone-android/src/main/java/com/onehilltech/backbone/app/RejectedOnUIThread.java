package com.onehilltech.backbone.app;

import android.support.annotation.NonNull;

public class RejectedOnUIThread <U> extends OnUIThread
    implements Promise.OnRejected <U>
{
  private final Promise.OnRejected onRejected_;

  private Throwable reason_;

  private ContinuationExecutor<U> cont_;

  public RejectedOnUIThread (@NonNull Promise.OnRejected onRejected)
  {
    this.onRejected_ = onRejected;
  }

  @Override
  public void onRejected (Throwable reason, ContinuationExecutor<U> cont)
  {
    this.reason_ = reason;
    this.cont_ = cont;

    this.runOnUiThread ();
  }

  @Override
  protected void run ()
  {
    this.onRejected_.onRejected (this.reason_, this.cont_);
  }
}
