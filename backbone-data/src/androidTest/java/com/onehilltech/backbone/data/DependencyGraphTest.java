package com.onehilltech.backbone.data;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.onehilltech.backbone.data.fixtures.Book;
import com.onehilltech.backbone.data.fixtures.TestDatabase;
import com.onehilltech.backbone.data.fixtures.User;
import com.raizlabs.android.dbflow.config.DatabaseDefinition;
import com.raizlabs.android.dbflow.config.FlowManager;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith (AndroidJUnit4.class)
public class DependencyGraphTest
{
  private DependencyGraph dependencyGraph_;

  @Before
  public void setup () throws Exception
  {
    InstrumentationRegistry.getContext ().deleteDatabase (TestDatabase.NAME + ".db");
    FlowManager.init (InstrumentationRegistry.getContext ());

    DatabaseDefinition databaseDefinition = FlowManager.getDatabase (TestDatabase.class);
    this.dependencyGraph_ = new DependencyGraph.Builder (databaseDefinition).build ();
  }

  @Test
  public void testGetInsertOrder ()
  {
    List<DependencyGraph.Node> bookInsertOrder = this.dependencyGraph_.getInsertOrder (Book.class);
    Assert.assertEquals (2, bookInsertOrder.size ());
    Assert.assertSame (User.class, bookInsertOrder.get (0).getDataClass ());
    Assert.assertSame (Book.class, bookInsertOrder.get (1).getDataClass ());

    List<DependencyGraph.Node> userInsertOrder = this.dependencyGraph_.getInsertOrder (User.class);
    Assert.assertEquals (1, userInsertOrder.size ());
    Assert.assertSame (User.class, userInsertOrder.get (0).getDataClass ());
  }
}
