package com.onehilltech.backbone.data;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.structure.container.ForeignKeyContainer;
import com.raizlabs.android.dbflow.structure.container.ModelContainerAdapter;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;

public class DataModelTypeAdapterFactory implements TypeAdapterFactory
{
  @Override
  public <T> TypeAdapter<T> create (Gson gson, TypeToken<T> type)
  {
    Class<T> rawType = (Class<T>) type.getRawType();

    if (rawType.equals (ForeignKeyContainer.class))
    {
      ParameterizedType foreignKeyType = (ParameterizedType)type.getType ();
      final Class <? extends DataModel> modelClass = (Class <? extends DataModel>)foreignKeyType.getActualTypeArguments ()[0];

      return (TypeAdapter<T>)this.newForeignKeyContainerAdapter (modelClass);
    }
    else
    {
      return null;
    }
  }

  private <E extends DataModel> TypeAdapter<ForeignKeyContainer<E>> newForeignKeyContainerAdapter(final Class <E> modelClass)
  {
    return new TypeAdapter<ForeignKeyContainer <E>> ()
    {
      @Override
      public void write (JsonWriter out, ForeignKeyContainer <E> value) throws IOException
      {

      }

      @Override
      public ForeignKeyContainer <E> read (JsonReader in) throws IOException
      {
        // Create a new instance of the target model, and set its id.
        @SuppressWarnings ("unchecked")
        E dataModel = (E)FlowManager.getInstanceAdapter (modelClass).newInstance ();
        dataModel.setId (in.nextString ());

        ModelContainerAdapter <E> modelContainerAdapter = FlowManager.getContainerAdapter (modelClass);
        return modelContainerAdapter.toForeignKeyContainer (dataModel);
      }
    };
  }
}
