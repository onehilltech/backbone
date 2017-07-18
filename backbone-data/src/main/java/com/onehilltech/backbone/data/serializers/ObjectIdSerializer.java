package com.onehilltech.backbone.data.serializers;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.onehilltech.backbone.objectid.ObjectId;
import com.raizlabs.android.dbflow.converter.TypeConverter;

import java.lang.reflect.Type;

@com.raizlabs.android.dbflow.annotation.TypeConverter
public class ObjectIdSerializer extends TypeConverter <String, ObjectId>
  implements JsonSerializer <ObjectId>, JsonDeserializer <ObjectId>
{
  @Override
  public String getDBValue (ObjectId model)
  {
    return model.toString ();
  }

  @Override
  public ObjectId getModelValue (String data)
  {
    return new ObjectId (data);
  }

  @Override
  public ObjectId deserialize (JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
  {
    return new ObjectId (json.getAsString ());
  }

  @Override
  public JsonElement serialize (ObjectId src, Type typeOfSrc, JsonSerializationContext context)
  {
    return new JsonPrimitive (src.toString ());
  }
}
