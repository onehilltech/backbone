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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class DataModelTypeAdapterFactory implements TypeAdapterFactory
{
  private final Map <Class <?>, TypeAdapter <?>> cache_ = new HashMap<> ();

  @SuppressWarnings("unchecked")
  @Override
  public <T> TypeAdapter<T> create (Gson gson, TypeToken<T> type)
  {
    Class<? super T> raw = type.getRawType ();

    if (!Object.class.isAssignableFrom (raw))
      return null;

    try
    {
      // Check our cache first so we are not creating the type adapter multiple
      // times throughout the lifetime of the application.
      Class<?> rawType = type.getRawType ();
      TypeAdapter<T> typeAdapter = (TypeAdapter <T>)this.cache_.get (rawType);

      if (typeAdapter != null)
        return typeAdapter;

      // Create a new type adapter for this element.
      InstanceAdapter<? extends DataModel> instanceAdapter =
          (InstanceAdapter<? extends DataModel>) FlowManager.getInstanceAdapter (rawType);

      typeAdapter = (TypeAdapter <T>)this.newDataModelTypeAdapter (gson, instanceAdapter);
      this.cache_.put (rawType, typeAdapter);

      return typeAdapter;
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
      if (field.isSynthetic ())
        continue;

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
      try
      {
        out.beginObject ();

        for (Map.Entry<String, BoundField> entry : this.fields_.entrySet ())
        {
          BoundField boundField = entry.getValue ();
          out.name (entry.getKey ());

          boundField.write (out, value);
        }

        out.endObject ();
      }
      catch (IllegalAccessException e)
      {
        throw new IOException ("Failed to write object", e);
      }
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
          else
            in.skipValue ();
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
    private final Class <?> idType_;

    ForeignKeyTypeAdapter (InstanceAdapter <E> instanceAdapter, Field idField)
    {
      this.instanceAdapter_ = instanceAdapter;
      this.idField_ = idField;
      this.idType_ = idField.getType ();
    }

    @Override
    public void write (JsonWriter out, E value) throws IOException
    {
      try
      {
        Object id = this.idField_.get (value);

        if (this.idType_.equals (String.class))
          out.value ((String)id);
        else if (this.idType_.equals (long.class) || this.idType_.equals (Long.class))
          out.value ((Number)id);
        else
          throw new IOException ("Foreign key value must be a String or Long [type=" + this.idType_ + "]");
      }
      catch (IllegalAccessException e)
      {
        throw new IOException ("Failed to read id", e);
      }
    }

    @Override
    public E read (JsonReader in) throws IOException
    {
      E refModel = this.instanceAdapter_.newInstance ();

      try
      {
        if (this.idType_.equals (String.class))
          this.idField_.set (refModel, in.nextString ());
        else if (this.idType_.equals (long.class) || this.idType_.equals (Long.class))
          this.idField_.set (refModel, in.nextLong ());
        else
          throw new IOException ("Foreign key value must be a String or Long [type=" + this.idType_ + "]");

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
    private final TypeAdapter typeAdapter_;

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

    @SuppressWarnings ("unchecked")
    void write (JsonWriter out, Object target)
        throws IllegalAccessException, IOException
    {
      Object value = this.field_.get (target);

      if (value != null)
        this.typeAdapter_.write (out, value);
      else
        out.nullValue ();
    }
  }
}

