package com.onehilltech.backbone.http;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.raizlabs.android.dbflow.config.FlowConfig;
import com.raizlabs.android.dbflow.config.FlowManager;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;

@RunWith (AndroidJUnit4.class)
public class ResourceCacheTest
{
  private URL testURL_;

  @Before
  public void setup () throws Exception
  {
    Context targetContext = InstrumentationRegistry.getTargetContext ();

    FlowManager.init (new FlowConfig.Builder (targetContext).build ());
    BackboneHttp.initialize ();

    // Do a hard reset of the database.
    FlowManager.getDatabase (BackboneHttpDatabase.class).reset (targetContext);

    this.testURL_ = new URL ("http://www.google.com");
  }

  @Test
  public void testAdd () throws Exception
  {
    DateTime lastModified = DateTime.now ();
    String eTag = "myTag";

    // Should perform an insert.
    ResourceCache.getInstance ().add (this.testURL_, eTag, lastModified);
    ResourceCacheModel model = ResourceCache.getInstance ().get (this.testURL_);

    Assert.assertEquals (this.testURL_, model.url);
    Assert.assertEquals (eTag, model.ETag);
    Assert.assertEquals (lastModified, model.lastModified);

    // Should perform an update.
    lastModified.withYear (lastModified.getYear () + 1);
    ResourceCache.getInstance ().add (this.testURL_, eTag, lastModified);

    model = ResourceCache.getInstance ().get (this.testURL_);

    Assert.assertEquals (this.testURL_, model.url);
    Assert.assertEquals (eTag, model.ETag);
    Assert.assertEquals (lastModified, model.lastModified);
  }

  @Test
  public void testRemove ()
  {
    ResourceCache.getInstance ().remove (this.testURL_);
    Assert.assertNull (ResourceCache.getInstance ().get (this.testURL_));
  }

  @Test
  public void testClear ()
  {
    DateTime lastModified = DateTime.now ();
    String eTag = "myTag";

    ResourceCache.getInstance ().add (this.testURL_, eTag, lastModified);
    ResourceCache.getInstance ().clear ();

    Assert.assertNull (ResourceCache.getInstance ().get (this.testURL_));
  }

  public void testHasBeenModified ()
  {
    DateTime pastDate = DateTime.now ().minusDays (5);
    DateTime futureDate = DateTime.now ().plusDays (5);

    // Nothing is present, should return true.
    Assert.assertTrue (ResourceCache.getInstance ().hasBeenModified (this.testURL_, pastDate));

    // Add entry to force comparison.
    ResourceCache.getInstance ().add (this.testURL_, null, DateTime.now ());
    Assert.assertTrue (ResourceCache.getInstance ().hasBeenModified (this.testURL_, pastDate));
    Assert.assertFalse (ResourceCache.getInstance ().hasBeenModified (this.testURL_, futureDate));
  }
}
