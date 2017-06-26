package com.onehilltech.backbone.app;

import android.support.annotation.NonNull;

/**
 * Proxy that run the OnRejected handler on the UI thread.
 */
public class RejectedOnUIThread extends OnUIThread
    implements Promise.OnRejected
{
  /**
   * Factory method that supports using a lambda function. It also removes the need
   * for using the new method so its usage in the Promise statements reads more fluid.
   *
   * @param onRejected      The real handler
   * @return                Promise.OnRejected object
   */
  public static Promise.OnRejected rejectOnUiThread (Promise.OnRejected onRejected)
  {
    return new RejectedOnUIThread (onRejected);
  }

  /// The real OnRejected handler.
  private final Promise.OnRejected onRejected_;

  /// Mock continuation promise.
  private final ContinuationPromise cont_ = new ContinuationPromise ();

  /// The reason for the failure.
  private Throwable reason_;

  /**
   * Initializing constructor.
   *
   * @param onRejected        The real object
   */
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

  @SuppressWarnings ("unchecked")
  @Override
  protected void run ()
  {
    try
    {
      Promise promise = this.onRejected_.onRejected (this.reason_);
      this.cont_.continueWith (promise);
    }
    catch (Exception e)
    {
      this.cont_.continueWith (e);
    }
  }
}
