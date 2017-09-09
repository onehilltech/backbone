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

@com.raizlabs.android.dbflow.annotation.TypeConverter
public class DateTimeSerializer
    extends TypeConverter <Long, DateTime>
    implements JsonDeserializer<DateTime>, JsonSerializer<DateTime>
{
  @Override
  public DateTime deserialize (JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
  {
    if (!json.isJsonPrimitive ())
      throw new JsonParseException ("Date must be a primitive type");

    JsonPrimitive jsonPrimitive = json.getAsJsonPrimitive ();

    if (jsonPrimitive.isString ())
      return DateTime.parse (json.getAsString ());
    else if (jsonPrimitive.isNumber ())
      return new DateTime (jsonPrimitive.getAsLong ());
    else
      throw new JsonParseException ("Date must be either a String or a Number");
  }

  @Override
  public JsonElement serialize (DateTime src, Type typeOfSrc, JsonSerializationContext context)
  {
    return new JsonPrimitive (src.toDateTimeISO ().toString ());
  }

  @Override
  public Long getDBValue (DateTime model)
  {
    return model != null ? model.getMillis () : null;
  }

  @Override
  public DateTime getModelValue (Long data)
  {
    return data != null ? new DateTime (data) : null;
  }
}
