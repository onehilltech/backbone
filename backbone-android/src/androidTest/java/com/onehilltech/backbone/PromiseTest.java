package com.onehilltech.backbone;


import android.support.test.runner.AndroidJUnit4;

import com.onehilltech.backbone.app.Promise;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class PromiseTest
{
  private final Object lock_ = new Object ();
  private boolean isComplete_;

  @Before
  public void setup ()
  {
    this.isComplete_ = false;
  }

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
          this.isComplete_ = true;

          Assert.assertEquals (5, (int)value);
          lock_.notify ();
        }
      });

      this.lock_.wait (5000);

      Assert.assertTrue (this.isComplete_);

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
                this.isComplete_ = true;

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

      Assert.assertTrue (this.isComplete_);
    }
  }

  @Test
  public void testMultipleThen () throws Exception
  {
    Promise <Integer> p1 = new Promise<> ((settlement) -> {
      try
      {
        Thread.sleep (30);
        settlement.resolve (1);
      }
      catch (Exception e)
      {
        settlement.reject (e);
      }
    });

    Promise <Integer> p2 = new Promise<> ((settlement) -> {
      try
      {
        Thread.sleep (30);
        settlement.resolve (2);
      }
      catch (Exception e)
      {
        settlement.reject (e);
      }
    });

    Promise <Integer> p3 = new Promise<> ((settlement) -> {
      try
      {
        Thread.sleep (30);
        settlement.resolve (3);
      }
      catch (Exception e)
      {
        settlement.reject (e);
      }
    });

    Promise <Integer> p4 = new Promise<> ((settlement) -> {
      try
      {
        Thread.sleep (30);
        settlement.resolve (4);
      }
      catch (Exception e)
      {
        settlement.reject (e);
      }
    });

    final Promise.OnResolved <Integer, Integer> doP2 = (n, cont) ->
        cont.with (p2);

    final Promise.OnResolved <Integer, Integer> doP3 = (n, cont) ->
        cont.with (p3);

    final Promise.OnResolved <Integer, Integer> doP4 = (n, cont) ->
        cont.with (p4);

    synchronized (this.lock_)
    {
      p1.then (doP2)
        .then (doP3)
        .then (doP4)
        .then ((n, cont) -> {
          this.isComplete_ = true;

          synchronized (this.lock_)
          {
            this.lock_.notify ();
          }
        })
        ._catch (reason -> Assert.fail ());

      this.lock_.wait ();

      Assert.assertTrue (this.isComplete_);
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
          this.isComplete_ = true;
          lock_.notify ();
        }
      });

      this.lock_.wait (5000);

      Assert.assertTrue (this.isComplete_);
      Assert.assertFalse (p.isPending ());
    }
  }


  @Test
  public void testAllAsContinuation () throws Exception
  {
    synchronized (this.lock_)
    {
      // Create a LOT of promises.
      ArrayList <Promise <?>> promises = new ArrayList<> ();

      for (int i = 0; i < 7; ++ i)
      {
        Promise <Integer> p = new Promise<> ((settlement) -> {
          try
          {
            Thread.sleep (30);
            settlement.resolve (10);
          }
          catch (Exception e)
          {
            settlement.reject (e);
          }
        });

        promises.add (p);
      }

      Promise.OnResolved <Integer, List <Object>> doAll =
          (n, cont) -> cont.with (Promise.all (promises));

      Promise <Integer> start = new Promise<> (settlement -> {
        try
        {
          Thread.sleep (40);
          settlement.resolve (20);
        }
        catch (Exception e)
        {
          settlement.reject (e);
        }
      });

      Promise <Integer> middle = new Promise<> (settlement -> {
        try
        {
          Thread.sleep (40);
          settlement.resolve (20);
        }
        catch (Exception e)
        {
          settlement.reject (e);
        }
      });

      Promise.OnResolved <Integer, Integer> doMiddle = (n, cont) ->
          cont.with (middle);

      start.then (doMiddle)
           .then (doAll)
           .then ((result, cont) -> {
             this.isComplete_ = true;

             Assert.assertEquals (promises.size (), result.size ());

             for (int i = 0; i < result.size (); ++ i)
               Assert.assertEquals (10, result.get (i));

             synchronized (this.lock_)
             {
               this.lock_.notify ();
             }
           });

      this.lock_.wait (5000);

      Assert.assertTrue (this.isComplete_);
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
  public void testRejectOnly () throws Exception
  {
    Promise.reject (new IllegalStateException ())
           ._catch (reason -> {
             this.isComplete_ = true;
             Assert.assertEquals (reason.getClass (), IllegalStateException.class);

             synchronized (this.lock_)
             {
               this.lock_.notify ();
             }
           });

    synchronized (this.lock_)
    {
      this.lock_.wait (5000);
    }

    Assert.assertTrue (this.isComplete_);
  }

  @Test
  public void testPromiseChainStronglyTyped () throws Exception
  {
    synchronized (this.lock_)
    {
      Promise.OnResolved <String, Long> completion1 = (str, cont) -> {
        Assert.assertEquals ("Hello, World", str);

        cont.with (Promise.resolve (10L));
      };

      Promise.OnResolved <Long, Void> completion2 = (value, cont) -> {
        Assert.assertEquals (10L, (long)value);

        synchronized (this.lock_)
        {
          this.isComplete_ = true;
          this.lock_.notify ();
        }
      };

      Promise.resolve ("Hello, World")
             .then (completion1)
             .then (completion2);

      this.lock_.wait (5000);

      Assert.assertTrue (this.isComplete_);
    }
  }

  @Test
  public void testPromiseChain () throws Exception
  {
    synchronized (this.lock_)
    {
      Promise.resolve ("Hello, World")
             .then ((str, cont) -> {
               Assert.assertEquals ("Hello, World", str);

               cont.with (Promise.resolve (10));
             })
             .then ((n, cont) -> {
               Assert.assertEquals (10, n);

               synchronized (this.lock_)
               {
                 this.isComplete_ = true;
                 this.lock_.notify ();
               }
             });

      this.lock_.wait (5000);

      Assert.assertTrue (this.isComplete_);
    }
  }

  @Test
  public void testBubbleRejection () throws Exception
  {
    synchronized (this.lock_)
    {
      Promise.reject (new IllegalStateException ("GREAT"))
             .then ((value, cont) -> {
               Assert.fail ();
               cont.with (Promise.resolve (10));
             })
             .then ((value, cont) ->{
               Assert.fail ();
               cont.with (Promise.resolve (40));
             })
             .then ((value, cont) -> {},
                    reason -> {
                      Assert.assertEquals (IllegalStateException.class, reason.getClass ());
                      Assert.assertEquals ("GREAT", reason.getLocalizedMessage ());

                      synchronized (this.lock_)
                      {
                        this.isComplete_ = true;
                        this.lock_.notify ();
                      }
                    });

      this.lock_.wait (5000);

      Assert.assertTrue (this.isComplete_);
    }
  }

  @Test
  public void testCatch () throws Exception
  {
    synchronized (this.lock_)
    {
      Promise.reject (new IllegalStateException ("GREAT"))
             .then ((value, cont) -> {
               Assert.fail ();
               cont.with (Promise.resolve (10));
             })
             .then ((value, cont) ->{
               Assert.fail ();
               cont.with (Promise.resolve (40));
             })
             ._catch ((reason) -> {
               Assert.assertEquals (IllegalStateException.class, reason.getClass ());
               Assert.assertEquals ("GREAT", reason.getLocalizedMessage ());

               synchronized (this.lock_)
               {
                 this.isComplete_ = true;
                 this.lock_.notify ();
               }
             });

      this.lock_.wait (5000);

      Assert.assertTrue (this.isComplete_);
    }
  }

  @Test
  public void testRace () throws Exception
  {
    synchronized (this.lock_)
    {
      Promise<Integer> p =
          Promise.race (
              new Promise<> ((settlement) -> {
                try
                {
                  Thread.sleep (500);
                  settlement.resolve (10);
                }
                catch (InterruptedException e)
                {
                  settlement.reject (e);
                }
              }),
              new Promise<> ((settlement) -> {
                try
                {
                  Thread.sleep (300);
                  settlement.resolve (20);
                }
                catch (InterruptedException e)
                {
                  settlement.reject (e);
                }
              }),
              new Promise<> ((settlement) -> {
                try
                {
                  Thread.sleep (600);
                  settlement.resolve (30);
                }
                catch (InterruptedException e)
                {
                  settlement.reject (e);
                }
              })
          );

      p.then ((value, cont) -> {
        Assert.assertEquals (20, (int)value);

        this.isComplete_ = true;

        synchronized (lock_)
        {
          lock_.notify ();
        }
      })
      ._catch (reason -> Assert.fail ());

      this.lock_.wait (5000);

      Assert.assertTrue (this.isComplete_);
    }
  }

  @Test
  public void testAlways () throws Exception
  {
    synchronized (this.lock_)
    {
      new Promise<Integer> (settlement -> settlement.resolve (10))
          .always (() -> {
            this.isComplete_ = true;

            synchronized (this.lock_)
            {
              this.lock_.notify ();
            }
          });

      this.lock_.wait (5000);

      Assert.assertTrue (this.isComplete_);
    }
  }

  @Test
  public void testAlwaysFromResolve () throws Exception
  {
    synchronized (this.lock_)
    {
      Promise.resolve (10)
             .always (() -> {
               this.isComplete_ = true;

               synchronized (this.lock_)
               {
                 this.lock_.notify ();
               }
             });

      this.lock_.wait (5000);

      Assert.assertTrue (this.isComplete_);
    }
  }

  @Test
  public void testAlwaysFromReject () throws Exception
  {
    synchronized (this.lock_)
    {
      Promise.reject (new IllegalStateException ("GREAT"))
             .always (() -> {
               this.isComplete_ = true;

               synchronized (this.lock_)
               {
                 this.lock_.notify ();
               }
             });

      this.lock_.wait (5000);

      Assert.assertTrue (this.isComplete_);
    }
  }

  @Test
  public void testAlwaysDownstream () throws Exception
  {
    synchronized (this.lock_)
    {
      Promise.reject (new IllegalStateException ("GREAT"))
             .then ((value, cont) -> {
               Assert.fail ();
               cont.with (Promise.resolve (10));
             })
             .then ((value, cont) ->{
               Assert.fail ();
               cont.with (Promise.resolve (40));
             })
             .always (( ) -> {
               this.isComplete_ = true;

               synchronized (this.lock_)
               {
                 this.lock_.notify ();
               }
             });

      this.lock_.wait (5000);

      Assert.assertTrue (this.isComplete_);
    }
  }
}
