package com.onehilltech.backbone.data;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.structure.InstanceAdapter;
import com.raizlabs.android.dbflow.structure.InvalidDBConfiguration;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

public class DataModelTypeAdapterFactory implements TypeAdapterFactory
{
  @SuppressWarnings("unchecked")
  @Override
  public <T> TypeAdapter<T> create (Gson gson, TypeToken<T> type)
  {
    Class<? super T> raw = type.getRawType ();

    if (!Object.class.isAssignableFrom (raw))
      return null;

    try
    {
      Class<?> rawType = type.getRawType ();
      InstanceAdapter<? extends DataModel> instanceAdapter =
          (InstanceAdapter<? extends DataModel>) FlowManager.getInstanceAdapter (rawType);

      return (TypeAdapter<T>) this.newDataModelTypeAdapter (gson, instanceAdapter);
    }
    catch (IllegalArgumentException | InvalidDBConfiguration e)
    {
      return null;
    }
  }

  private <E extends DataModel> TypeAdapter<E> newDataModelTypeAdapter (Gson context, final InstanceAdapter<E> instanceAdapter)
  {
    Class<E> modelClass = instanceAdapter.getModelClass ();
    Field[] fields = modelClass.getDeclaredFields ();

    Map<String, BoundField> adapters = new LinkedHashMap<> ();

    for (Field field : fields)
    {
      String fieldName = field.getName ();
      SerializedName serializedName = field.getAnnotation (SerializedName.class);

      if (serializedName != null)
        fieldName = serializedName.value ();

      Class<?> fieldType = field.getType ();
      TypeAdapter<?> fieldTypeAdapter;

      if (fieldType.isPrimitive ())
      {
        // The type adapter for a primitive type should already be registered with the
        // current context.
        fieldTypeAdapter = context.getAdapter (fieldType);
      }
      else
      {
        try
        {
          // Let's see if the non-primitive is a model. If this is the case, it will be
          // considered a foreign key reference. The value should be the id of the referenced
          // object, and this model will be stubbed.
          fieldTypeAdapter = newForeignKeyTypeAdapter (fieldType);
        }
        catch (IllegalArgumentException | InvalidDBConfiguration e)
        {
          fieldTypeAdapter = context.getAdapter (fieldType);
        }
        catch (NoSuchFieldException e)
        {
          throw new AssertionError (e);
        }
      }

      if (fieldTypeAdapter == null)
        throw new IllegalStateException ("No type adapter registered for " + fieldName);

      adapters.put (fieldName, new BoundField (field, fieldTypeAdapter));
    }

    return new DataModelTypeAdapter<> (instanceAdapter, adapters);
  }

  private static class DataModelTypeAdapter <T> extends TypeAdapter<T>
  {
    private final InstanceAdapter<T> instanceAdapter_;

    private final Map<String, BoundField> fields_;

    DataModelTypeAdapter (InstanceAdapter<T> instanceAdapter, Map<String, BoundField> fields)
    {
      this.instanceAdapter_ = instanceAdapter;
      this.fields_ = fields;
    }

    @Override
    public void write (JsonWriter out, T value) throws IOException
    {

    }

    @Override
    public T read (JsonReader in) throws IOException
    {
      T model = this.instanceAdapter_.newInstance ();
      in.beginObject ();

      try
      {
        while (in.hasNext ())
        {
          String name = in.nextName ();
          BoundField boundField = this.fields_.get (name);

          if (boundField != null)
            boundField.read (in, model);
        }
      }
      catch (IllegalAccessException e)
      {
        throw new AssertionError (e);
      }

      in.endObject ();

      return model;
    }
  }

  private static <E> ForeignKeyTypeAdapter newForeignKeyTypeAdapter (Class <E> dataClass)
      throws NoSuchFieldException
  {
    InstanceAdapter <E> instanceAdapter = FlowManager.getInstanceAdapter (dataClass);
    Field idField = dataClass.getField ("_id");

    return new ForeignKeyTypeAdapter <>(instanceAdapter, idField);
  }

  private static class ForeignKeyTypeAdapter <E> extends TypeAdapter <E>
  {
    private final InstanceAdapter <E> instanceAdapter_;
    private final Field idField_;

    ForeignKeyTypeAdapter (InstanceAdapter <E> instanceAdapter, Field idField)
    {
      this.instanceAdapter_ = instanceAdapter;
      this.idField_ = idField;
    }

    @Override
    public void write (JsonWriter out, E value) throws IOException
    {

    }

    @Override
    public E read (JsonReader in) throws IOException
    {
      E refModel = this.instanceAdapter_.newInstance ();
      Class<?> fieldType = this.idField_.getType ();

      try
      {
        if (fieldType.equals (String.class))
          this.idField_.set (refModel, in.nextString ());
        else if (fieldType.equals (long.class) || fieldType.equals (Long.class))
          this.idField_.set (refModel, in.nextLong ());
        else
          throw new IOException ("Foreign key value must be a String or Long [type=" + fieldType + "]");

        return refModel;
      }
      catch (IllegalAccessException e)
      {
        throw new AssertionError (e);
      }
    }
  }

  private static class BoundField
  {
    private final Field field_;

    private final TypeAdapter <?> typeAdapter_;

    BoundField (Field field, TypeAdapter <?> typeAdapter)
    {
      this.field_ = field;
      this.typeAdapter_ = typeAdapter;
    }

    void read (JsonReader in, Object target)
        throws IOException, IllegalAccessException
    {
      Object value = this.typeAdapter_.read (in);
      this.field_.set (target, value);
    }
  }
}

