package com.onehilltech.backbone;

import android.os.Looper;
import android.support.test.runner.AndroidJUnit4;

import com.onehilltech.backbone.app.Promise;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.onehilltech.backbone.app.ResolvedOnUIThread.resolveOnUiThread;
import static com.onehilltech.backbone.app.Promise.resolved;

@RunWith(AndroidJUnit4.class)
public class ResolvedOnUIThreadTest
{
  private boolean complete_;
  private final Object lock_ = new Object ();

  @Before
  public void setup ()
  {
    this.complete_ = false;
  }

  @Test
  public void testResolvedOnUiThread () throws Exception
  {
    synchronized (this.lock_)
    {
      Promise.resolve (10)
             .then (resolveOnUiThread (resolved (value -> {
               boolean isUiThread = Looper.getMainLooper ().getThread ().equals (Thread.currentThread ());
               Assert.assertTrue (isUiThread);

               synchronized (this.lock_)
               {
                 this.complete_ = true;
                 this.lock_.notify ();
               }
             })));

      this.lock_.wait (5000);

      Assert.assertTrue (this.complete_);
    }
  }
}
