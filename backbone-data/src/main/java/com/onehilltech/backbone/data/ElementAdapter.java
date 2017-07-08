package com.onehilltech.backbone.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

public interface ElementAdapter
{
  Object fromJson (Gson gson, JsonElement element);
  JsonElement toJson (Gson gson, Object value);
}
