package com.onehilltech.backbone.firebase;

import android.content.Context;
import android.content.SharedPreferences;

class CloudMessagingPreferences
{
  private SharedPreferences prefs_;

  private static final String NAME = "cloud-messaging.prefs";
  private static final String PREF_CLAIM_TICKET = "claim_ticket";

  public static class Editor
  {
    private SharedPreferences.Editor editor_;

    private Editor (SharedPreferences.Editor editor)
    {
      this.editor_ = editor;
    }

    public Editor putClaimTicket (String claimTicket)
    {
      this.editor_.putString (PREF_CLAIM_TICKET, claimTicket);


      return this;
    }

    public Editor removeClaimTicket ()
    {
      this.editor_.remove (PREF_CLAIM_TICKET);

      return this;
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

  public static CloudMessagingPreferences open (Context context)
  {
    SharedPreferences prefs = context.getSharedPreferences (NAME, Context.MODE_PRIVATE);
    return new CloudMessagingPreferences (prefs);
  }

  private CloudMessagingPreferences (SharedPreferences prefs)
  {
    this.prefs_ = prefs;
  }

  public String getClaimTicket ()
  {
    return this.prefs_.getString (PREF_CLAIM_TICKET, null);
  }

  public Editor edit ()
  {
    return new Editor (this.prefs_.edit ());
  }
}
