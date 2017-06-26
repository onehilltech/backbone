package com.onehilltech.backbone.app;

import android.support.annotation.NonNull;


/**
 * @class ResolvedOnUIThread
 *
 * Run the OnResolved handler on the UI thread.
 */
public class ResolvedOnUIThread <T, U> extends OnUIThread
    implements Promise.OnResolved <T, U>
{
  public static <T, U> Promise.OnResolved <T, U> resolveOnUiThread (Promise.OnResolved <T, U> onResolved)
  {
    return new ResolvedOnUIThread<> (onResolved);
  }

  private final Promise.OnResolved <T, U> onResolved_;

  private final ContinuationPromise cont_ = new ContinuationPromise ();

  private T value_;

  private ResolvedOnUIThread (@NonNull Promise.OnResolved <T, U> onResolved)
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
      this.cont_.continueWith (promise);
    else
      this.cont_.continueWith (Promise.resolve (null));
  }
}
