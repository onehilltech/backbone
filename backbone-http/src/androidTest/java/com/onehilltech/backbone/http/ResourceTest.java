package com.onehilltech.backbone.http;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RunWith (AndroidJUnit4.class)
public class ResourceTest
{
  @Test
  public void testConstructor ()
  {
    Resource r = new Resource ();
    Assert.assertEquals (0, r.entityCount ());

    r = new Resource ("count", 45);
    Assert.assertEquals (1, r.entityCount ());
    Assert.assertEquals (45, r.get ("count"));

    Map <String, Object> entities = new HashMap<> ();
    entities.put ("count", 35);
    r = new Resource (entities);
    Assert.assertEquals (1, r.entityCount ());
  }

  @Test
  public void testEntitySet ()
  {
    Map <String, Object> entities = new HashMap<> ();
    entities.put ("count", 35);
    entities.put ("timestamp", new Date ());

    Resource r = new Resource (entities);
    Assert.assertEquals (entities.entrySet (), r.entitySet ());
  }
}
