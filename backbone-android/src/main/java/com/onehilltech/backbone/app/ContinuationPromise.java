package com.onehilltech.backbone.app;

class ContinuationPromise <T> extends Promise <T>
{
  public ContinuationPromise ()
  {
    super (null);
  }

  @SuppressWarnings ("unchecked")
  public void evaluate (Promise <T> promise)
  {
    // We need to execute this promise and pass the result to the executor
    // in this continuation executor. This allows us to chain the result.
    promise.then (
        (result, cont) -> {
          for (OnResolved <T, ?> onResolved: onResolved_)
          {
            onResolved.onResolved (result, (p) -> {
              for (Continuation continuation : cont_)
                continuation.promise.evaluate (p);
            });
          }
        },
        reason -> {
          for (OnRejected onRejected: this.onRejected_)
            onRejected.onRejected (reason);
        });
  }
}
