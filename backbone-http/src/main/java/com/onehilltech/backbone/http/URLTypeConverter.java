package com.onehilltech.backbone.http;

import com.raizlabs.android.dbflow.converter.TypeConverter;

import java.net.MalformedURLException;
import java.net.URL;

public class URLTypeConverter extends TypeConverter <String, URL>
{
  @Override
  public String getDBValue (URL model)
  {
    return model.toString ();
  }

  @Override
  public URL getModelValue (String data)
  {
    try
    {
      return new URL (data);
    }
    catch (MalformedURLException e)
    {
      throw new IllegalArgumentException ("Failed to convert model to URL", e);
    }
  }
}
