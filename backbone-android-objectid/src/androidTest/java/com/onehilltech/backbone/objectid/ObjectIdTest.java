package com.onehilltech.backbone.objectid;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ObjectIdTest
{
  @Test
  public void testConstructor ()
  {
    ObjectIdGenerator generator = ObjectIdGenerator.getInstance ();
    int counter = generator.getCounter ();

    ObjectId objectId = generator.nextObjectId ();
    Assert.assertEquals (counter, objectId.getCounter ());
    Assert.assertEquals (generator.getMachinePart (), objectId.getMachinePart ());
    Assert.assertEquals (generator.getProcessPart (), objectId.getProcessPart ());
  }

  @Test
  public void testToString ()
  {
    ObjectId objectId = ObjectIdGenerator.getInstance ().nextObjectId ();
    String str = objectId.toString ();

    Assert.assertEquals (24, str.length ());
  }
}
