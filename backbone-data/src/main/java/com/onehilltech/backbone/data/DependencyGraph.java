package com.onehilltech.backbone.data;

import com.raizlabs.android.dbflow.config.DatabaseDefinition;
import com.raizlabs.android.dbflow.structure.ModelAdapter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class DependencyGraph
{
  public static class Builder
  {
    private DatabaseDefinition database_;

    public Builder (DatabaseDefinition database)
    {
      this.database_ = database;
    }

    @SuppressWarnings ("unchecked")
    public DependencyGraph build ()
    {
      DependencyGraph graph = new DependencyGraph ();

      for (ModelAdapter <?> modelAdapter: this.database_.getModelAdapters ())
      {
        // Add the class for the data model to our dependency graph.
        ModelAdapter <? extends DataModel> dataModelAdapter = (ModelAdapter <? extends DataModel>)modelAdapter;
        graph.addDataModel (dataModelAdapter);

        // Add the dependencies for this data model to the graph.
        Class <? extends DataModel> dataClass = dataModelAdapter.getModelClass ();
        Field[] fields = dataClass.getFields ();

        for (Field field: fields)
        {
          if (field.isSynthetic ())
            continue;

          Class fieldType = field.getType ();
          ModelAdapter <? extends DataModel> targetAdapter = this.database_.getModelAdapterForTable (fieldType);

          if (targetAdapter == null)
            continue;

          graph.addDependency (dataModelAdapter, targetAdapter);
        }
      }

      return graph;
    }
  }

  public static class Node
  {
    private final ModelAdapter <? extends DataModel> modelAdapter_;

    private final Class<? extends DataModel> dataClass_;

    private final String pluralName_;

    private final String singularName_;

    private final ArrayList <Node> depends_ = new ArrayList<> ();

    private Node (ModelAdapter <? extends DataModel> modelAdapter)
    {
      this.modelAdapter_ = modelAdapter;
      this.dataClass_ = modelAdapter.getModelClass ();
      this.pluralName_ = TableUtils.getRawTableName (modelAdapter.getTableName ());
      this.singularName_ = Pluralize.getInstance ().singular (this.pluralName_);
    }

    public List<Node> getDependencies ()
    {
      return Collections.unmodifiableList (this.depends_);
    }

    void addDependency (Node node)
    {
      this.depends_.add (node);
    }

    public boolean isPluraleTantum ()
    {
      return this.singularName_.equals (this.pluralName_);
    }

    public String getPluralName ()
    {
      return this.pluralName_;
    }

    public String getSingularName ()
    {
      return this.singularName_;
    }

    public ModelAdapter <? extends DataModel> getModelAdapter ()
    {
      return this.modelAdapter_;
    }

    public Class<? extends DataModel> getDataClass ()
    {
      return this.dataClass_;
    }
  }

  private final HashMap <Class <? extends DataModel>, Node> nodes_ = new HashMap<> ();

  private DependencyGraph ()
  {

  }

  /**
   * Add a data model to the dependency graph.
   *
   * @param modelAdapter      ModelAdapter object
   */
  void addDataModel (ModelAdapter <? extends DataModel> modelAdapter)
  {
    Class <? extends DataModel> dataClass = modelAdapter.getModelClass ();

    if (this.nodes_.containsKey (dataClass))
      return;

    this.nodes_.put (dataClass, new Node (modelAdapter));
  }

  /**
   * Add a new dependency.
   *
   * @param src
   * @param dst
   */
  void addDependency (ModelAdapter <? extends DataModel> src, ModelAdapter <? extends DataModel> dst)
  {
    Node srcNode = this.getNodeOrCreate (src);
    Node dstNode = this.getNodeOrCreate (dst);

    srcNode.addDependency (dstNode);
  }

  /**
   * Clear the graph.
   */
  void clear ()
  {
    this.nodes_.clear ();
  }

  public List <Node> getInsertOrder (Class <? extends DataModel> dataClass)
  {
    ArrayList <Node> ordering = new ArrayList<> ();
    Node currentNode = this.nodes_.get (dataClass);

    // The current node will be first in our ordering.
    ordering.add (currentNode);
    this.getInsertOrderFrom (currentNode, ordering);

    // Reverse the ordering so we know what to insert first, and what to
    // insert last.
    Collections.reverse (ordering);

    return ordering;
  }

  private void getInsertOrderFrom (Node node, ArrayList <Node> ordering)
  {
    ArrayList <Node> continuation = new ArrayList<> ();

    // The first iteration through the nodes will appends them to the current
    // ordering since this nodes depends on each one. The second iteration goes
    // adds the ordering for each dependency.
    for (Node dependency: node.getDependencies ())
    {
      if (ordering.contains (dependency))
        continue;

      ordering.add (dependency);
      continuation.add (dependency);
    }

    for (Node dependency: continuation)
      this.getInsertOrderFrom (dependency, ordering);
  }

  private Node getNodeOrCreate (ModelAdapter <? extends DataModel> modelAdapter)
  {
    Class <? extends DataModel> dataClass = modelAdapter.getModelClass ();
    Node node = this.nodes_.get (dataClass);

    if (node != null)
      return node;

    node = new Node (modelAdapter);
    this.nodes_.put (dataClass, node);

    return node;
  }


}
