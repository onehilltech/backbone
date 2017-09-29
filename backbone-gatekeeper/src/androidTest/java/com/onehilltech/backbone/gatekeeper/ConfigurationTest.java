package com.onehilltech.backbone.gatekeeper;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.onehilltech.backbone.gatekeeper.test.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith (AndroidJUnit4.class)
public class ConfigurationTest
{
  @Test
  public void testLoadFromMetadata () throws Exception
  {
    Context targetContext = InstrumentationRegistry.getTargetContext ();
    GatekeeperClient.Configuration config = GatekeeperClient.Configuration.loadFromMetadata (targetContext);

    Assert.assertEquals (targetContext.getString (R.string.gatekeeper_baseuri), config.baseUri);
    Assert.assertEquals (targetContext.getString (R.string.gatekeeper_client_id), config.clientId);
    Assert.assertEquals (targetContext.getString (R.string.gatekeeper_client_secret), config.clientSecret);
  }
}
