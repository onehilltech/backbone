package com.onehilltech.backbone.http;

import com.raizlabs.android.dbflow.converter.TypeConverter;

import org.joda.time.DateTime;

@com.raizlabs.android.dbflow.annotation.TypeConverter
public class DateTimeTypeConverter extends TypeConverter <Long, DateTime>
{
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
