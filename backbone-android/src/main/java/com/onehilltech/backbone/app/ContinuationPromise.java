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

import android.support.annotation.Nullable;

class ContinuationPromise <T> extends Promise <T>
{
  public ContinuationPromise ()
  {
    super (null);
  }

  @SuppressWarnings ("unchecked")
  public void continueWith (@Nullable Promise <T> promise)
  {
    if (promise != null)
      promise.then (resolved (this::onResolve), rejected (this::onReject));
    else
      this.onResolve (null);
  }

  public void continueWithNull ()
  {
    this.onResolve (null);
  }

  public void continueWith (Throwable t)
  {
    this.onReject (t);
  }
}
