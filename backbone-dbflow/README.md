# backbone-dbflow

Utility library for extension classes to DBFlow

* Supports Android loader framework
* Loaders and cursors for binding data models to Android views
* Automatically reload data (and update views) when original data source changes

## Installation

#### Gradle

```
buildscript {
  repositories {
    maven { url "https://jitpack.io" }
  }
}

dependencies {
  compile 'com.onehilltech.backbone:backbone-dbflow:x.y.z'
}
```

## Usage

### Loading models using loaders

The Android framework use a concept called 
[Loaders](https://developer.android.com/guide/components/loaders.html), which
allows you to load data from a data source. One of the advantages of using loaders
is their lifecycle is managed by the hosting `Activity` or `Fragment`. 

DBFlow, by default, provides different mechanisms for loading data from a SQLite
database. DBFlow, however, does not provide any mechanisms for loading data in the
context of the Android loaders.

We provide several loaders that can be used to load DBFlow models via loaders. We
have loaders for loading a single model:

* `FlowModelLoader`
* `FlowModelViewLoader`
* `FlowQueryModelLoader`

And, we have loaders for loading a collection of models:

* `FlowCursorLoader`

Let's look at loading a single model. For our example, we are going to use the
following DBFlow model:

```javascript
@Table(name = "books", database = LibraryDatabase.class)
public class Book extends BaseModel
{
  @PrimaryKey
  public long _id;

  @ForeignKey(stubbedRelationship = true)
  public User author;

  @Column
  public String title;

  public Book () { }
  public Book (long id) { this._id = id; }
}
```

Using the model above, we can use the `FlowModelLoader` to load a single book
model stored in our SQLite database. For simplicity, we only show the code for 
creating the loader.

```javascript
@Override
public Loader<Book> onCreateLoader (int id, Bundle args)
{
  Queriable queriable =
      SQLite.select ()
            .from (Book.class)
            .where (Book_Table._id.eq (1));

  return new FlowModelLoader (this, Book.class, queriable);
}

@Override
public void onLoadFinished (Loader<Book> loader, Book book)
{
  // The book variable is the one loaded above.
}
```  

When using a subclass of `FlowSingleModelLoader`, the loader automatically 
registers for content changes to the target model via `loader.registerForContentChanges()`.
In our example, the loader we created will automatically register for notification
changes to any of the `book` models (i.e., the loader will reload its data).

All single model loaders behavior this way.

### Using cursor loaders

There are cases where putting the loaded models into a collection is not sufficient
due to limited resource availability. For example, executing a query the would result
in loading 100s of models into an array. In these cases, it is better to load the data
via cursor.

The `FlowCursorLoader` is similar to the `FlowSingleModelLoader` classes discussed
above in that you must construct it with with a `Queriable` object. The main difference
is between the two classes of loaders is

1. The model type returned from the loader is a `FlowCursor`.
2. You must manually register the model types that trigger reloads.
3. You must use a `ModelAdapter` to extract the data model from each cursor location.
4. The `FlowCursorAdapter` and `FlowSimpleCursorAdapter` can replace the need for `ModelAdapter`.

Here is an example for loading all the books in the database.

```javascript
@Override
public FlowCursorLoader onCreateLoader (int id, Bundle args)
{
  Queriable queriable = SQLite.select () .from (Book.class);
  FlowCursorLoader loader = FlowCursorLoader (this, Book.class, queriable);
  
  loader.registerForContentChanges (Book.class);
  
  return loader;
}

@Override
public void onLoadFinished (Loader<FlowCursor> loader, FlowCursor cursor)
{
  // The book variable is the one loaded above.
  this.adapter_.changeCursor (cursor);
}
``` 

Now, anytime a change occurs to a model in the `books` table, this loader will
reset and reload the data. 

Lastly, the cursor can be passed to any adapter.

### Using adapters to view the data
 
Android uses adapters to adapt data from different sources to its views. We 
provide 2 adapters to integrating DBFlow cursor with the adapters.

* `FlowCursorAdapter`
* `FlowSimpleCursorAdapter`

Both adapters are constructed with a reference to the target model class.

```javascript
FlowCursorAdapter adapter = new FlowCursorAdapter (context, Book.class, cursor, 0);
```

The `FlowSimpleCursorAdapter`, however, is a the `SimpleCursorAdapter` for DBFlow. This
means you must provide the layout to use for when displaying each item, and the mapping
of the columns to widgets.
