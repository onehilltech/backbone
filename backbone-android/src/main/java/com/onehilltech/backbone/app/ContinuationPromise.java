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
    // Instead of using ? in the <nextExecutor_> generic to prevent compilation
    // errors, we are removing the generic type from this variable. We then are
    // going to suppress the warnings.

    // We need to execute this promise and pass the result to the executor
    // in this continuation executor. This allows us to chain the result.
    promise.then ((result, cont) -> onResolved_.onResolved (result, nextExecutor_),
                  this.onRejected_);
  }
}
