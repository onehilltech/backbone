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

  @Before
  public void setup () throws Exception
  {
    this.isComplete_ = false;

    FlowManager.init (
        new FlowConfig.Builder (InstrumentationRegistry.getContext ())
            .openDatabasesOnInit (true)
            .build ());

    this.server_ = new MockWebServer ();
    this.server_.start ();

    this.dataStore_ =
        new DataStore.Builder (InstrumentationRegistry.getContext ())
            .setBaseUrl (this.server_.getUrl ("/").toString ())
            .setDatabaseClass (TestDatabase.class)
            .build ();
  }

  @Test
  public void testGet () throws Exception
  {
    // Schedule some responses.
    DateTime today = DateTime.now ();
    this.server_.enqueue (new MockResponse ().setBody ("{\"user\": {\"_id\": 1, \"first_name\": \"John\", \"last_name\": \"Doe\", \"birthday\": \"" + today.toDateTimeISO () + "\"}}"));

    synchronized (this.lock_)
    {
      this.dataStore_.get (User.class, "me")
                     .then (resolved (user -> {
                       Assert.assertNotNull (user);

                       Assert.assertEquals (1, user._id);
                       Assert.assertEquals ("John", user.firstName);
                       Assert.assertEquals ("Doe", user.lastName);
                       Assert.assertTrue (today.isEqual (user.birthday));

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
  public void testQuery () throws Exception
  {
    // Schedule some responses.
    DateTime today = DateTime.now ();
    this.server_.enqueue (new MockResponse ().setBody ("{\"users\": [{\"_id\": 1, \"first_name\": \"John\", \"last_name\": \"Doe\", \"birthday\": \"" + today.toDateTimeISO () + "\"}]}"));

    synchronized (this.lock_)
    {
      this.dataStore_.query (User.class)
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
    // Schedule some responses.
    DateTime today = DateTime.now ();
    this.server_.enqueue (new MockResponse ().setBody ("{\"user\": {\"_id\": 1, \"first_name\": \"John\", \"last_name\": \"Doe\", \"birthday\": \"" + today.toDateTimeISO () + "\"}}"));
    this.server_.enqueue (new MockResponse ().setResponseCode (304).setBody ("Not Modified"));

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
  public void testPeek () throws Exception
  {
    synchronized (this.lock_)
    {
      User user = new User ();
      user.firstName = "Jane";
      user.lastName = "Doe";
      user.birthday = DateTime.now ();
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
}
