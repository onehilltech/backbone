package com.onehilltech.backbone;


import android.support.test.runner.AndroidJUnit4;

import com.onehilltech.backbone.app.Promise;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class PromiseTest
{
  private final Object lock_ = new Object ();

  @Test
  public void testThenResolve () throws Exception
  {
    synchronized (this.lock_)
    {
      Promise <Integer> p = new Promise <> ((completion) -> completion.resolve (5));
      Assert.assertTrue (p.isPending ());
      Assert.assertFalse (p.isRejected ());
      Assert.assertFalse (p.isResolved ());

      p.then ((value, cont) -> {
        synchronized (lock_)
        {
          Assert.assertEquals (5, (int)value);
          lock_.notify ();
        }
      });

      this.lock_.wait (5000);

      Assert.assertFalse (p.isPending ());
      Assert.assertTrue (p.isResolved ());
      Assert.assertFalse (p.isRejected ());
    }
  }

  @Test
  public void testThenReject () throws Exception
  {
    synchronized (this.lock_)
    {
      Promise <Integer> p = new Promise <> ((completion) -> completion.reject (new IllegalStateException ()));

      p.then ((value, cont) -> Promise.resolve (5),
              (reason) -> {
                synchronized (lock_)
                {
                  Assert.assertEquals (IllegalStateException.class, reason.getClass ());
                  lock_.notify ();
                }
              });

      this.lock_.wait (5000);

      Assert.assertFalse (p.isPending ());
      Assert.assertFalse (p.isResolved ());
      Assert.assertTrue (p.isRejected ());
    }
  }

  @Test
  public void testAll () throws Exception
  {
    synchronized (this.lock_)
    {
      Promise<List<Object>> p =
          Promise.all (
              new Promise<Integer> ((settlement) -> settlement.resolve (10)),
              new Promise<Integer> ((settlement) -> settlement.resolve (20)));

      p.then ((value, cont) -> {
        Assert.assertEquals (2, value.size ());
        Assert.assertEquals (10, value.get (0));
        Assert.assertEquals (20, value.get (1));

        synchronized (lock_)
        {
          lock_.notify ();
        }
      });

      this.lock_.wait (5000);

      Assert.assertFalse (p.isPending ());
    }
  }

  @Test
  public void testStaticResolve ()
  {
    Promise <Integer> p = Promise.resolve (7);

    Assert.assertTrue (p.isResolved ());
    Assert.assertFalse (p.isRejected ());
    Assert.assertFalse (p.isPending ());
  }

  @Test
  public void testStaticReject ()
  {
    Promise <?> p = Promise.reject (new IllegalStateException ());

    Assert.assertTrue (p.isRejected ());
    Assert.assertFalse (p.isResolved ());
    Assert.assertFalse (p.isPending ());
  }

  @Test
  public void testPromiseContinuation () throws Exception
  {
    synchronized (this.lock_)
    {
      Promise <?> p = Promise.resolve ("Hello, World");

      p.then ((str, cont) -> {
        Assert.assertEquals ("Hello, World", str);

        cont.with (Promise.resolve (10));
      }).then ((n, cont) -> {
        Assert.assertEquals (10, n);

        synchronized (this.lock_)
        {
          this.lock_.notify ();
        }
      });

      this.lock_.wait (5000);

      Assert.assertTrue (p.isResolved ());
    }
  }
}
