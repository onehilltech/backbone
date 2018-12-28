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

  @Override
  protected void onDestroy ()
  {
    super.onDestroy ();

    if (this.session_ != null)
      this.session_.onDestroy (this);
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
    LOG.info ("Signing the user into their account.");

    return this.session_.signIn (username, password)
                        .then (resolved (result -> this.completeSignIn (username, password, savePassword, userData)));
  }

  /**
   * Sign up a new user for the service.
   *
   * @param username        Username
   * @param password        Password
   * @param email           Email address
   * @return                Promise object
   */
  protected Promise <Void> signUp (String username, String password, String email)
  {
    return this.signUp (username, password, email, false, null);
  }

  /**
   * Sign up a new user for the service.
   *
   * @param username        Username
   * @param password        Password
   * @param email           Email address
   * @param savePassword    Save the password to the account
   * @param userData        User data associated with account
   * @return                Promise object
   */
  protected Promise <Void> signUp (String username, String password, String email, boolean savePassword, Bundle userData)
  {
    LOG.info ("Signing up the user for a new account");

    return this.session_.createAccount (username, password, email, true)
                        .then (resolved (account -> this.completeSignIn (username, password, savePassword, userData)));
  }

  private void completeSignIn (String username, String password, boolean savePassword, Bundle userData)
  {
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
  }
}
