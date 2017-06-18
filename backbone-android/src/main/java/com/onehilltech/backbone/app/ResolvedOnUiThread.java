package com.onehilltech.backbone.app;

import android.support.annotation.NonNull;

public class ResolvedOnUiThread <T, U> extends OnUIThread
    implements Promise.OnResolved <T, U>
{
  private final Promise.OnResolved <T, U> onResolved_;

  private final ContinuationPromise cont_ = new ContinuationPromise ();

  private T value_;

  public ResolvedOnUiThread (@NonNull Promise.OnResolved <T, U> onResolved)
  {
    this.onResolved_ = onResolved;
  }

  @Override
  public Promise onResolved (T value)
  {
    this.value_ = value;

    this.runOnUiThread ();

    return this.cont_;
  }

  @Override
  protected void run ()
  {
    Promise <?> promise = this.onResolved_.onResolved (this.value_);

    if (promise != null)
      this.cont_.settle (promise);
    else
      this.cont_.settle (Promise.resolve (null));
  }
}
