package com.onehilltech.backbone.app;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Executor;
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

  private T resolve_;

  private Throwable rejection_;

  private OnResolved <T> onResolved_;

  private OnRejected onRejected_;

  private static final ExecutorService DEFAULT_EXECUTOR = Executors.newCachedThreadPool ();

  private final PromiseFulfill<T> impl_;

  private final Executor executor_;

  public Promise (PromiseFulfill<T> impl)
  {
    this.impl_ = impl;
    this.executor_ = DEFAULT_EXECUTOR;
  }

  /**
   * Fulfill the promise.
   *
   * @param onResolved
   */
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
    this.executor_.execute (this);
  }

  @Override
  public void run ()
  {
    if (this.resolve_ != null)
    {
      // The promise has already been resolved. Let's go ahead and pass the value
      // to the caller.
      if (this.onResolved_ != null)
        this.onResolved_.onResolved (this.resolve_);
    }
    else if (this.rejection_ != null)
    {
      // The promise has already been rejected. Let's go ahead and pass the reason back
      // to the caller.
      if (this.onRejected_ != null)
        this.onRejected_.onRejected (this.rejection_);
    }
    else
    {
      // Execute the promise. This method must call either resolve or reject
      // before this method return. Failure to do so means the promise was not
      // completed, and in a bad state.
      this.impl_.execute (this.completion_);
    }
  }

  /**
   * Check if the promise is pending.
   *
   * @return
   */
  public boolean isPending ()
  {
    return this.rejection_ == null && this.resolve_ == null;
  }

  /**
   * Fulfill all the promises.
   *
   * @param promises
   * @return
   */
  public static Promise <Collection <Object>> all (Promise... promises)
  {
    return new Promise<> ((fulfill) -> {
      ArrayList <Object> results = new ArrayList<> (promises.length);

      for (Promise <?> promise : promises) {
        // Execute the promise.
        promise.run ();

        // Extract the result from the promise, and then add it
        if (promise.resolve_ != null)
        {

        }
        else if (promise.rejection_ != null)
        {
          // This promise failed.
          break;
        }
      }
    });
  }

  /**
   * Implementation of the completion callback for this promise.
   */
  private final PromiseFulfillment<T> completion_ = new PromiseFulfillment<T> ()
  {
    @Override
    public void resolve (T value)
    {
      // Check that the promise is still pending.
      if (!isPending ())
        throw new IllegalStateException ("Promise already resolved/rejected");

      // Store the value, and notify the client.
      resolve_ = value;

      executor_.execute (() -> {
        if (onResolved_ != null)
          onResolved_.onResolved (resolve_);
      });
    }

    @Override
    public void reject (Throwable reason)
    {
      // Check that the promise is still pending.
      if (!isPending ())
        throw new IllegalStateException ("Promise already resolved/rejected");

      // Store the reason, and notify the client.
      rejection_ = reason;

      executor_.execute (() -> {
        if (onRejected_ != null)
          onRejected_.onRejected (rejection_);
      });
    }
  };
}
