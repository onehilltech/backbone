package com.onehilltech.backbone.data;

import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PluralizeTest
{
  @Test
  public void testSingular ()
  {
    Assert.assertEquals ("bunny", Pluralize.getInstance ().singular ("bunnies"));

    Assert.assertEquals ("person", Pluralize.getInstance ().singular ("person"));
    Assert.assertEquals ("person", Pluralize.getInstance ().singular ("people"));

    Assert.assertEquals ("child", Pluralize.getInstance ().singular ("children"));

    Assert.assertEquals ("man", Pluralize.getInstance ().singular ("men"));
  }
}
