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
  /**
   * Factory method that supports using a lambda function. It also removes the need
   * for using the new method so its usage in the Promise statements reads more fluid.
   *
   * @param onResolved
   * @param <T>
   * @param <U>
   * @return
   */
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

  @SuppressWarnings ("unchecked")
  @Override
  public Promise onResolved (T value)
  {
    this.value_ = value;

    this.runOnUiThread ();

    return this.cont_;
  }

  @SuppressWarnings ("unchecked")
  @Override
  protected void run ()
  {
    try
    {
      Promise<?> promise = this.onResolved_.onResolved (this.value_);
      this.cont_.continueWith (promise);
    }
    catch (Exception e)
    {
      this.cont_.continueWith (e);
    }
  }
}
