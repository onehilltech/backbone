package com.onehilltech.backbone.app;

import android.support.annotation.NonNull;

/**
 * @class RejectedOnUIThread
 *
 * Run the OnRejected handler on the UI thread.
 */
public class RejectedOnUIThread extends OnUIThread
    implements Promise.OnRejected
{
  /**
   * Factory method that supports using a lambda function. It also removes the need
   * for using the new method so its usage in the Promise statements reads more fluid.
   *
   * @param onRejected
   * @return
   */
  public static Promise.OnRejected rejectOnUiThread (Promise.OnRejected onRejected)
  {
    return new RejectedOnUIThread (onRejected);
  }

  private final Promise.OnRejected onRejected_;

  private final ContinuationPromise cont_ = new ContinuationPromise ();

  private Throwable reason_;

  private RejectedOnUIThread (@NonNull Promise.OnRejected onRejected)
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
      this.cont_.continueWith (promise);
    else
      this.cont_.continueWith (Promise.resolve (null));
  }
}
