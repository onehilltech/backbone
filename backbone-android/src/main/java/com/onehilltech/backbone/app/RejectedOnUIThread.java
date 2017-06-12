package com.onehilltech.backbone.app;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;

public class RejectedOnUIThread implements Promise.OnRejected
{
  public interface OnRejected
  {
    void onRejected (Throwable reason);
  }

  private final OnRejected onRejected_;

  private Throwable reason_;

  public RejectedOnUIThread (@NonNull OnRejected onRejected)
  {
    this.onRejected_ = onRejected;
  }

  @Override
  public void onRejected (Throwable reason)
  {
    // Cache the reason, and resume this call on the UI thread.
    this.reason_ = reason;

    Message msg = uiHandler_.obtainMessage (0, this);
    msg.sendToTarget ();
  }

  private static final Handler uiHandler_ = new Handler (Looper.getMainLooper ()) {
    @Override
    public void handleMessage (Message msg)
    {
      if (msg.what == 0)
      {
        RejectedOnUIThread uiOnRejected = (RejectedOnUIThread) msg.obj;
        uiOnRejected.onRejected_.onRejected (uiOnRejected.reason_);
      }
    }
  };
}
