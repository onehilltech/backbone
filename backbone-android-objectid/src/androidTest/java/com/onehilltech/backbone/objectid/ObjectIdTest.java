package com.onehilltech.backbone.objectid;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
public class ObjectIdTest
{
  @Test
  public void testToString ()
  {
    ObjectId objectId = new ObjectId ();
    String str = objectId.toString ();

    Assert.assertEquals (24, str.length ());
  }

  @Test
  public void testFromString ()
  {
    ObjectId expected = new ObjectId ();
    String objStr = expected.toString ();
    ObjectId actual = new ObjectId (objStr);

    Assert.assertEquals (expected, actual);
  }
}
