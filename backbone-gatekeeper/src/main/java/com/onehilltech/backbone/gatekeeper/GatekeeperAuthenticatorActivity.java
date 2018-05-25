package com.onehilltech.backbone.gatekeeper;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;

import com.onehilltech.promises.Promise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.onehilltech.promises.Promise.resolved;

public abstract class GatekeeperAuthenticatorActivity extends AccountAuthenticatorActivity
{
  private GatekeeperSessionClient session_;

  private final Logger LOG = LoggerFactory.getLogger (GatekeeperAuthenticatorActivity.class);

  abstract protected String getAccountType ();

  @Override
  protected void onCreate (Bundle savedInstanceState)
  {
    super.onCreate (savedInstanceState);
    this.session_ = GatekeeperSessionClient.getInstance (this);
  }

  /**
   * Sign the user.
   *
   * When the user is successfully signed in, the activity will create the corresponding
   * account for the user.
   *
   * @param username          Username for sign in
   * @param password          Password associated with username
   * @return
   */
  protected Promise <Void> signIn (String username, String password)
  {
    return this.signIn (username, password, false, null);
  }

  /**
   * Sign the user.
   *
   * When the user is successfully signed in, the activity will create the corresponding
   * account for the user.
   *
   * @param username          Username for sign in
   * @param password          Password associated with username
   * @return                  Promise object
   */
  protected Promise <Void> signIn (String username, String password, boolean savePassword, Bundle userData)
  {
    LOG.info ("Signing in the user");

    return this.session_.signIn (this, username, password)
                        .then (resolved (result -> {
                          LOG.info ("Creating an account for the signed in user");

                          // Create a new account object. Then, explicitly add the account to the account
                          // manager and set the authentication token for the account.
                          String accountType = this.getAccountType ();
                          Account account = new Account (username, accountType);
                          AccountManager accountManager = AccountManager.get (this);
                          String authToken = this.session_.getAccessToken ();

                          accountManager.addAccountExplicitly (account, savePassword ? password : null, userData);
                          accountManager.setAuthToken (account, "Bearer", authToken);

                          Intent intent = new Intent();
                          intent.putExtra (AccountManager.KEY_ACCOUNT_NAME, username);
                          intent.putExtra (AccountManager.KEY_ACCOUNT_TYPE, accountType);
                          intent.putExtra (AccountManager.KEY_AUTHTOKEN, authToken);

                          this.setAccountAuthenticatorResult (intent.getExtras());
                          this.setResult (RESULT_OK, intent);
                          this.finish ();
                        }));
  }
}
