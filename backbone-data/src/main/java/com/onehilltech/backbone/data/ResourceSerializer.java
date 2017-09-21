package com.onehilltech.backbone.data;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @class ResourceSerializer
 *
 * Resource marshaller for Gson extensions.
 */
public class ResourceSerializer
    implements JsonDeserializer <Resource>, JsonSerializer <Resource>
{
  private Gson gson_ = new Gson ();

  private final HashMap <String, Class <?>> types_ = new HashMap<> ();

  public ResourceSerializer ()
  {

  }

  public void setGson (Gson gson)
  {
    this.gson_ = gson;
  }

  public void put (String name, Class <?> dataClass)
  {
    this.types_.put (name, dataClass);
  }

  @Override
  public Resource deserialize (JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException
  {
    // The element is a JSON object. Each field in the object should be a registered
    // object. Iterate over each field and convert it to its concrete type.
    JsonObject obj = json.getAsJsonObject ();
    Resource resource = new Resource ();

    for (Map.Entry <String, JsonElement> entry : obj.entrySet ())
    {
      String field = entry.getKey ();
      JsonElement element = entry.getValue ();

      // Get the name of the resource element, and locate its adapter. We need
      // the adapter to transform the JSON element to a Java object.
      Class <?> dataClass = this.types_.get (field);

      if (dataClass == null)
        throw new JsonParseException (String.format ("%s does not have a registered type", field));

      if (element.isJsonObject ())
      {
        Object value = this.gson_.fromJson (element, dataClass);
        resource.add (field, value);
      }
      else if (element.isJsonArray ())
      {
        JsonArray jsonArray = element.getAsJsonArray ();
        DataModelList list = new DataModelList (jsonArray.size ());

        for (JsonElement item: jsonArray)
          list.add (this.gson_.fromJson (item, dataClass));

        resource.add (field, list);
      }
      else if (element.isJsonNull ())
      {
        resource.add (field, null);
      }
    }

    return resource;
  }

  @Override
  public JsonElement serialize (Resource src, Type typeOfSrc, JsonSerializationContext context)
  {
    JsonObject jsonObject = new JsonObject ();

    for (Map.Entry <String, Object> entry: src.entitySet ())
    {
      String field = entry.getKey ();
      Object object = entry.getValue ();

      boolean isCollection = (object instanceof Collection);

      // Get the name of the resource element, and locate its adapter. We need
      // the adapter to transform the JSON element to a Java object.
      Class <?> dataClass = this.types_.get (field);

      if (dataClass == null)
        throw new JsonParseException (String.format ("%s does not have a registered type", field));

      // Get the name of the resource element, and locate its adapter. We need
      // the adapter to transform the Java object to a JSON element.

      if (isCollection)
      {
        Collection collection = (Collection)object;
        JsonArray array = new JsonArray ();

        for (Object item : collection)
          array.add (this.gson_.toJsonTree (item, dataClass));

        jsonObject.add (field, array);
      }
      else
      {
        JsonElement element = this.gson_.toJsonTree (object, dataClass);
        jsonObject.add (field, element);
      }
    }

    return jsonObject;
  }
}
