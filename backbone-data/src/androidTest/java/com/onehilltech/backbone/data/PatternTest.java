package com.onehilltech.backbone.data;

import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PatternTest
{
  @Test
  public void testSingular ()
  {
    Assert.assertEquals ("favorite", Pluralize.getInstance ().singular ("favorites"));
  }
}
