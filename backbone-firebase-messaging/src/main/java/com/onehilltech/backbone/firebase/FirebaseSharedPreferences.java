package com.onehilltech.backbone.firebase;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.iid.FirebaseInstanceId;

class FirebaseSharedPreferences
{
  private SharedPreferences prefs_;

  private static final String NAME = "firebase.prefs";
  private static final String PREF_INSTANCE_ID = "device";
  private static final String PREF_TOKEN = "token";

  public static class Editor
  {
    private SharedPreferences.Editor editor_;

    private Editor (SharedPreferences.Editor editor)
    {
      this.editor_ = editor;
    }

    public Editor putInstance (FirebaseInstanceId instance)
    {
      this.editor_.putString (PREF_INSTANCE_ID, instance.getId ());
      this.editor_.putString (PREF_TOKEN, instance.getToken ());

      return this;
    }

    public boolean commit ()
    {
      return this.editor_.commit ();
    }
  }

  public static FirebaseSharedPreferences open (Context context)
  {
    SharedPreferences prefs = context.getSharedPreferences (NAME, Context.MODE_PRIVATE);
    return new FirebaseSharedPreferences (prefs);
  }

  private FirebaseSharedPreferences (SharedPreferences prefs)
  {
    this.prefs_ = prefs;
  }

  public String getDeviceId ()
  {
    return this.prefs_.getString (PREF_INSTANCE_ID, null);
  }

  public String getToken ()
  {
    return this.prefs_.getString (PREF_TOKEN, null);
  }

  public Editor edit ()
  {
    return new Editor (this.prefs_.edit ());
  }
}
