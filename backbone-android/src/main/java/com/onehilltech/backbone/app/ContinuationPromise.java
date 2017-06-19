package com.onehilltech.backbone.app;

import android.support.annotation.NonNull;
import android.util.Log;

class ContinuationPromise <T> extends Promise <T>
{
  public ContinuationPromise ()
  {
    super (null);
  }

  @SuppressWarnings ("unchecked")
  public void settle (@NonNull Promise <T> promise)
  {
    Log.d ("ContinuationPromise", "Settling a continuation promise [thread=" + Thread.currentThread ().getId () + "]");

    // We need to execute this promise and pass the result to the executor
    // in this continuation executor. This allows us to chain the result.
    final OnResolved <T, ?> resolved = result -> {
      for (OnResolved <T, ?> onResolved: onResolved_)
      {
        Promise <?> p = onResolved.onResolved (result);

        if (p != null)
        {
          for (Continuation continuation : cont_)
            continuation.promise.settle (p);
        }
        else
        {
          for (Continuation continuation : cont_)
            continuation.promise.settle (Promise.resolve (null));
        }
      }

      return null;
    };

    final OnRejected rejected = reason -> {
      if (!this.onRejected_.isEmpty ())
      {
        for (OnRejected onRejected : this.onRejected_)
        {
          Promise <?> p = onRejected.onRejected (reason);

          if (p != null)
          {
            for (Continuation continuation : cont_)
              continuation.promise.settle (p);
          }
          else
          {
            for (Continuation continuation : cont_)
              continuation.promise.settle (Promise.resolve (null));
          }
        }
      }
      else
      {
        for (Continuation continuation : cont_)
          continuation.promise.processRejection (reason);
      }

      return null;
    };

    promise.then (resolved, rejected);
  }
}
