package com.onehilltech.backbone.app;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;

public class ResolvedOnUiThread <T, U> implements Promise.OnResolved <T, U>
{
  public interface OnResolved <T>
  {
    void onResolved (T value);
  }

  private final Promise.OnResolved <T, U> onResolved_;

  private T value_;

  private ContinuationExecutor<U> cont_;

  public ResolvedOnUiThread (@NonNull Promise.OnResolved <T, U> onResolved)
  {
    this.onResolved_ = onResolved;
  }

  @Override
  public void onResolved (T value, ContinuationExecutor<U> cont)
  {
    this.value_ = value;
    this.cont_ = cont;

    Message msg = uiHandler_.obtainMessage (0, this);
    msg.sendToTarget ();
  }

  @SuppressWarnings ("unchecked")
  private static final Handler uiHandler_ = new Handler (Looper.getMainLooper ()) {
    @Override
    public void handleMessage (Message msg)
    {
      if (msg.what == 0)
      {
        ResolvedOnUiThread resolvedOnUiThread = (ResolvedOnUiThread) msg.obj;
        resolvedOnUiThread.onResolved_.onResolved (resolvedOnUiThread.value_, resolvedOnUiThread.cont_);
      }
    }
  };
}
