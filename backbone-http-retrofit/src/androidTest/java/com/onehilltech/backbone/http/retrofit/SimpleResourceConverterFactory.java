package com.onehilltech.backbone.http.retrofit;

import com.onehilltech.backbone.http.Resource;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

class SimpleResourceConverterFactory extends Converter.Factory
{
  private static final MediaType MEDIA_TYPE = MediaType.parse("text/plain; charset=UTF-8");

  @Override
  public Converter<?, RequestBody> requestBodyConverter (Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit)
  {
    return type.equals (Resource.class) ? new ResourceToRequestBodyConverter () : null;
  }

  @Override
  public Converter<ResponseBody, ?> responseBodyConverter (Type type, Annotation[] annotations, Retrofit retrofit)
  {
    return type.equals (Resource.class) ? new ResponseBodyToResourceConverter () : null;
  }

  @Override
  public Converter<?, String> stringConverter (Type type, Annotation[] annotations, Retrofit retrofit)
  {
    return null;
  }

  private static class ResourceToRequestBodyConverter implements Converter <Resource, RequestBody>
  {
    @Override
    public RequestBody convert (Resource value) throws IOException
    {
      StringBuilder builder = new StringBuilder ();

      Iterator <Map.Entry <String, Object>> iterator = value.entitySet ().iterator ();

      while (iterator.hasNext ())
      {
        Map.Entry <String, Object> entry = iterator.next ();
        builder.append (entry.getKey ()).append ("=").append (entry.getValue ());

        if (iterator.hasNext ())
          builder.append (";");
      }

      return RequestBody.create (MEDIA_TYPE, builder.toString ());
    }
  }

  private static class ResponseBodyToResourceConverter implements Converter <ResponseBody, Resource>
  {
    @Override
    public Resource convert (ResponseBody value) throws IOException
    {
      String [] entries = value.string ().split (";");
      Resource r = new Resource ();

      for (String entry: entries)
      {
        String [] parts = entry.split ("=");
        r.add (parts[0], parts[1]);
      }

      return r;
    }
  }
}
