# backbone-android-objectid

A simple library/module for adding ObjectId support on Android

* Based on ObjectId from MongoDB
* Generate new ObjectId just by creating one
* Parse/generate string representation of an ObjectId

## Installation

#### Gradle

```
buildscript {
  repositories {
    maven { url "https://jitpack.io" }
  }
}

dependencies {
  implementation 'com.onehilltech.backbone:backbone-android-objectid:x.y.z'
}
```

## Usage

The default constructor will create a new `ObjectId`.

```java
ObjectId oid = new ObjectId ();
```

The `toString()` method can be used to covert the `ObjectId` to its string format.

```java
oid.toString ()
```

You can also create an `ObjectId` from its string representation.

```java
ObjectId oid = new ObjectId (str);
```
