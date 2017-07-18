package com.onehilltech.backbone.data;

import com.onehilltech.promises.Promise;

/**
 * @class DataModel
 *
 * Base class for all data model objects.
 */
public abstract class DataModel <T extends DataModel>
{
  /// Data store associated with the data model.
  private DataStore store_;

  void assignTo (DataStore store)
  {
    this.store_ = store;
  }

  /**
   * Get the parent data store for this model. If the model has not been created, then
   * it will not be assigned to a data store.
   *
   * @return          DataStore object
   */
  public DataStore getStore ()
  {
    return this.store_;
  }

  /**
   * Update the model on both the server, and the local database.
   *
   * @return        Promise object
   */
  @SuppressWarnings ("unchecked")
  public Promise <T> update ()
  {
    if (this.store_ == null)
      throw new IllegalStateException ("You must first create the model using the data store.");

    Class <T> dataClass = (Class <T>)this.getClass ();
    T model = (T)this;

    return this.store_.update (dataClass, model);
  }

  /**
   * Delete the model.
   *
   * @return       Promise object
   */
  @SuppressWarnings ("unchecked")
  public Promise <Boolean> delete ()
  {
    if (this.store_ == null)
      throw new IllegalStateException ("You must first create the model using the data store.");

    Class <T> dataClass = (Class <T>)this.getClass ();
    T model = (T)this;

    return this.store_.delete (dataClass, model);
  }
}
