package com.onehilltech.backbone;


import android.support.test.runner.AndroidJUnit4;

import com.onehilltech.backbone.app.Promise;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PromiseTest
{
  private final Object lock_ = new Object ();

  @Test
  public void testThenResolve ()
      throws Exception
  {
    synchronized (this.lock_)
    {
      Promise <Integer> p = new Promise <> ((completion) -> completion.resolve (5));

      p.then ((value) -> {
        synchronized (lock_)
        {
          Assert.assertEquals (5, (int)value);
          lock_.notify ();
        }
      });

      this.lock_.wait (5000);
    }
  }

  @Test
  public void testThenReject ()
      throws Exception
  {
    synchronized (this.lock_)
    {
      Promise <Integer> p = new Promise <> ((completion) -> completion.reject (new IllegalStateException ()));

      p.then ((value) -> { }, (reason) -> {
        synchronized (lock_)
        {
          Assert.assertEquals (IllegalStateException.class, reason.getClass ());
          lock_.notify ();
        }
      });

      this.lock_.wait (5000);
    }
  }
}
