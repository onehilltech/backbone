package com.onehilltech.backbone.data;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public class ArrayAdapter implements ElementAdapter
{
  private final Class <?> elementType_;

  public ArrayAdapter (Class<?> elementType)
  {
    this.elementType_ = elementType;
  }

  @Override
  public Object fromJson (Gson gson, JsonElement element)
  {
    JsonArray jsonArray = element.getAsJsonArray ();
    DataModelList list = new DataModelList (jsonArray.size ());

    for (JsonElement item: jsonArray)
      list.add (gson.fromJson (item, this.elementType_));

    return list;
  }

  @Override
  public JsonElement toJson (Gson gson, Object value)
  {
    return gson.toJsonTree (value, this.elementType_);
  }
}
