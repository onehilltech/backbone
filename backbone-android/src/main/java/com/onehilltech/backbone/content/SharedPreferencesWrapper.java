package com.onehilltech.backbone.content;

import android.content.SharedPreferences;

/**
 * @class SharedPreferencesWrapper
 *
 * Proxy class for Android SharedPreferences. This class makes it where subclasses
 * do not have to implement the expected methods.
 */
public class SharedPreferencesWrapper
{
  private final SharedPreferences sharedPrefs_;

  SharedPreferencesWrapper (SharedPreferences prefs)
  {
    this.sharedPrefs_ = prefs;
  }

  public static class Editor
  {
    private final SharedPreferences.Editor editor_;

    protected Editor (SharedPreferences.Editor editor)
    {
      this.editor_ = editor;
    }

    public void apply ()
    {
      this.editor_.apply ();
    }

    public Editor clear ()
    {
      this.editor_.clear ();
      return this;
    }

    public boolean commit ()
    {
      return this.editor_.commit ();
    }
  }
}
