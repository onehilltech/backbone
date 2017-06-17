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
  @FunctionalInterface
  public interface OnResolved <T, U>
  {
    void onResolved (T value, ContinuationExecutor <U> cont);
  }

  @FunctionalInterface
  public interface OnRejected <T>
  {
    void onRejected (Throwable reason, ContinuationExecutor <T> cont);
  }

  public enum Status
  {
    Pending,
    Resolved,
    Rejected
  }

  /// The resolved value for the promise.
  private T value_;

  private Status status_;

  /// The rejected value for the promise.
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

  /**
   * The executor for the Promise.
   *
   * @param impl
   */
  public Promise (PromiseExecutor<T> impl)
  {
    this (impl, Status.Pending, null, null);
  }

  /**
   * Create a Promise that is resolved.
   *
   * @param resolve
   */
  private Promise (T resolve)
  {
    this (null, Status.Resolved, resolve, null);
  }

  /**
   * Create a Promise that is rejected.
   *
   * @param reason
   */
  private Promise (Throwable reason)
  {
    this (null, Status.Rejected, null, reason);
  }

  /**
   * Initializing constructor.
   *
   * @param impl
   * @param resolve
   * @param reason
   */
  private Promise (PromiseExecutor<T> impl, Status status, T resolve, Throwable reason)
  {
    this.impl_ = impl;
    this.value_ = resolve;
    this.rejection_ = reason;
    this.status_ = status;
    this.executor_ = DEFAULT_EXECUTOR;

    // If the promise is not pending, then we need to settle the promise. We also
    // need to settle the promise in the background so normal control can continue.
    if (this.status_ == Status.Pending && this.impl_ != null)
      this.executor_.execute (this::runInBackground);
  }

  public Status getStatus ()
  {
    return this.status_;
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

  static class RejectionHandled
  {
    private boolean handled = false;
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

    if (this.status_ == Status.Resolved)
    {
      // The promise is already resolved. If we can, we are going to pass the
      // value to the handler. We also need to run all the "settled" handlers.

      if (onResolved != null)
        this.executor_.execute (() -> onResolved.onResolved (this.value_, promise -> contPromise.evaluate (promise)));
    }
    else if (this.status_ == Status.Rejected)
    {
      if (onRejected != null)
      {
        final RejectionHandled rejectionHandled = new RejectionHandled ();

        // We are handling the rejection as this level. If the client calls the
        // continuation function, then we are going to use that value. Otherwise,
        // let's start the next chain on fresh start.
        this.executor_.execute (() -> {
          onRejected.onRejected (this.rejection_, promise -> {
            rejectionHandled.handled = true;
            contPromise.evaluate (promise);
          });

          if (!rejectionHandled.handled)
            contPromise.processResolve (null);
        });
      }
      else
      {
        // Pass the rejection up a single level.
        contPromise.processRejection (this.rejection_);
      }
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
            if (status_ != Status.Pending)
              throw new IllegalStateException ("Promise already resolved/rejected");

            // Cache the result of the promise.
            status_ = Status.Resolved;
            value_ = value;

            // Execute the resolved callback on a different thread of execution. We pass
            // a continuation executor to the callback just in case we must execute another
            // promise after this promise has been resolved.
            processCurrentResolve ();
          }

          @Override
          public void reject (Throwable reason)
          {
            // Check that the promise is still pending.
            if (status_ != Status.Pending)
              throw new IllegalStateException ("Promise already resolved/rejected");

            status_ = Status.Rejected;
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

  private void processCurrentResolve ()
  {
    for (OnResolved <T, ?> onResolved: this.onResolved_)
    {
      this.executor_.execute (() ->
        onResolved.onResolved (this.value_, (promise -> {
          for (Continuation cont : this.cont_)
            cont.promise.evaluate (promise);
        }))
      );
    }

    for (Continuation continuation: this.cont_)
      this.executor_.execute (() -> continuation.promise.processResolve (this.value_));
  }

  protected void processResolve (T value)
  {
    this.value_ = value;
    this.status_ = Status.Resolved;

    this.processCurrentResolve ();
  }

  /**
   * Bubble the rejection.
   *
   * @param reason
   */
  protected void processRejection (Throwable reason)
  {
    this.rejection_ = reason;
    this.status_ = Status.Rejected;

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

    for (OnRejected <T> onRejected: this.onRejected_)
    {
      this.executor_.execute (() -> onRejected.onRejected (this.rejection_, (promise -> {
        for (Continuation cont : this.cont_)
          cont.promise.evaluate (promise);
      })));
    }

    if (this.onRejected_.isEmpty ())
    {
      for (Continuation cont : this.cont_)
        this.executor_.execute (() -> cont.promise.processRejection (this.rejection_));
    }
  }

  public <U> Promise <U> _catch (@NonNull OnRejected onRejected)
  {
    return this.then (null, onRejected);
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
      final OnRejected onRejected = (reason, cont) -> settlement.reject (reason);

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
      final OnRejected onRejected = (reason, cont) -> {
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
