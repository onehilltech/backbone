package com.onehilltech.backbone.gatekeeper;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public abstract class GatekeeperAuthenticatedActivity extends AppCompatActivity
{
  abstract protected String getAccountType ();

  @Override
  protected void onStart ()
  {
    super.onStart ();

    // Make sure there is at least one account on the device. If there is an
    // account already active
    AccountManager am = AccountManager.get (this);
    Account[] accounts = am.getAccountsByType (this.getAccountType ());

    if (accounts.length == 0)
      am.addAccount (this.getAccountType (),
                     "Bearer",
                     null,
                     null,
                     this,
                     this::onAddAccountComplete,
                     null);
  }

  /**
   * Method called with adding an account is complete.
   *
   * @param future        Completion future
   */
  protected void onAddAccountComplete (AccountManagerFuture<Bundle> future)
  {

  }
}
