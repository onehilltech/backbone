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
{
  public interface Settlement <T>
  {
    void resolve (T value);

    void reject (Throwable reason);
  }

  public interface OnResolved <T, U>
  {
    void onResolved (T value, ContinuationExecutor <U> cont);
  }

  public interface OnRejected
  {
    void onRejected (Throwable reason);
  }

  private T value_;

  protected Throwable rejection_;

  private static final ExecutorService DEFAULT_EXECUTOR = Executors.newCachedThreadPool ();

  private final PromiseExecutor<T> impl_;

  private final Executor executor_;

  protected ContinuationPromise <?> next_;

  protected ContinuationExecutor nextExecutor_;

  protected OnResolved <T, ?> onResolved_;

  protected OnRejected onRejected_;

  private boolean isRunning_ = false;

  private final Object runLock_ = new Object ();

  public Promise (PromiseExecutor<T> impl)
  {
    this (impl, null, null);
  }

  private Promise (T resolve)
  {
    this (null, resolve, null);
  }

  private Promise (Throwable reason)
  {
    this (null, null, reason);
  }

  private Promise (PromiseExecutor<T> impl, T resolve, Throwable reason)
  {
    this.impl_ = impl;
    this.value_ = resolve;
    this.rejection_ = reason;
    this.executor_ = DEFAULT_EXECUTOR;

    // Force the promise to start.
    this.runInBackground ();
  }

  /**
   * Settle the promise.
   *
   * @param onResolved
   */
  public <U> Promise <U> then (OnResolved <T, U> onResolved)
  {
    return this.then (onResolved, null);
  }

  /**
   * Settle the promise. The promised will either be resolved or rejected.
   *
   * @param onResolved
   * @param onRejected
   */
  public <U> Promise <U> then (OnResolved <T, U> onResolved, OnRejected onRejected)
  {
    this.onResolved_ = onResolved;
    this.onRejected_ = onRejected;

    ContinuationPromise <U> continuation = new ContinuationPromise<> ();
    ContinuationExecutor <U> continuationExecutor = continuation::evaluate;

    this.next_ = continuation;
    this.nextExecutor_ = continuationExecutor;

    // Run the promise implementation.
    this.runInBackground ();

    return continuation;
  }

  private void runInBackground ()
  {
    this.executor_.execute (this::run);
  }

  private synchronized void run ()
  {
    if (this.value_ != null)
    {
      // The promise has already been resolved. Let's go ahead and pass the value
      // to the caller.
      if (this.onResolved_ != null)
        this.onResolved_.onResolved (this.value_, this.nextExecutor_);
    }
    else if (this.rejection_ != null)
    {
      // The promise has already been rejected. Let's go ahead and pass the reason
      // back to the caller.
      this.bubbleCurrentRejection ();
    }
    else if (!this.isRunning_ && this.impl_ != null)
    {
      // The promise is now running.
      this.isRunning_ = true;

      // Execute the promise. This method must call either resolve or reject
      // before this method return. Failure to do so means the promise was not
      // completed, and in a bad state.
      try
      {
        this.impl_.execute (new Settlement<T> ()
        {
          @Override
          public void resolve (T value)
          {
            // Check that the promise is still pending.
            if (!isPending ())
              throw new IllegalStateException ("Promise already resolved/rejected");

            // Cache the result of the promise.
            value_ = value;

            if (onResolved_ != null)
            {
              // Execute the resolved callback on a different thread of execution. We pass
              // a continuation executor to the callback just in case we must execute another
              // promise after this promise has been resolved.
              executor_.execute (() -> onResolved_.onResolved (value_, nextExecutor_));
            }
          }

          @Override
          public void reject (Throwable reason)
          {
            // Check that the promise is still pending.
            if (!isPending ())
              throw new IllegalStateException ("Promise already resolved/rejected");

            executor_.execute (() -> bubbleRejection (reason));
          }
        });
      }
      catch (Exception e)
      {
        this.executor_.execute (() -> bubbleRejection (e));
      }
    }
  }

  /**
   * Bubble the rejection.
   *
   * @param reason
   */
  void bubbleRejection (Throwable reason)
  {
    this.rejection_ = reason;
    this.bubbleCurrentRejection ();
  }

  /**
   * Bubble the current rejection.
   */
  private void bubbleCurrentRejection ()
  {
    // If the rejection was set here, then we can stop bubbling the rejection
    // at this promise. Otherwise, we need to continue to the next promise
    // in the chain.
    if (this.onRejected_ != null)
      this.onRejected_.onRejected (this.rejection_);
    else if (this.next_ != null)
      this.next_.bubbleRejection (this.rejection_);
  }

  public Promise <T> _catch (OnRejected onRejected)
  {
    this.onRejected_ = onRejected;

    if (this.isRejected ())
      this.executor_.execute (() -> onRejected.onRejected (this.rejection_));

    return this;
  }

  /**
   * Check if the promise is pending.
   *
   * @return
   */
  public boolean isPending ()
  {
    return this.rejection_ == null && this.value_ == null;
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
    return this.value_ != null;
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
  public static <T> Promise <T> reject (Throwable reason)
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
    if (promises.isEmpty ())
      return Promise.resolve (new ArrayList<> ());

    return new Promise<> ((settlement) -> {
      ArrayList <Object> results = new ArrayList<> (promises.size ());
      Iterator<Promise<?>> iterator = promises.iterator ();

      // The first promise in the collection that is rejected causes all promises
      // to be rejected.
      final OnRejected onRejected = (reason) -> settlement.reject (reason);

      final OnResolved onResolved = new OnResolved ()
      {
        @Override
        public void onResolved (Object value, ContinuationExecutor cont)
        {
          // Add the resolved value to the result set.
          results.add (value);

          if (iterator.hasNext ())
          {
            // We have more promises to resolve. So, let's move to the next one and
            // attempt to resolve it.
            Promise<?> promise = iterator.next ();
            promise.then (this, onRejected);
          }
          else
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
    });
  }

  /**
   * This method returns a promise that resolves or rejects as soon as one of the
   * promises in the iterable resolves or rejects, with the value or reason from
   * that promise.
   *
   * @param promises
   * @param <U>
   * @return
   */
  public static <U> Promise <U> race (Promise <U>... promises)
  {
    return race (Arrays.asList (promises));
  }

  /**
   * This method returns a promise that resolves or rejects as soon as one of the
   * promises in the iterable resolves or rejects, with the value or reason from
   * that promise.
   *
   * @param promises
   * @param <U>
   * @return
   */
  public static <U> Promise <U> race (List <Promise <U>> promises)
  {
    if (promises.isEmpty ())
      return Promise.resolve (null);

    final Object lock = new Object ();

    return new Promise<> ((settlement) -> {
      // The first promise in the collection that is rejected causes all promises
      // to be rejected.
      final OnRejected onRejected = reason -> {
        synchronized (lock)
        {
          try
          {
            settlement.reject (reason);
          }
          catch (IllegalStateException e)
          {
            // Do nothing since we are not the first to finish
          }
        }
      };

      final OnResolved <U, ?> onResolved = (value, cont) -> {
        synchronized (lock)
        {
          try
          {
            settlement.resolve (value);
          }
          catch (IllegalStateException e)
          {
            // Do nothing since we were not the first to finish.
          }
        }
      };

      for (Promise <U> promise: promises)
        promise.then (onResolved, onRejected);
    });
  }
}
