package com.onehilltech.backbone.app;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public abstract class OnUIThread
{
  protected abstract void run ();

  protected void runOnUiThread ()
  {
    Message message = uiHandler_.obtainMessage (0, this);
    message.sendToTarget ();
  }

  protected static final Handler uiHandler_ = new Handler (Looper.getMainLooper ()) {
    @Override
    public void handleMessage (Message msg)
    {
      if (msg.what == 0)
      {
        OnUIThread onUIThread = (OnUIThread) msg.obj;
        onUIThread.run ();
      }
    }
  };
}
