package com.onehilltech.backbone.data;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.onehilltech.backbone.data.fixtures.Book;
import com.onehilltech.backbone.data.fixtures.TestDatabase;
import com.onehilltech.backbone.data.fixtures.User;
import com.onehilltech.backbone.data.fixtures.User_Table;
import com.onehilltech.promises.Promise;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;

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
    FlowManager.init (InstrumentationRegistry.getContext ());

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
  public void testCreate () throws Exception
  {
    this.dispatcher_.add ("/users", new MockResponse ().setBody ("{\"user\": {\"_id\": 45, \"first_name\": \"John\", \"last_name\": \"Doe\"}}"));

    synchronized (this.lock_)
    {
      User user = new User ();
      user.firstName = "John";
      user.lastName = "Doe";

      this.dataStore_.create (User.class, user)
                     .then (newUser -> {
                       Assert.assertEquals (45, newUser._id);
                       Assert.assertEquals ("John", newUser.firstName);
                       Assert.assertEquals ("Doe", newUser.lastName);
                       Assert.assertSame (this.dataStore_, newUser.getStore ());

                       return this.dataStore_.peek (User.class, newUser._id);
                     })
                     .then (resolved (cachedUser -> {
                       Assert.assertEquals (45, cachedUser._id);
                       Assert.assertEquals ("John", cachedUser.firstName);
                       Assert.assertEquals ("Doe", cachedUser.lastName);
                       Assert.assertSame (this.dataStore_, cachedUser.getStore ());

                       synchronized (this.lock_)
                       {
                         this.lock_.notify ();
                       }
                     }))
                     ._catch (rejected (reason -> Assert.fail (reason.getLocalizedMessage ())));

      this.lock_.wait (5000);

      Assert.assertEquals (1, this.server_.getRequestCount ());

      RecordedRequest createRequest = this.server_.takeRequest ();
      Assert.assertEquals ("POST", createRequest.getMethod ());
      Assert.assertEquals ("/users", createRequest.getPath ());
    }
  }

  @Test
  public void testDelete () throws Exception
  {
    this.dispatcher_.add ("/users", new MockResponse ().setBody ("{\"user\": {\"_id\": 45, \"first_name\": \"John\", \"last_name\": \"Doe\"}}"));
    this.dispatcher_.add ("/users/45", new MockResponse ().setBody ("true"));

    synchronized (this.lock_)
    {
      User user = new User ();
      user.firstName = "John";
      user.lastName = "Doe";

      this.dataStore_.create (User.class, user)
                     .then (User::delete)
                     .then (result -> {
                       Assert.assertTrue (result);
                       return this.dataStore_.peek (User.class, 45);
                     })
                     .then (resolved (cachedUser -> {
                       Assert.assertNull (cachedUser);

                       synchronized (this.lock_)
                       {
                         this.lock_.notify ();
                       }
                     }))
                     ._catch (rejected (reason -> Assert.fail (reason.getLocalizedMessage ())));

      this.lock_.wait (5000);

      Assert.assertEquals (2, this.server_.getRequestCount ());

      // Ignore the first request.
      this.server_.takeRequest ();

      RecordedRequest createRequest = this.server_.takeRequest ();
      Assert.assertEquals ("DELETE", createRequest.getMethod ());
      Assert.assertEquals ("/users/45", createRequest.getPath ());
    }
  }

  @Test
  public void testUpdate () throws Exception
  {
    this.dispatcher_.add ("/users", new MockResponse ().setBody ("{\"user\": {\"_id\": 45, \"first_name\": \"John\", \"last_name\": \"Doe\"}}"));
    this.dispatcher_.add ("/users/45", new MockResponse ().setBody ("{\"user\": {\"_id\": 45, \"first_name\": \"Jane\", \"last_name\": \"Doe\"}}"));

    synchronized (this.lock_)
    {
      User user = new User ();
      user.firstName = "John";
      user.lastName = "Doe";

      this.dataStore_.create (User.class, user)
                     .then (newUser -> {
                       newUser.firstName = "Jane";

                       return newUser.update ();
                     })
                     .then (updatedUser -> {
                       Assert.assertEquals ("Jane", updatedUser.firstName);

                       return this.dataStore_.peek (User.class, 45);
                     })
                     .then (resolved (cachedUser -> {
                       Assert.assertEquals (45, cachedUser._id);
                       Assert.assertEquals ("Jane", cachedUser.firstName);
                       Assert.assertEquals ("Doe", cachedUser.lastName);

                       synchronized (this.lock_)
                       {
                         this.lock_.notify ();
                       }
                     }))
                     ._catch (rejected (reason -> Assert.fail (reason.getLocalizedMessage ())));

      this.lock_.wait (5000);

      Assert.assertEquals (2, this.server_.getRequestCount ());

      // Ignore the first request.
      this.server_.takeRequest ();

      RecordedRequest createRequest = this.server_.takeRequest ();
      Assert.assertEquals ("PUT", createRequest.getMethod ());
      Assert.assertEquals ("/users/45", createRequest.getPath ());
    }
  }

  @Test (expected = IllegalStateException.class)
  public void testDeleteNonExistentModel () throws Exception
  {
    User user = new User ();
    user.firstName = "John";
    user.lastName = "Doe";
    user.delete ();
  }

  @Test (expected = IllegalStateException.class)
  public void testUpdateNonExistentModel () throws Exception
  {
    User user = new User ();
    user.firstName = "John";
    user.lastName = "Doe";
    user.update ();
  }

  @Test
  public void testPush () throws Exception
  {
    synchronized (this.lock_)
    {
      User user = new User (56);
      user.firstName = "John";
      user.lastName = "Doe";

      this.dataStore_.push (User.class, user)
                     .then (newUser -> this.dataStore_.peek (User.class, newUser._id))
                     .then (resolved (cachedUser ->  {
                       Assert.assertEquals (user.firstName, cachedUser.firstName);
                       Assert.assertEquals (user.lastName, cachedUser.lastName);
                       Assert.assertEquals (user._id, cachedUser._id);

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
  public void testGetOne () throws Exception
  {
    this.dispatcher_.add ("/users/1", new MockResponse ().setBody ("{\"user\": {\"_id\": 1, \"first_name\": \"John\", \"last_name\": \"Doe\"}}"));

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
  public void testGetOneWithRelation () throws Exception
  {
    // We need this for the foreign relation.
    User user = new User (25);

    this.dispatcher_.add ("/books/1", new MockResponse ().setBody ("{\"book\": {\"_id\": 1, \"author\": 25, \"title\": \"Book Title\"}}"));

    synchronized (this.lock_)
    {
      this.dataStore_.push (User.class, user)
                     .then (newUser -> this.dataStore_.get (Book.class, 1))
                     .then (resolved (book -> {
                       Assert.assertNotNull (book);

                       Assert.assertEquals (1, book._id);
                       Assert.assertEquals (25, book.author._id);
                       Assert.assertEquals ("Book Title", book.title);

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
      this.dataStore_.get (User.class, 1)
                     .then (resolved (user -> {
                       this.dataStore_.get (User.class, 1)
                                      .then (resolved (cached -> {
                                        Assert.assertEquals (user, cached);

                                        this.isComplete_ = true;

                                        synchronized (this.lock_)
                                        {
                                          this.lock_.notify ();
                                        }
                                      }));
                     }))
                     ._catch (rejected (reason -> Assert.fail (reason.getLocalizedMessage ())));

      this.lock_.wait ();

      Assert.assertTrue (this.isComplete_);
    }
  }

  @Test
  public void testPeekOne () throws Exception
  {
    synchronized (this.lock_)
    {
      User user = new User (25);
      user.firstName = "Jane";
      user.lastName = "Doe";

      this.dataStore_.push (User.class, user)
                     .then (newUser -> this.dataStore_.peek (User.class, newUser._id))
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
    User user = new User (54);
    user.firstName = "John";
    user.lastName = "Doe";

    synchronized (this.lock_)
    {
      this.dataStore_.push (User.class, user)
                     .then (newUser -> this.dataStore_.peek (User.class))
                     .then (resolved (actual -> {
                       Assert.assertEquals (1, actual.size ());

                       User cachedUser = actual.get (0);
                       Assert.assertEquals (user, cachedUser);

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
  public void testSelect ()
      throws Exception
  {
    User user1 = new User (54, "John", "Doe");
    User user2 = new User (57, "Jane", "Doe");

    synchronized (this.lock_)
    {
      Promise.all (
          this.dataStore_.push (User.class, user1),
          this.dataStore_.push (User.class, user2))
             .then (result -> {
               HashMap <String, Object> selector = new HashMap< > ();
               selector.put (User_Table.first_name.getDefinition (), "Jane");

               return this.dataStore_.select (User.class, selector);
             })
             .then (resolved (users -> {
               Assert.assertEquals (1, users.size ());
               Assert.assertEquals (user2, users.get (0));

               synchronized (this.lock_)
               {
                 this.lock_.notify ();
               }
             }))
             ._catch (rejected (reason -> Assert.fail (reason.getLocalizedMessage ())));

      this.lock_.wait ();
    }
  }
}
