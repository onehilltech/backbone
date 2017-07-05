package com.onehilltech.backbone.data.serializers;


import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.raizlabs.android.dbflow.converter.TypeConverter;

import org.joda.time.DateTime;

import java.lang.reflect.Type;

public class DateTimeSerializer
    extends TypeConverter <String, DateTime>
    implements JsonDeserializer<DateTime>, JsonSerializer<DateTime>
{
  @Override
  public DateTime deserialize (JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
  {
    return DateTime.parse (json.getAsString ());
  }

  @Override
  public JsonElement serialize (DateTime src, Type typeOfSrc, JsonSerializationContext context)
  {
    return new JsonPrimitive (src.toDateTimeISO ().toString ());
  }

  @Override
  public String getDBValue (DateTime model)
  {
    return model.toDateTimeISO ().toString ();
  }

  @Override
  public DateTime getModelValue (String data)
  {
    return DateTime.parse (data);
  }
}
