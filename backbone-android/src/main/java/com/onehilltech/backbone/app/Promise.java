package com.onehilltech.backbone.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @class Promise
 *
 * A promise is an object which can be returned synchronously from an asynchronous
 * function.
 *
 * @param <T>
 */
public class Promise <T>
  implements Runnable
{
  public interface Settlement <T>
  {
    void resolve (T value);

    void reject (Throwable reason);
  }

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

  private final PromiseExecutor<T> impl_;

  private final Executor executor_;

  public Promise (PromiseExecutor<T> impl)
  {
    this.impl_ = impl;
    this.executor_ = DEFAULT_EXECUTOR;
  }

  private Promise (T resolve)
  {
    this.impl_ = null;
    this.resolve_ = resolve;
    this.executor_ = DEFAULT_EXECUTOR;
  }

  private Promise (Throwable reason)
  {
    this.impl_ = null;
    this.rejection_ = reason;
    this.executor_ = DEFAULT_EXECUTOR;
  }

  /**
   * Settle the promise.
   *
   * @param onResolved
   */
  public void then (OnResolved <T> onResolved)
  {
    this.then (onResolved, null);
  }

  /**
   * Settle the promise. The promised will either be resolved or rejected.
   *
   * @param onResolved
   * @param onRejected
   */
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
   * Test if the promise has been rejected.
   *
   * @return
   */
  public boolean isRejected ()
  {
    return this.rejection_ != null;
  }

  /**
   * Get the reason the promise was rejected.
   *
   * @return
   */
  public Throwable getReason ()
  {
    return this.rejection_;
  }

  /**
   * Test if the promise has been resolved, or fulfilled.
   *
   * @return
   */
  public boolean isResolved ()
  {
    return this.resolve_ != null;
  }

  /**
   * Create a Promise that is already resolved.
   *
   * @param value
   * @param <T>
   * @return
   */
  public static <T> Promise <T> resolve (T value)
  {
    return new Promise<> (value);
  }

  /**
   * Create a promise that is already rejected.
   *
   * @param reason
   * @return
   */
  public static Promise <?> reject (Throwable reason)
  {
    return new Promise<> (reason);
  }

  /**
   * Settle a collection of promises.
   *
   * @param promises
   * @return
   */
  public static Promise <List <Object>> all (Promise <?>... promises)
  {
    return all (Arrays.asList (promises));
  }

  /**
   * Settle a collection of promises.
   *
   * @param promises
   * @return
   */
  public static Promise <List <Object>> all (List <Promise <?>> promises)
  {
    return new Promise<> ((settlement) -> {
      ArrayList <Object> results = new ArrayList<> (promises.size ());

      if (!promises.isEmpty ())
      {
        Iterator<Promise<?>> iterator = promises.iterator ();

        // The first promise in the collection that is rejected causes all promises
        // to be rejected.
        final OnRejected onRejected = (reason) -> settlement.reject (reason);

        final OnResolved onResolved = new OnResolved ()
        {
          @Override
          public void onResolved (Object value)
          {
            // Add the resolved value to the result set.
            results.add (value);

            if (iterator.hasNext ())
            {
              // We have more promises to resolve. So, let's move to the next one and
              // attempt to resolve it.
              Promise<?> promise = iterator.next ();
              promise.then (this, onRejected);
            } else
            {
              // We have fulfilled all the promises. We can return control to the
              // client so it can continue.
              settlement.resolve (results);
            }
          }
        };

        // Start resolving the promises.
        Promise<?> promise = iterator.next ();
        promise.then (onResolved, onRejected);
      }
      else
      {
        // There are no promises to resolve. We can just issue our settlement.
        settlement.resolve (results);
      }
    });
  }

  /**
   * Implementation of the completion callback for this promise.
   */
  private final Settlement<T> completion_ = new Settlement<T> ()
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
