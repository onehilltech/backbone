package com.onehilltech.backbone.data;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class InetAddressTypeAdapter
    implements JsonDeserializer<InetAddress>, JsonSerializer<InetAddress>
{
  @Override
  public InetAddress deserialize (JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
  {
    try
    {
      return InetAddress.getByName (json.getAsString ());
    }
    catch (UnknownHostException e)
    {
      throw new IllegalArgumentException ("Cannot parse address", e);
    }
  }

  @Override
  public JsonElement serialize (InetAddress src, Type typeOfSrc, JsonSerializationContext context)
  {
    return new JsonPrimitive (src.toString ());
  }
}
