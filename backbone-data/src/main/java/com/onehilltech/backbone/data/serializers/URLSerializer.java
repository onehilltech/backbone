package com.onehilltech.backbone.data.serializers;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.raizlabs.android.dbflow.converter.TypeConverter;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;

public class URLSerializer extends TypeConverter<String, URL>
    implements JsonSerializer <URL>, JsonDeserializer <URL>
{
  @Override
  public URL deserialize (JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
  {
    try
    {
      return new URL (json.getAsString ());
    }
    catch (MalformedURLException e)
    {
      throw new JsonParseException (e);
    }
  }

  @Override
  public JsonElement serialize (URL src, Type typeOfSrc, JsonSerializationContext context)
  {
    return new JsonPrimitive (src.toString ());
  }

  @Override
  public String getDBValue (URL model)
  {
    return model.toString ();
  }

  @Override
  public URL getModelValue (String data)
  {
    try
    {
      return new URL (data);
    }
    catch (MalformedURLException e)
    {
      throw new AssertionError (e);
    }
  }
}
