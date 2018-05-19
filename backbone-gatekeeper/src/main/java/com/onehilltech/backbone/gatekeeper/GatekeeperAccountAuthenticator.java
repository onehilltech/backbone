package com.onehilltech.backbone.gatekeeper;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.SyncStateContract;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @class AccountAuthenticator
 *
 * Provide access to the accounts. Right now, we do not permit third-party access
 * to our services.
 */
public class GatekeeperAccountAuthenticator extends AbstractAccountAuthenticator
{
  private final Context context_;
  private final Class <AccountAuthenticatorActivity> activityClass_;

  private final Logger LOG = LoggerFactory.getLogger (GatekeeperAccountAuthenticator.class);

  public GatekeeperAccountAuthenticator (Context context, Class <AccountAuthenticatorActivity> activityClass)
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
      throws NetworkErrorException
  {
    Intent intent = new Intent (this.context_, this.activityClass_);
    intent.putExtra (SyncStateContract.Constants.ACCOUNT_TYPE, authTokenType);
    intent.putExtra (AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

    Bundle result = new Bundle();
    result.putParcelable (AccountManager.KEY_INTENT, intent);

    return result;
  }

  @Override
  public Bundle confirmCredentials (AccountAuthenticatorResponse response,
                                    Account account,
                                    Bundle options)
      throws NetworkErrorException
  {
    return null;
  }

  @Override
  public Bundle getAuthToken (AccountAuthenticatorResponse response,
                              Account account,
                              String authTokenType,
                              Bundle options)
      throws NetworkErrorException
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
      throws NetworkErrorException
  {
    return null;
  }

  @Override
  public Bundle hasFeatures (AccountAuthenticatorResponse response,
                             Account account,
                             String[] features)
      throws NetworkErrorException
  {
    return null;
  }
}
