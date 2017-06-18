package com.onehilltech.backbone;

import android.os.Looper;
import android.support.test.runner.AndroidJUnit4;

import com.onehilltech.backbone.app.Promise;
import com.onehilltech.backbone.app.RejectedOnUIThread;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class RejectedOnUIThreadTest
{
  private boolean complete_;

  private final Object lock_ = new Object ();

  @Before
  public void setup ()
  {
    this.complete_ = false;
  }

  @Test
  public void testRejectedOnUiThread () throws Exception
  {
    synchronized (this.lock_)
    {
      Promise.reject (new IllegalStateException ())
             ._catch (new RejectedOnUIThread (reason -> {
               boolean isUiThread = Looper.getMainLooper ().getThread ().equals (Thread.currentThread ());
               Assert.assertTrue (isUiThread);

               synchronized (this.lock_)
               {
                 this.complete_ = true;
                 this.lock_.notify ();
               }

               return null;
             }));

      this.lock_.wait (5000);

      Assert.assertTrue (this.complete_);
    }
  }
}
