package com.onehilltech.backbone.os;

import android.os.Looper;
import android.os.Message;

/**
 * @class Handler
 *
 * Extension of the android.os.Handler class provided with the Android SDK that
 * allows the developer to register different Callback objects for each message type.
 */
public class Handler extends android.os.Handler
{
  /// Collection of Callback objects for this handler.
  private final HandlerCallbacks callbacks_ = new HandlerCallbacks ();

  /**
   * Default constructor.
   */
  public Handler ()
  {
  }

  /**
   * Initializing constructor.
   *
   * @param looper
   */
  public Handler (Looper looper)
  {
    super (looper);
  }

  @Override
  public void handleMessage (Message msg)
  {
    this.callbacks_.handleMessage (msg);
  }

  /**
   * Get the HandlerCallbacks object.
   *
   * @return
   */
  public HandlerCallbacks getCallbacks ()
  {
    return this.callbacks_;
  }
}
