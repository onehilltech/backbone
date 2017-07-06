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
    extends TypeConverter <Long, DateTime>
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
  public Long getDBValue (DateTime model)
  {
    return model.getMillis ();
  }

  @Override
  public DateTime getModelValue (Long data)
  {
    return new DateTime (data);
  }
}
