package com.onehilltech.backbone.data;

import com.onehilltech.backbone.http.HttpHeaders;
import com.onehilltech.backbone.http.ResourceCache;
import com.onehilltech.backbone.http.ResourceCacheModel;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.net.URL;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class ResourceCacheInterceptor implements Interceptor
{
  public static final String HTTP_DATE_PATTERN = "EEE, dd MMM yyyy HH:mm:ss z";
  public static final DateTimeFormatter HTTP_DATE_FORMATTER = DateTimeFormat.forPattern (HTTP_DATE_PATTERN);

  @Override
  public Response intercept (Chain chain) throws IOException
  {
    // Check the resource cache for the url. If the url does not exist, or
    // there is no last modified date, then do not update the request headers.
    Request request = chain.request ();
    URL url = request.url ().url ();

    ResourceCache cache = ResourceCache.getInstance ();

    if (cache.getIsCachingEnabled ())
    {
      ResourceCacheModel model = cache.get (url);

      if (model != null && model.lastModified != null)
      {
        // Make a new request object with the addition of the Last-Modified header
        // to the request. The date must be in UTC format.

        DateTime utcDateTime = model.lastModified.withZone (DateTimeZone.UTC);
        String ifModifiedSince = utcDateTime.toString (HTTP_DATE_FORMATTER);

        Request.Builder builder =
            request.newBuilder ()
                   .header (HttpHeaders.IF_MODIFIED_SINCE, ifModifiedSince);

        request = builder.build ();
      }
    }

    Response response = chain.proceed (request);

    if (response.isSuccessful () && cache.getIsCachingEnabled ())
    {
      // We need to check the response headers for the Last-Modified header. If
      // it exist, we need to update the entry in our local cache.

      String lastModified = response.header (HttpHeaders.LAST_MODIFIED);
      String eTag = response.header (HttpHeaders.ETAG);

      if (lastModified != null || eTag != null)
      {
        DateTime date =
            lastModified != null ?
                DateTime.parse (lastModified, HTTP_DATE_FORMATTER) :
                null;

        cache.add (url, eTag, date);
      }
    }

    return response;
  }
}
