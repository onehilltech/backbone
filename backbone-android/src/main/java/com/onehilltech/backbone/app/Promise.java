package com.onehilltech.backbone.app;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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
  /**
   * @interface Settlement
   *
   * Settlement interface used to either resolve or reject a promise.
   *
   * @param <T>
   */
  public interface Settlement <T>
  {
    void resolve (T value);

    void reject (Throwable reason);
  }

  /**
   * @interface OnResolved
   *
   * Function for resolving a promise. The promise implementation has the option
   * of chaining another promise that will be used to get the value for resolve handlers
   * later in the chain.
   *
   * @param <T>
   * @param <U>
   */
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

  protected final ArrayList <OnResolved <T, ?>> onResolved_ = new ArrayList<> ();

  protected final ArrayList <OnRejected> onRejected_ = new ArrayList<> ();

  protected static class Continuation
  {
    Continuation (ContinuationPromise promise, ContinuationExecutor executor)
    {
      this.promise = promise;
      this.executor = executor;
    }

    final ContinuationPromise promise;
    final ContinuationExecutor executor;
  }

  protected final ArrayList <Continuation> cont_ = new ArrayList<> ();

  private boolean isRunning_ = false;

  private final Object stateLock_ = new Object ();

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
    if (this.isPending () && this.impl_ != null)
      this.executor_.execute (this::runInBackground);
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
  public <U> Promise <U> then (OnResolved <T, U> onResolved, @Nullable OnRejected onRejected)
  {
    if (onResolved != null)
      this.onResolved_.add (onResolved);

    if (onRejected != null)
      this.onRejected_.add (onRejected);

    ContinuationPromise <U> contPromise = new ContinuationPromise<> ();
    ContinuationExecutor <U> contExecutor = contPromise::evaluate;

    this.cont_.add (new Continuation (contPromise, contExecutor));

    if (this.isResolved ())
    {
      this.executor_.execute (() -> onResolved.onResolved (this.value_, promise -> contPromise.evaluate (promise)));
    }
    else if (onRejected != null && this.isRejected ())
    {
      this.executor_.execute (() -> onRejected.onRejected (this.rejection_));
    }

    return contPromise;
  }

  @SuppressWarnings ("unchecked")
  private void runInBackground ()
  {
    this.executor_.execute (() -> {
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

            // Execute the resolved callback on a different thread of execution. We pass
            // a continuation executor to the callback just in case we must execute another
            // promise after this promise has been resolved.
            executor_.execute (() -> processResolve ());
          }

          @Override
          public void reject (Throwable reason)
          {
            // Check that the promise is still pending.
            if (!isPending ())
              throw new IllegalStateException ("Promise already resolved/rejected");

            executor_.execute (() -> processRejection (reason));
          }
        });
      }
      catch (Exception e)
      {
        this.executor_.execute (() -> processRejection (e));
      }
    });
  }

  private void processResolve ()
  {
    for (OnResolved <T, ?> onResolved: this.onResolved_)
    {
      onResolved.onResolved (this.value_, (promise -> {
        for (Continuation cont : this.cont_)
          cont.promise.evaluate (promise);
      }));
    }
  }

  /**
   * Bubble the rejection.
   *
   * @param reason
   */
  void processRejection (Throwable reason)
  {
    this.rejection_ = reason;
    this.processCurrentRejection ();
  }

  /**
   * Bubble the current rejection.
   */
  private void processCurrentRejection ()
  {
    // If the rejection was set here, then we can stop bubbling the rejection
    // at this promise. Otherwise, we need to continue to the next promise
    // in the chain.
    for (OnRejected onRejected : this.onRejected_)
      onRejected.onRejected (this.rejection_);

    for (Continuation cont: this.cont_)
      cont.promise.processRejection (this.rejection_);
  }

  public Promise <T> _catch (@NonNull OnRejected onRejected)
  {
    // Always add the rejected function to the list of rejection handlers. This way,
    // we know how many have been attached to this promise. Then, run this rejection
    // handler if the promise has already been rejected.

    this.onRejected_.add (onRejected);

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
