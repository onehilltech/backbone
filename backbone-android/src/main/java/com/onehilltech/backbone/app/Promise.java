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

  protected OnResolved <T, ?> onResolved_;

  protected OnRejected onRejected_;

  public Promise (PromiseExecutor<T> impl)
  {
    this.impl_ = impl;
    this.executor_ = DEFAULT_EXECUTOR;
  }

  private Promise (T resolve)
  {
    this.impl_ = null;
    this.value_ = resolve;
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
    ContinuationExecutor <U> continuationExecutor = promise -> continuation.evaluate (promise);

    this.next_ = continuation;

    // Place the promise on the executor.
    this.executor_.execute (()-> {
      if (this.value_ != null)
      {
        // The promise has already been resolved. Let's go ahead and pass the value
        // to the caller.
        if (onResolved != null)
          onResolved.onResolved (this.value_, continuationExecutor);
      }
      else if (this.rejection_ != null)
      {
        // The promise has already been rejected. Let's go ahead and pass the reason
        // back to the caller.
        if (onRejected != null)
          onRejected.onRejected (this.rejection_);
        else
          continuation.bubbleRejection (this.rejection_);
      }
      else if (this.impl_ != null)
      {
        // Execute the promise. This method must call either resolve or reject
        // before this method return. Failure to do so means the promise was not
        // completed, and in a bad state.
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

            if (onResolved == null)
              return;

            // Execute the resolved callback on a different thread of execution. We pass
            // a continuation executor to the callback just in case we must execute another
            // promise after this promise has been resolved.
            executor_.execute (() -> onResolved.onResolved (value_, continuationExecutor));
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
              if (onRejected != null)
                onRejected.onRejected (rejection_);
              else
                continuation.bubbleRejection (rejection_);
            });
          }
        });
      }
    });

    return continuation;
  }

  void bubbleRejection (Throwable reason)
  {
    this.rejection_ = reason;

    // If the rejection was set here, then we can stop bubbling the rejection
    // at this promise. Otherwise, we need to continue to the next promise
    // in the chain.
    if (this.onRejected_ != null)
      this.onRejected_.onRejected (reason);
    else if (this.next_ != null)
      this.next_.bubbleRejection (reason);
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
      }
      else
      {
        // There are no promises to resolve. We can just issue our settlement.
        settlement.resolve (results);
      }
    });
  }
}
