package com.onehilltech.backbone.gatekeeper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import static com.onehilltech.promises.Promise.rejected;
import static com.onehilltech.promises.Promise.resolved;
import static com.onehilltech.promises.RejectedOnUIThread.onUiThread;

/**
 * @class GatekeeperSignInFragment
 *
 * Base class for all signIn fragments. The GatekeeperSignInFragment initializes a
 * session client, and provide a signIn() method to perform the signIn task.
 */
public class GatekeeperSignInFragment extends Fragment
{
  public interface LoginFragmentListener
  {
    void onSignInComplete (GatekeeperSignInFragment fragment);
  }

  public static final class Builder
  {
    private final Bundle args_ = new Bundle ();

    private GatekeeperSignInFragment signInFragment_;

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

    public Builder setSignInButtonText (String text)
    {
      this.args_.putString (ARG_SIGN_IN_BUTTON_TEXT, text);
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

    public Builder setErrorMessage (String errorMessage)
    {
      this.args_.putString (ARG_ERROR_MESSAGE, errorMessage);
      return this;
    }

    public Builder setLayout (int layout)
    {
      this.args_.putInt (ARG_LAYOUT, layout);
      return this;
    }

    public Builder setSignInFragment (GatekeeperSignInFragment fragment)
    {
      this.signInFragment_ = fragment;
      return this;
    }

    public GatekeeperSignInFragment build ()
    {
      GatekeeperSignInFragment fragment =
          this.signInFragment_ != null ?
              this.signInFragment_ :
              new GatekeeperSignInFragment ();

      fragment.setArguments (this.args_);

      return fragment;
    }
  }

  private LoginFragmentListener loginFragmentListener_;

  private static final String ARG_TITLE = "title";

  private static final String ARG_USERNAME = "username";
  private static final String ARG_USERNAME_HINT = "username_hint";
  private static final String ARG_USERNAME_LABEL = "username_label";

  private static final String ARG_PASSWORD = "password";
  private static final String ARG_PASSWORD_HINT = "password_hint";
  private static final String ARG_PASSWORD_LABEL = "password_label";

  private static final String ARG_CREATE_ACCOUNT_INTENT = "create_account_intent";

  private static final String ARG_SIGN_IN_BUTTON_TEXT = "sign_in_button_text";
  private static final String ARG_ERROR_MESSAGE = "error_message";

  private static final String ARG_LAYOUT = "layout";

  private GatekeeperSessionClient sessionClient_;

  /**
   * Default constructor.
   */
  public GatekeeperSignInFragment ()
  {
    // Required empty public constructor
  }

  @Override
  public void onAttach (Context context)
  {
    super.onAttach (context);

    this.sessionClient_ = GatekeeperSessionClient.getInstance (context);

    try
    {
      this.loginFragmentListener_ = (LoginFragmentListener) context;
    }
    catch (ClassCastException e)
    {
      throw new ClassCastException (context + " must implement LoginFragmentListener");
    }
  }

  protected void signIn (String username, String password)
  {
    this.sessionClient_.signIn (username, password)
                       .then (resolved (value -> loginFragmentListener_.onSignInComplete (this)))
                       ._catch (onUiThread (rejected (reason -> showErrorMessage (reason.getLocalizedMessage ()))));
  }


  @Override
  public void onDetach ()
  {
    super.onDetach ();

    this.loginFragmentListener_ = null;
  }

  /**
   * Start the activity for creating a new account.
   */
  protected void startNewAccountActivity (Intent targetIntent)
  {
    // Start the activity for creating a new account.
    Activity activity = getActivity ();

    Intent upIntent = activity.getIntent ();
    Intent redirectIntent = upIntent.getParcelableExtra (GatekeeperSignInActivity.ARG_REDIRECT_INTENT);

    targetIntent.putExtra (GatekeeperCreateAccountActivity.EXTRA_REDIRECT_INTENT, redirectIntent);
    targetIntent.putExtra (GatekeeperCreateAccountActivity.EXTRA_UP_INTENT, upIntent);

    // Start the activity for creating the account, and finish this activity.
    activity.startActivity (targetIntent);
    activity.finish ();
  }

  /**
   * Show an error message. This causes all views to be hidden except for the logo and
   * the error message. When this happens, the user must exit the parent activity.
   *
   * @param message
   */
  protected void showErrorMessage (String message)
  {

  }
}
