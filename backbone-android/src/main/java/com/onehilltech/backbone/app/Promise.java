package com.onehilltech.backbone.app;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Promise <T>
  implements Runnable
{

  public interface OnResolved <T>
  {
    void onResolved (T value);
  }

  public interface OnRejected
  {
    void onRejected (Throwable reason);
  }

  private T resolved_;

  private Throwable rejected_;

  private OnResolved <T> onResolved_;

  private OnRejected onRejected_;

  private static final ExecutorService PROMISE_EXECUTOR = Executors.newCachedThreadPool ();

  private final PromiseExecutor <T> impl_;

  public Promise (PromiseExecutor <T> impl)
  {
    this.impl_ = impl;
  }

  public void then (OnResolved <T> onResolved)
  {
    this.then (onResolved, null);
  }

  public void then (OnResolved <T> onResolved, OnRejected onRejected)
  {
    // Store the resolve and rejected callbacks.
    this.onResolved_ = onResolved;
    this.onRejected_ = onRejected;

    // Place the promise on the executor.
    PROMISE_EXECUTOR.execute (this);
  }

  @Override
  public void run ()
  {
    if (this.resolved_ != null)
    {
      // The promise has already been resolved. Let's go ahead and pass the value
      // to the caller.
      if (this.onResolved_ != null)
        this.onResolved_.onResolved (this.resolved_);
    }
    else if (this.rejected_ != null)
    {
      // The promise has already been rejected. Let's go ahead and pass the reason back
      // to the caller.
      if (this.onRejected_ != null)
        this.onRejected_.onRejected (this.rejected_);
    }
    else
    {
      // Execute the promise. This method must call either resolve or reject
      // before this method return. Failure to do so means the promise was not
      // completed, and in a bad state.
      this.impl_.execute (this.completion_);

      // Call the correct callback method, or throw an exception. The client cannot
      // handle the exception, which will force a stack trace.
      if (this.resolved_ != null)
        this.onResolved_.onResolved (this.resolved_);
      else if (this.rejected_ != null)
        this.onRejected_.onRejected (this.rejected_);
      else
        throw new IllegalStateException ("Execute method must call resolve() or reject()");
    }
  }

  /**
   * Check if the promise is pending.
   *
   * @return
   */
  public boolean isPending ()
  {
    return this.rejected_ == null && this.resolved_ == null;
  }

  /**
   * Implementation of the completion callback for this promise.
   */
  private final PromiseCompletion <T> completion_ = new PromiseCompletion <T> ()
  {
    @Override
    public void resolve (T value)
    {
      // Check that the promise is still pending.
      if (!isPending ())
        throw new IllegalStateException ("Promise already resolved/rejected");

      resolved_ = value;
    }

    @Override
    public void reject (Throwable reason)
    {
      // Check that the promise is still pending.
      if (!isPending ())
        throw new IllegalStateException ("Promise already resolved/rejected");

      rejected_ = reason;
    }
  };
}
