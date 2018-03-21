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

  return loader;
}
```  

When using a subclass of `FlowSingleModelLoader`, the loader automatically 
registers for content changes to the target model via `loader.registerForContentChanges()`.
In our example, the loader we created will automatically register for notification
changes to any of the `book` models.
