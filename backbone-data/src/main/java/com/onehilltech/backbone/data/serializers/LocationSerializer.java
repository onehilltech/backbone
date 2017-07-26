package com.onehilltech.backbone.data.serializers;

import android.location.Location;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.raizlabs.android.dbflow.converter.TypeConverter;

import java.lang.reflect.Type;
import java.util.Locale;

@com.raizlabs.android.dbflow.annotation.TypeConverter
public class LocationSerializer extends TypeConverter <String, Location>
  implements JsonDeserializer <Location>, JsonSerializer <Location>
{
  @Override
  public String getDBValue (Location model)
  {
    return model != null ? String.format (Locale.ENGLISH, "%f,%f", model.getLatitude (), model.getLongitude ()) : null;
  }

  @Override
  public Location getModelValue (String data)
  {
    if (data == null)
      return null;

    String [] coordinates = data.split (",");
    double latitude = Double.parseDouble (coordinates[0]);
    double longitude = Double.parseDouble (coordinates[1]);

    Location location = new Location ("");
    location.setLatitude (latitude);
    location.setLongitude (longitude);

    return location;
  }

  @Override
  public Location deserialize (JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
  {
    if (!json.isJsonObject ())
      throw new JsonParseException ("The location must be an object.");

    JsonObject jsonLocation = json.getAsJsonObject ();

    Location location = new Location ("");
    location.setLatitude (jsonLocation.get ("latitude").getAsDouble ());
    location.setLongitude (jsonLocation.get ("longitude").getAsDouble ());

    return location;
  }

  @Override
  public JsonElement serialize (Location src, Type typeOfSrc, JsonSerializationContext context)
  {
    JsonObject jsonLocation = new JsonObject ();
    jsonLocation.addProperty ("latitude", src.getLatitude ());
    jsonLocation.addProperty ("longitude", src.getLongitude ());

    return jsonLocation;
  }
}
