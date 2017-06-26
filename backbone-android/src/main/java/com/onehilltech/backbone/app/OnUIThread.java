/*
 * Copyright (c) 2017 One Hill Technologies, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
