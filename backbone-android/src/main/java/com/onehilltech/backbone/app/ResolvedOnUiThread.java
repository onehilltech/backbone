package com.onehilltech.backbone.app;

import android.support.annotation.NonNull;

public class ResolvedOnUiThread <T, U> extends OnUIThread
    implements Promise.OnResolved <T, U>
{
  private final Promise.OnResolved <T, U> onResolved_;

  private T value_;

  private ContinuationExecutor<U> cont_;

  public ResolvedOnUiThread (@NonNull Promise.OnResolved <T, U> onResolved)
  {
    this.onResolved_ = onResolved;
  }

  @Override
  public void onResolved (T value, ContinuationExecutor<U> cont)
  {
    this.value_ = value;
    this.cont_ = cont;

    this.runOnUiThread ();
  }

  @Override
  protected void run ()
  {
    this.onResolved_.onResolved (this.value_, this.cont_);
  }
}
