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
    int counter = generator.getNextCounter () & 0x00ffffff;
    int machinePart = generator.getMachinePart () & 0x00ffffff;

    ObjectId objectId = generator.nextObjectId ();
    Assert.assertEquals (counter, objectId.getCounter ());
    Assert.assertEquals (machinePart, objectId.getMachinePart ());
    Assert.assertEquals (generator.getProcessPart (), objectId.getProcessPart ());
  }

  @Test
  public void testToString ()
  {
    ObjectId objectId = ObjectIdGenerator.getInstance ().nextObjectId ();
    String str = objectId.toString ();

    Assert.assertEquals (24, str.length ());
  }

  @Test
  public void testFromString ()
  {
    ObjectId expected = ObjectIdGenerator.getInstance ().nextObjectId ();
    String objStr = expected.toString ();
    ObjectId actual = new ObjectId (objStr);

    Assert.assertEquals (expected, actual);
  }
}
