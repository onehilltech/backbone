package com.onehilltech.backbone.app;

class ContinuationPromise <T> extends Promise <T>
{
  public ContinuationPromise ()
  {
    super (null);
  }

  public void evaluate (Promise <T> promise)
  {
    promise.then (this.onResolved_, this.onRejected_);
  }
}
