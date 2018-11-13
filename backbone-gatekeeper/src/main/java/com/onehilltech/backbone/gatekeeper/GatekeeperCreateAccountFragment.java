package com.onehilltech.backbone.gatekeeper;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.onehilltech.backbone.gatekeeper.http.JsonAccount;

import androidx.fragment.app.Fragment;

import static com.onehilltech.promises.Promise.rejected;
import static com.onehilltech.promises.Promise.resolved;


public class GatekeeperCreateAccountFragment extends Fragment
{
  public interface Listener
  {
    void onAccountCreated (GatekeeperCreateAccountFragment fragment, JsonAccount account);
    void onError (GatekeeperCreateAccountFragment fragment, Throwable t);
  }

  public static final class Builder
  {
    private final Bundle args_ = new Bundle ();

    public Builder setTitle (String title)
    {
      this.args_.putString (ARG_TITLE, title);
      return this;
    }

    public Builder setUsername (String username)
    {
      this.args_.putString (ARG_USERNAME, username);
      return this;
    }

    public Builder setUsernameHint (String hint)
    {
      this.args_.putString (ARG_USERNAME_HINT, hint);
      return this;
    }

    public Builder setPasswordHint (String hint)
    {
      this.args_.putString (ARG_PASSWORD_HINT, hint);
      return this;
    }

    public Builder setPassword (String password)
    {
      this.args_.putString (ARG_PASSWORD, password);
      return this;
    }

    public Builder setCreateButtonText (String text)
    {
      this.args_.putString (ARG_CREATE_BUTTON_TEXT, text);
      return this;
    }

    public Builder setUsernameLabelText (String text)
    {
      this.args_.putString (ARG_USERNAME_LABEL, text);
      return this;
    }

    public Builder setPasswordLabelText (String text)
    {
      this.args_.putString (ARG_PASSWORD_LABEL, text);
      return this;
    }

    public Builder setCreateAccountIntent (Intent intent)
    {
      this.args_.putParcelable (ARG_CREATE_ACCOUNT_INTENT, intent);
      return this;
    }

    public Builder setUsernameIsEmail (boolean value)
    {
      this.args_.putBoolean (ARG_USERNAME_IS_EMAIL, value);
      return this;
    }

    public GatekeeperCreateAccountFragment build ()
    {
      GatekeeperCreateAccountFragment fragment = new GatekeeperCreateAccountFragment ();
      fragment.setArguments (this.args_);

      return fragment;
    }
  }

  private static final String ARG_TITLE = "title";

  private static final String ARG_USERNAME = "username";
  private static final String ARG_USERNAME_HINT = "username_hint";
  private static final String ARG_USERNAME_LABEL = "username_label";

  private static final String ARG_PASSWORD = "password";
  private static final String ARG_PASSWORD_HINT = "password_hint";
  private static final String ARG_PASSWORD_LABEL = "password_label";

  private static final String ARG_CREATE_ACCOUNT_INTENT = "create_account_intent";

  private static final String ARG_CREATE_BUTTON_TEXT = "create_button_text";

  private static final String ARG_USERNAME_IS_EMAIL = "username_is_email";

  private Listener listener_;

  public GatekeeperCreateAccountFragment ()
  {
    // Required empty public constructor
  }

  @Override
  public void onAttach (Context context)
  {
    super.onAttach (context);
    this.listener_ = (Listener) context;
  }

  @Override
  public void onDetach ()
  {
    super.onDetach ();

    this.listener_ = null;
  }

  /**
   * Attempts to sign in or register the account specified by the signIn form. If
   * there are form errors (invalid email, missing fields, etc.), the errors are
   * presented and no actual signIn attempt is made.
   */
  protected void createAccount (String username, String password, String email)
  {
    GatekeeperSessionClient.getInstance (this.getContext ())
                           .createAccount (this.getContext (), username, password, email, true)
                           .then (resolved (value -> this.listener_.onAccountCreated (this, value)))
                           ._catch (rejected (reason -> this.listener_.onError (this, reason)));
  }
}
