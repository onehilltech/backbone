package com.onehilltech.backbone.http.retrofit;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.onehilltech.backbone.http.BackboneHttp;
import com.onehilltech.backbone.http.BackboneHttpDatabase;
import com.onehilltech.backbone.http.Resource;
import com.raizlabs.android.dbflow.config.FlowConfig;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class ResourceEndpointTest
{
  private MockWebServer mockWebServer_;

  private static final String ACTION_RESOURCE_PATH = "actions";
  private static final String ACTION_RESOURCE_NAME = "action";

  private ResourceEndpoint <String> endpoint_;

  private final SimpleResourceConverterFactory converter_ = new SimpleResourceConverterFactory ();

  @Before
  public void setup () throws Exception
  {
    Context targetContext = InstrumentationRegistry.getTargetContext ();

    FlowManager.init (new FlowConfig.Builder (targetContext).build ());
    BackboneHttp.initialize ();

    // Do a hard reset of the database.
    FlowManager.getDatabase (BackboneHttpDatabase.class).reset (targetContext);

    this.mockWebServer_ = new MockWebServer ();
    this.mockWebServer_.play ();

    OkHttpClient client =
        new OkHttpClient.Builder ()
            .addInterceptor (new ResourceCacheInterceptor ())
            .build ();

    Retrofit retrofit =
        new Retrofit.Builder ()
            .baseUrl (this.mockWebServer_.getUrl ("/").toString ())
            .addConverterFactory (new SimpleResourceConverterFactory ())
            .addConverterFactory (ScalarsConverterFactory.create ())
            .client (client)
            .build ();

    this.endpoint_ = ResourceEndpoint.create (retrofit, ACTION_RESOURCE_NAME, ACTION_RESOURCE_PATH);
  }

  @After
  public void teardown () throws Exception
  {
    if (this.mockWebServer_ != null)
      this.mockWebServer_.shutdown ();
  }

  @Test
  public void testCreate () throws Exception
  {
    this.mockWebServer_.enqueue (new MockResponse ().setBody ("action=created"));

    Response<Resource> response = this.endpoint_.create ("create").execute ();
    RecordedRequest request = this.mockWebServer_.takeRequest ();

    // Make sure the request is correct.
    Assert.assertEquals ("POST", request.getMethod ());
    Assert.assertEquals ("/actions", request.getPath ());
    Assert.assertEquals ("action=create", request.getUtf8Body ());

    Resource r = response.body ();
    Assert.assertEquals (1, r.entityCount ());
    Assert.assertEquals ("created", r.get ("action"));
  }

  @Test
  public void testGetOne () throws Exception
  {
    this.mockWebServer_.enqueue (new MockResponse ().setBody ("action=getOne"));

    Response<Resource> response = this.endpoint_.get ("12").execute ();
    RecordedRequest request = this.mockWebServer_.takeRequest ();

    // Make sure the request is correct.
    Assert.assertEquals ("GET", request.getMethod ());
    Assert.assertEquals ("/actions/12", request.getPath ());

    Resource r = response.body ();
    Assert.assertEquals (1, r.entityCount ());
    Assert.assertEquals ("getOne", r.get ("action"));
  }

  @Test
  public void testGetAll () throws Exception
  {
    this.mockWebServer_.enqueue (new MockResponse ().setBody ("action=getAll"));

    Response<Resource> response = this.endpoint_.get ().execute ();
    RecordedRequest request = this.mockWebServer_.takeRequest ();

    // Make sure the request is correct.
    Assert.assertEquals ("GET", request.getMethod ());
    Assert.assertEquals ("/actions", request.getPath ());

    Resource r = response.body ();
    Assert.assertEquals (1, r.entityCount ());
    Assert.assertEquals ("getAll", r.get ("action"));
  }

  @Test
  public void testGetAllWithQuery () throws Exception
  {
    this.mockWebServer_.enqueue (new MockResponse ().setBody ("action=getAllWithQuery"));

    Map <String, Object> params = new HashMap<> ();
    params.put ("param1", "value1");

    Response<Resource> response = this.endpoint_.get (params).execute ();
    RecordedRequest request = this.mockWebServer_.takeRequest ();

    // Make sure the request is correct.
    Assert.assertEquals ("GET", request.getMethod ());
    Assert.assertEquals ("/actions?param1=value1", request.getPath ());

    Resource r = response.body ();
    Assert.assertEquals (1, r.entityCount ());
    Assert.assertEquals ("getAllWithQuery", r.get ("action"));
  }

  @Test
  public void testUpdate () throws Exception
  {
    this.mockWebServer_.enqueue (new MockResponse ().setBody ("action=updated"));

    Response<Resource> response = this.endpoint_.update ("42", "update").execute ();
    RecordedRequest request = this.mockWebServer_.takeRequest ();

    // Make sure the request is correct.
    Assert.assertEquals ("PUT", request.getMethod ());
    Assert.assertEquals ("/actions/42", request.getPath ());
    Assert.assertEquals ("action=update", request.getUtf8Body ());

    Resource r = response.body ();
    Assert.assertEquals (1, r.entityCount ());
    Assert.assertEquals ("updated", r.get ("action"));
  }

  @Test
  public void testDelete () throws Exception
  {
    this.mockWebServer_.enqueue (new MockResponse ().setBody ("true"));

    Response<Boolean> response = this.endpoint_.delete ("34").execute ();
    RecordedRequest request = this.mockWebServer_.takeRequest ();

    // Make sure the request is correct.
    Assert.assertEquals ("DELETE", request.getMethod ());
    Assert.assertEquals ("/actions/34", request.getPath ());
    Assert.assertEquals (true, response.body ());
  }

  @Test
  public void testCount () throws Exception
  {
    this.mockWebServer_.enqueue (new MockResponse ().setBody ("action=counted"));

    Response<Resource> response = this.endpoint_.count ().execute ();
    RecordedRequest request = this.mockWebServer_.takeRequest ();

    // Make sure the request is correct.
    Assert.assertEquals ("GET", request.getMethod ());
    Assert.assertEquals ("/actions/count", request.getPath ());

    Resource r = response.body ();
    Assert.assertEquals (1, r.entityCount ());
    Assert.assertEquals ("counted", r.get ("action"));
  }
}
