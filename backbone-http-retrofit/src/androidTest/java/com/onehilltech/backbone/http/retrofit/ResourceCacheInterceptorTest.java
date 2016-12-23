package com.onehilltech.backbone.http.retrofit;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.onehilltech.backbone.http.BackboneHttp;
import com.onehilltech.backbone.http.BackboneHttpDatabase;
import com.onehilltech.backbone.http.HttpHeaders;
import com.onehilltech.backbone.http.ResourceCache;
import com.onehilltech.backbone.http.ResourceCacheModel;
import com.raizlabs.android.dbflow.config.FlowConfig;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;

@RunWith (AndroidJUnit4.class)
public class ResourceCacheInterceptorTest
{
  private MockWebServer mockWebServer_;
  private FakeService fakeService_;

  private static final String TEST_URL_PATH = "/helloworld";

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
            .client (client)
            .build ();

    this.fakeService_ = retrofit.create (FakeService.class);
  }

  @After
  public void teardown () throws Exception
  {
    if (this.mockWebServer_ != null)
      this.mockWebServer_.shutdown ();
  }

  @Test
  public void testInterceptWithNoCacheEntry () throws Exception
  {
    this.mockWebServer_.enqueue (new MockResponse ().setBody ("Hello, World!"));
    this.fakeService_.getHelloWorld ().execute ();

    RecordedRequest request = this.mockWebServer_.takeRequest ();
    Assert.assertEquals (TEST_URL_PATH, request.getPath ());
    Assert.assertNull (request.getHeader (HttpHeaders.IF_MODIFIED_SINCE));
  }

  @Test
  public void testInterceptWithCacheEntry () throws Exception
  {
    // Add the url to the cache. This should force the interceptor to add the
    // appropriate header to optimize bandwidth usage.
    DateTime lastModified = new DateTime ().minusDays (3);
    URL testURL = this.mockWebServer_.getUrl (TEST_URL_PATH);
    ResourceCache.getInstance ().add (testURL, null, lastModified);

    this.mockWebServer_.enqueue (new MockResponse ().setBody ("Hello, World!"));
    this.fakeService_.getHelloWorld ().execute ();

    RecordedRequest request = this.mockWebServer_.takeRequest ();
    Assert.assertEquals (TEST_URL_PATH, request.getPath ());

    String headerIfModifiedSince = request.getHeader (HttpHeaders.IF_MODIFIED_SINCE);
    Assert.assertNotNull (headerIfModifiedSince);

    // We only get second granularity.
    DateTime actual = DateTime.parse (headerIfModifiedSince, ResourceCacheInterceptor.HTTP_DATE_FORMATTER);
    DateTime expected = lastModified.withMillisOfSecond (0).withZone (DateTimeZone.UTC);
    Assert.assertEquals (expected, actual);
  }

  @Test
  public void testInterceptorUpdateCacheEntry () throws Exception
  {
    // Make sure the cache is clear.
    ResourceCache.getInstance ().clear ();

    // Add the url to the cache. This should force the interceptor to add the
    // appropriate header to optimize bandwidth usage.
    DateTime lastModified = DateTime.now ();
    String lastModifiedStr = lastModified.withZone (DateTimeZone.UTC).toString (ResourceCacheInterceptor.HTTP_DATE_FORMATTER);

    MockResponse mockResponse =
        new MockResponse ()
            .setBody ("Hello, World!")
            .setHeader (HttpHeaders.LAST_MODIFIED, lastModifiedStr);

    this.mockWebServer_.enqueue (mockResponse);
    Response response = this.fakeService_.getHelloWorld ().execute ();
    Assert.assertNotNull (response.headers ().get (HttpHeaders.LAST_MODIFIED));

    RecordedRequest request = this.mockWebServer_.takeRequest ();
    Assert.assertEquals (TEST_URL_PATH, request.getPath ());

    // Check the cache for the new entry.
    URL testURL = this.mockWebServer_.getUrl (TEST_URL_PATH);
    ResourceCacheModel model = ResourceCache.getInstance ().get (testURL);

    Assert.assertNotNull (model);
    Assert.assertEquals (lastModified.withMillisOfSecond (0), model.lastModified);
  }

  interface FakeService
  {
    @GET(TEST_URL_PATH)
    Call <ResponseBody> getHelloWorld ();
  }
}
