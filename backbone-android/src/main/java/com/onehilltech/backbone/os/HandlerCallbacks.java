package com.onehilltech.backbone.os;

import android.os.Handler;
import android.os.Message;
import android.util.SparseArray;

/**
 * @class HandlerCallbacks
 *
 * Collection of Callback objects for dispatching Handler messages. The HandlerCallbacks
 * lets the client to register a single Callback object for each message type. The client
 * therefore does not have to use if-else, or switch-case, statements to process events.
 *
 * The HandlerCallbacks class allows the developer to separate the concerns of each
 * message type into its own object.
 */
public class HandlerCallbacks implements Handler.Callback
{
  private final SparseArray<Handler.Callback> callbacks_ = new SparseArray<> ();

  @Override
  public boolean handleMessage (Message message)
  {
    Handler.Callback callback = this.callbacks_.get (message.what);
    return callback != null && callback.handleMessage (message);
  }

  public void put (int what, Handler.Callback callback)
  {
    this.callbacks_.put (what, callback);
  }

  public Handler.Callback get (int what)
  {
    return this.callbacks_.get (what);
  }

  public int size ()
  {
    return this.callbacks_.size ();
  }
}
