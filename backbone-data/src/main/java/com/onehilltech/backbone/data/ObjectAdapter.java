package com.onehilltech.backbone.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class ObjectAdapter implements ElementAdapter
{
  private final Class <?> elementType_;

  public ObjectAdapter (Class<?> elementType)
  {
    this.elementType_ = elementType;
  }

  @Override
  public Object fromJson (Gson gson, JsonElement element)
  {
    return gson.fromJson (element, this.elementType_);
  }

  @Override
  public JsonElement toJson (Gson gson, Object value)
  {
    return gson.toJsonTree (value, this.elementType_);
  }
}
