package com.onehilltech.backbone.app;

import android.support.annotation.NonNull;


/**
 * Proxy that run the OnResolved handler on the UI thread.
 */
public class ResolvedOnUIThread <T, U> extends OnUIThread
    implements Promise.OnResolved <T, U>
{
  /**
   * Factory method that supports using a lambda function. It also removes the need
   * for using the new method so its usage in the Promise statements reads more fluid.
   *
   * @param onResolved        The real handler
   * @param <T>               Parameter type of the current value
   * @param <U>               Parameter type of the next value
   * @return                  Promise.OnResolved object
   */
  public static <T, U> Promise.OnResolved <T, U> resolveOnUiThread (Promise.OnResolved <T, U> onResolved)
  {
    return new ResolvedOnUIThread<> (onResolved);
  }

  /// The real OnResolved handler.
  private final Promise.OnResolved <T, U> onResolved_;

  /// Mock continuation promise.
  private final ContinuationPromise cont_ = new ContinuationPromise ();

  /// The value of the settlement.
  private T value_;

  /**
   * Initializing constructor.
   *
   * @param onResolved        The real handler
   */
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
