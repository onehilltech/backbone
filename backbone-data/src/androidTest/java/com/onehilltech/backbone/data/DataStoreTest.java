package com.onehilltech.backbone.data;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.onehilltech.backbone.data.fixtures.TestDatabase;
import com.onehilltech.backbone.data.fixtures.User;
import com.onehilltech.promises.Promise;
import com.raizlabs.android.dbflow.config.FlowConfig;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.onehilltech.promises.Promise.rejected;
import static com.onehilltech.promises.Promise.resolved;

@RunWith (AndroidJUnit4.class)
public class DataStoreTest
{
  private DataStore dataStore_;

  private final Object lock_ = new Object ();

  private MockWebServer server_;

  private boolean isComplete_;

  private SimpleDispatcher dispatcher_;

  @Before
  public void setup () throws Exception
  {
    this.isComplete_ = false;

    InstrumentationRegistry.getContext ().deleteDatabase (TestDatabase.NAME + ".db");

    FlowManager.init (
        new FlowConfig.Builder (InstrumentationRegistry.getContext ())
            .openDatabasesOnInit (true)
            .build ());

    this.dispatcher_ = new SimpleDispatcher ();

    this.server_ = new MockWebServer ();
    this.server_.setDispatcher (this.dispatcher_);
    this.server_.start ();

    this.dataStore_ =
        new DataStore.Builder (TestDatabase.class)
            .setBaseUrl (this.server_.getUrl ("/").toString ())
            .build ();
  }

  @After
  public void teardown ()
  {
    FlowManager.reset ();
  }

  @Test
  public void testGetOne () throws Exception
  {
    // Schedule some responses.
    DateTime today = DateTime.now ();

    this.dispatcher_.add ("/users/1", new MockResponse ().setBody ("{\"user\": {\"_id\": 1, \"first_name\": \"John\", \"last_name\": \"Doe\", \"birthday\": \"" + today.toDateTimeISO () + "\"}}"));

    synchronized (this.lock_)
    {
      this.dataStore_.get (User.class, 1)
                     .then (resolved (user -> {
                       Assert.assertNotNull (user);

                       Assert.assertEquals (1, user._id);
                       Assert.assertEquals ("John", user.firstName);
                       Assert.assertEquals ("Doe", user.lastName);

                       synchronized (this.lock_)
                       {
                         this.lock_.notify ();
                       }
                     }))
                     ._catch (rejected (reason -> Assert.fail (reason.getLocalizedMessage ())));

      this.lock_.wait (5000);
    }
  }

  @Test
  public void testGetAll () throws Exception
  {
    this.dispatcher_.add ("/users", new MockResponse ().setBody ("{\"users\": [{\"_id\": 1, \"first_name\": \"John\", \"last_name\": \"Doe\"}]}"));

    synchronized (this.lock_)
    {
      this.dataStore_.get (User.class)
                     .then (resolved (users -> {
                       Assert.assertEquals (1, users.size ());

                       synchronized (this.lock_)
                       {
                         this.lock_.notify ();
                       }
                     }))
                     ._catch (rejected (reason -> Assert.fail (reason.getLocalizedMessage ())));

      this.lock_.wait ();
    }
  }

  @Test
  public void testGetNotModified () throws Exception
  {
    this.dispatcher_.add ("/users/1", new MockResponse ().setBody ("{\"user\": {\"_id\": 1, \"first_name\": \"John\", \"last_name\": \"Doe\"}}"));
    this.dispatcher_.add ("/users/1", new MockResponse ().setResponseCode (304).setBody ("Not Modified"));

    synchronized (this.lock_)
    {
      Promise.all (
          this.dataStore_.get (User.class, 1),
          this.dataStore_.get (User.class, 1)
      ).then (resolved (result -> {
        Assert.assertEquals (result.get (0), result.get (1));

        this.isComplete_ = true;

        synchronized (this.lock_)
        {
          this.lock_.notify ();
        }
      }))._catch (rejected (reason -> Assert.fail (reason.getLocalizedMessage ())));

      this.lock_.wait ();

      Assert.assertTrue (this.isComplete_);
    }
  }

  @Test
  public void testPeekOne () throws Exception
  {
    synchronized (this.lock_)
    {
      User user = new User ();
      user.firstName = "Jane";
      user.lastName = "Doe";
      user.save ();

      this.dataStore_.peek (User.class, user._id)
                     .then (resolved (actual -> {
                       Assert.assertEquals (user, actual);

                       synchronized (this.lock_)
                       {
                         this.lock_.notify ();
                       }

                     }))
                     ._catch (rejected (reason -> Assert.fail (reason.getLocalizedMessage ())));

      this.lock_.wait (5000);
    }
  }

  @Test
  public void testPeekAll () throws Exception
  {
    synchronized (this.lock_)
    {
      User user = new User ();
      user.firstName = "Jane";
      user.lastName = "Doe";
      user.save ();

      this.dataStore_.peek (User.class)
                     .then (resolved (actual -> {
                       Assert.assertEquals (1, actual.size ());
                       Assert.assertEquals (user, actual.get (0));

                       synchronized (this.lock_)
                       {
                         this.lock_.notify ();
                       }

                     }))
                     ._catch (rejected (reason -> Assert.fail (reason.getLocalizedMessage ())));

      this.lock_.wait (5000);
    }
  }
}
