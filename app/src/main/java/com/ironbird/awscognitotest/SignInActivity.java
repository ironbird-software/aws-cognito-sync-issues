package com.ironbird.awscognitotest;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.ironbird.awscognitotest.account.AccountManager;

import java.util.HashMap;
import java.util.Map;

public class SignInActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, Constants {


    //***********************************************************************//
    //                           Inner classes                               //
    //***********************************************************************//


    private class CreateIdentityTask extends AsyncTask<Map<String, String>, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(Map<String, String>... logins) {

            String identity = AccountManager.getInstance().getIdentityId(logins[0]);

            if (identity != null && !identity.isEmpty()) {

                Log.v(TAG, String.format("Obtained AWS identity %s", identity));

                // Storing last used provider & token (will be used when refreshing)
                String provider = (String) logins[0].keySet().toArray()[0];
                String token = logins[0].get(provider);
                SharedPreferences settings = getSharedPreferences(PREF_FILE, Activity.MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(KEY_LAST_USED_PROVIDER, provider);
                editor.putString(KEY_PROVIDER_TOKEN, token);
                editor.apply();

                return Boolean.TRUE;
            }

            Log.e(TAG, "Failed to obtain a valid identity... In all likelihood, this will never show... An exception happened above...");
            return Boolean.FALSE;
        }


        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            if (result) {
                // We successfully logged in... Can bo back to main activity
                SignInActivity.this.finish();
            }
        }
    }


    //***********************************************************************//
    //                          Class variables                              //
    //***********************************************************************//

    private static String TAG = SignInActivity.class.getSimpleName();

    private static final String FIRST_NAME = "first_name";
    private static final String LAST_NAME = "last_name";
    private static final String EMAIL = "email";


    //***********************************************************************//
    //                         Instance variables                            //
    //***********************************************************************//

    //Activity requests
    private static final int GOOGLE_SIGN_IN = 1;

    // Google
    private SignInButton googleSignInBtn;
    private GoogleApiClient googleApiClient;

    // Facebook
    private LoginButton facebookSignInBtn;
    private CallbackManager callbackManager;


    //***********************************************************************//
    //                            Constructors                               //
    //***********************************************************************//


    //***********************************************************************//
    //                         Getters and setters                           //
    //***********************************************************************//


    //***********************************************************************//
    //                               Interfaces                              //
    //***********************************************************************//

    /* Implements GoogleApiClient.OnConnectionFailedListener */

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        String message = String.format("Failed to connect to Google [error #%d, %s]...", connectionResult.getErrorCode(), connectionResult.getErrorMessage());
        Log.e(TAG, message);
    }

    //***********************************************************************//
    //                               Overrides                               //
    //***********************************************************************//

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        googleSignInBtn = (SignInButton) this.findViewById(R.id.signin_with_google_btn);
        facebookSignInBtn = (LoginButton) this.findViewById(R.id.signin_with_facebook_btn);

        this.setUpGoogleSignIn();

        this.setUpFacebookSignIn();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GOOGLE_SIGN_IN) {
            // We are coming back from the Google login activity
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            this.handleGoogleSignInResult(result);
        } else {
            // We are coming back from the Google login activity
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }


    //***********************************************************************//
    //                           Public methods                              //
    //***********************************************************************//


    //***********************************************************************//
    //                           Private methods                             //
    //***********************************************************************//


    /**
     * Configures (or hides) the Facebook sign in button
     */
    private void setUpFacebookSignIn() {

        String facebookAppId = getString(R.string.facebook_app_id);

        // The Facebook application ID must be defined in res/values/configuration_strings.xml
        if (facebookAppId.isEmpty()) {
            facebookSignInBtn.setVisibility(View.GONE);
            findViewById(R.id.signin_no_facebook_signin_txt).setVisibility(View.VISIBLE);
            return;
        }

        callbackManager = CallbackManager.Factory.create();

        facebookSignInBtn.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {

            @Override
            public void onSuccess(LoginResult loginResult) {
                SignInActivity.this.handleFacebookLogin(loginResult);
            }


            @Override
            public void onCancel() {
                Log.v(TAG, "User cancelled log in with Facebook");
            }

            @Override
            public void onError(FacebookException error) {
                SignInActivity.this.handleFacebookError(error);
            }
        });
    }

    /**
     * Configures (or hides) the Google sign in button
     */
    private void setUpGoogleSignIn() {

        String serverClientId = getString(R.string.google_server_client_id);

        // The Google server client ID must be defined in res/values/configuration_strings.xml
        if (serverClientId.isEmpty()) {
            googleSignInBtn.setVisibility(View.GONE);
            findViewById(R.id.signin_no_google_signin_txt).setVisibility(View.VISIBLE);
            return;
        }

        // We configure the Google Sign in button
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(serverClientId)
                .build();

        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        googleSignInBtn.setSize(SignInButton.SIZE_WIDE);
        googleSignInBtn.setScopes(gso.getScopeArray());

        googleSignInBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v(TAG, "Signing in with Google...");

                Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
                SignInActivity.this.startActivityForResult(signInIntent, GOOGLE_SIGN_IN);
            }
        });
    }



    /**
     * Handle Google sign in result
     */
    private void handleGoogleSignInResult(GoogleSignInResult result) {

        if (result.isSuccess()) {

            Log.v(TAG, "Successfully logged in with Google...");
            // We can request some Cognito Credentials
            GoogleSignInAccount acct = result.getSignInAccount();
            Map<String, String> logins = new HashMap<>();
            logins.put(GOOGLE_LOGIN, acct.getIdToken());
            Log.v(TAG, String.format("Google token <<<\n%s\n>>>", logins.get(GOOGLE_LOGIN)));

            // The identity must be created asynchronously
            new CreateIdentityTask().execute(logins);

        } else {

            Log.w(TAG, String.format("Failed to authenticate against Google #%d - %s", result.getStatus().getStatusCode(), result.getStatus().getStatusMessage()));
;
        }
    }


    /**
     * Handle a Facebook login error
     * @param error the error that should be handled.
     */
    private void handleFacebookError(FacebookException error) {

        String message = String.format("Failed to authenticate against Facebook %s - \"%s\"", error.getClass().getSimpleName(), error.getLocalizedMessage());
        Log.e(TAG, message, error);

    }

    /**
     * Handle a Facebook login success
     * @param loginResult the successful login result
     */
    private void handleFacebookLogin(LoginResult loginResult) {

        Log.v(TAG, "Successfully logged in with Facebook...");

        final Map<String, String> logins = new HashMap<>();
        logins.put(FACEBOOK_LOGIN, AccessToken.getCurrentAccessToken().getToken());
        Log.v(TAG, String.format("Facebook token <<<\n%s\n>>>", logins.get(FACEBOOK_LOGIN)));

        // The identity must be created asynchronously
        new CreateIdentityTask().execute(logins);

    }

}
