package com.onehilltech.backbone.gatekeeper;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * @class AccountAuthenticator
 *
 * Provide access to the accounts. Right now, we do not permit third-party access
 * to our services.
 */
public class GatekeeperAccountAuthenticator extends AbstractAccountAuthenticator
{
  private final Context context_;
  private final Class <? extends AccountAuthenticatorActivity> activityClass_;

  public GatekeeperAccountAuthenticator (Context context, Class <? extends AccountAuthenticatorActivity> activityClass)
  {
    super (context);

    this.context_ = context;
    this.activityClass_ = activityClass;
  }

  @Override
  public Bundle editProperties (AccountAuthenticatorResponse response, String accountType)
  {
    return null;
  }

  @Override
  public Bundle addAccount (AccountAuthenticatorResponse response,
                            String accountType,
                            String authTokenType,
                            String[] requiredFeatures,
                            Bundle options)
  {
    Intent intent = new Intent (this.context_, this.activityClass_);
    intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

    Bundle result = new Bundle();
    result.putParcelable (AccountManager.KEY_INTENT, intent);

    return result;
  }

  @Override
  public Bundle confirmCredentials (AccountAuthenticatorResponse response,
                                    Account account,
                                    Bundle options)
  {
    return null;
  }

  @Override
  public Bundle getAuthToken (AccountAuthenticatorResponse response,
                              Account account,
                              String authTokenType,
                              Bundle options)
  {
    return null;
  }

  @Override
  public String getAuthTokenLabel (String authTokenType)
  {
    return authTokenType;
  }

  @Override
  public Bundle updateCredentials (AccountAuthenticatorResponse response,
                                   Account account,
                                   String authTokenType,
                                   Bundle options)
  {
    return null;
  }

  @Override
  public Bundle hasFeatures (AccountAuthenticatorResponse response,
                             Account account,
                             String[] features)
  {
    return null;
  }
}
