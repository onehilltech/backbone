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

  void bubbleRejection (Throwable t)
  {
    // If the rejection was set here, then we can stop bubbling the rejection
    // at this promise. Otherwise, we need to continue to the next promise
    // in the chain.
    if (this.onRejected_ != null)
      this.onRejected_.onRejected (t);
    else if (this.next_ != null)
      this.next_.bubbleRejection (t);
  }
}
