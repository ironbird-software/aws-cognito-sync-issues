package com.ironbird.awscognitotest;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.FacebookSdk;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.ironbird.awscognitotest.account.AccountManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, AccountManager.SyncObserver, Constants {


    //***********************************************************************//
    //                           Inner classes                               //
    //***********************************************************************//

    private class RefreshTokenTask extends AsyncTask<Map<String, String>, Integer, Boolean> {

        private Runnable onCompletion;

        public RefreshTokenTask(Runnable onCompletion) {
            this.onCompletion = onCompletion;
        }

        @Override
        protected Boolean doInBackground(Map<String, String>... logins) {

            AccountManager.getInstance().refreshCredentials(logins[0]);
            return Boolean.TRUE;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if (onCompletion != null) {
                onCompletion.run();
            }
        }
    }



    //***********************************************************************//
    //                          Class variables                              //
    //***********************************************************************//

    private static final String UNSET_AWS_IDENTITY = "region:identity_pool";

    private static final String TAG = MainActivity.class.getSimpleName();

    private static Context applicationContext;


    //***********************************************************************//
    //                         Instance variables                            //
    //***********************************************************************//

    private TextView expirationTxt;
    private TextView whenTxt;
    private TextView phoneTxt;


    //***********************************************************************//
    //                            Constructors                               //
    //***********************************************************************//


    //***********************************************************************//
    //                         Getters and setters                           //
    //***********************************************************************//


    //***********************************************************************//
    //                               Interfaces                              //
    //***********************************************************************//

    /* Implements AccountManager.SyncObserver */
    /**
     * Called when a dataset in the account manager changes
     * @param dataset the name of teh dataset that did change
     */
    @Override
    public void onDatasetDidSync(String dataset) {
        this.runOnUiThread( new Runnable() {
            @Override
            public void run() {
                MainActivity.this.updateUI();
            }
        });
    }

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
        setContentView(R.layout.activity_main);

        applicationContext = this.getApplicationContext();

        Log.v(TAG , "Configuration");
        Log.v(TAG , "  > Amazon Identity Pool:          " + getString(R.string.aws_identity_pool));
        Log.v(TAG , "  > Google Service ID:             " + getString(R.string.google_server_client_id));
        Log.v(TAG , "  > Facebook Application ID:       " + getString(R.string.facebook_app_id));

        // Need to initialize the Facebook SDK
        FacebookSdk.sdkInitialize(this);

        // Widgets
        expirationTxt = (TextView) this.findViewById(R.id.main_session_expiration_txt);
        whenTxt = (TextView) this.findViewById(R.id.main_when_txt);
        phoneTxt = (TextView) this.findViewById(R.id.main_phone_txt);

        AccountManager.getInstance().setSyncObserver(this);

        Button putValuesBtn = (Button) this.findViewById(R.id.main_put_values_btn);
        putValuesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.this.putValues();
            }
        });

        Button refreshValuesBtn = (Button) this.findViewById(R.id.main_refresh_values_btn);
        refreshValuesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.this.refreshValues();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (UNSET_AWS_IDENTITY.equals(getString(R.string.aws_identity_pool))) {
            findViewById(R.id.main_aws_pool_not_defined_txt).setVisibility(View.VISIBLE);
            return;
        }

        if (AccountManager.getInstance().hasCachedIdentity()) {
            this.updateUI();
        } else {

            // We do not have a cached identity, the user never identified themselves...
            // We go to the sign in activity.

            Intent intent = new Intent(this, SignInActivity.class);
            this.startActivity(intent);
        }
    }



    //***********************************************************************//
    //                           Public methods                              //
    //***********************************************************************//

    /**
     * Return the shared context for those unlucky classes that cannot easily access one.
     * @return the shared context
     */
    public static Context getStaticApplicationContext() {
        return applicationContext;
    }



    //***********************************************************************//
    //                           Private methods                             //
    //***********************************************************************//


    /**
     * Makes sure that we have valid session and push a new set of values to the remote storage.
     * The UI will be updated via a call to onDatasetDidSync (as we are SyncObserver of teh account manager)
     */
    private void putValues() {

        if (!AccountManager.getInstance().isSessionExpired()) {
            // The session is not expired, we can put values directly
            AccountManager.getInstance().putValues();
            return;
        }

        //The session has expired, we need to refresh credentials (based on provider) and call refreshValues on completion
        this.refreshCredentialsAndRun(new Runnable() {

            @Override
            public void run() {
                Log.v(TAG, "Putting values after Cognito credentials refresh...");
                AccountManager.getInstance().putValues();
            }
        });


    }


    /**
     * Makes sure that we have valid session and synchronize values from the remote storage.
     * The UI will be updated via a call to onDatasetDidSync (as we are SyncObserver of teh account manager)
     */
    private void refreshValues() {

        if (!AccountManager.getInstance().isSessionExpired()) {
            // The session is not expired, we can refresh values directly
            AccountManager.getInstance().refreshValues();
            return;
        }

        //The session has expired, we need to refresh credentials (based on provider) and call refreshValues on completion
        this.refreshCredentialsAndRun(new Runnable() {

            @Override
            public void run() {
                Log.v(TAG, "Refreshing values after Cognito credentials refresh...");
                AccountManager.getInstance().refreshValues();
            }
        });

    }


    /**
     * Refresh the Cognito credentials based on the last used provider and run onCompletion once valid credentials have been obtained
     * @param onCompletion a runnable that will be executed once the credentials have been successfully renewed
     */
    private void refreshCredentialsAndRun(Runnable onCompletion) {

        SharedPreferences settings = getSharedPreferences(PREF_FILE, Activity.MODE_PRIVATE);
        String lastUsedProvider = settings.getString(KEY_LAST_USED_PROVIDER, null);

        // The below might produce an NPE, but should not. Left as is on purpose (would highlight a flow issue)
        if (lastUsedProvider.equals(GOOGLE_LOGIN)) {
            this.refreshGoogleCredentials(onCompletion);
        } else if (lastUsedProvider.equals(FACEBOOK_LOGIN)) {
            this.refreshFacebookCredentials(onCompletion);
        } else {
            Log.e(TAG, "Unknown previous identity provider " + lastUsedProvider);
        }
    }


    /**
     * Refresh cognito credentials based on identity token previously obtained from Facebook
     * @param onCompletion a runnable that will be executed once the credentials have been successfully renewed
     */
    private void refreshFacebookCredentials(Runnable onCompletion) {

        Log.v(TAG, "Will try to refresh Facebook token as session has expired...");

        final AccessToken accessToken = AccessToken.getCurrentAccessToken();

        if (!accessToken.isExpired()) {

            Log.v(TAG, String.format("  -> Facebook token is not expired (expiration %s)...", SimpleDateFormat.getInstance().format(accessToken.getExpires())));
            String token = accessToken.getToken();

            Map<String, String> logins = new HashMap<>();
            logins.put(FACEBOOK_LOGIN, token);

            Log.v(TAG, "  -> will refresh login for " + FACEBOOK_LOGIN);

            // Refreshing session credentials (must be asynchronous)...
            new RefreshTokenTask(onCompletion).execute(logins);

            // We use the opportunity to refresh the Facebook token
            Date now = new Date();
            if (accessToken.getLastRefresh().getTime() < (now.getTime() - 7 * 86400)) {
                Log.v(TAG, "  -> triggering async facebook token refresh");
                AccessToken.refreshCurrentAccessTokenAsync();
            }

        } else {

            Log.v(TAG, String.format("  -> Facebook token is expired (expiration %s)...", SimpleDateFormat.getInstance().format(accessToken.getExpires())));
            //User has to sign in to refresh an elapsed facebook token
            // TODO onCompletion is ignored (should be dealt with in onActivityResult)
            Intent intent = new Intent(this, SignInActivity.class);
            this.startActivity(intent);

        }
    }


    /**
     * Refresh cognito credentials based on identity token previously obtained from Google
     * @param onCompletion a runnable that will be executed once the credentials have been successfully renewed
     */
    private void refreshGoogleCredentials(final Runnable onCompletion) {
        Log.v(TAG, "Will try to refresh Google token as session has expired...");

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.google_server_client_id))
                .build();

        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .build();

        final OptionalPendingResult<GoogleSignInResult> pendingResult = Auth.GoogleSignInApi.silentSignIn(googleApiClient);

        if (pendingResult.isDone()) {
            Log.v(TAG, "  -> got immediate result...");
            // There's immediate result available.
            GoogleSignInResult result = pendingResult.get();
            GoogleSignInAccount acct = result.getSignInAccount();
            String token = acct.getIdToken();
            Map<String, String> logins = new HashMap<>();
            logins.put(GOOGLE_LOGIN, token);

            Log.v(TAG, "  -> will refresh login for " + GOOGLE_LOGIN);

            // Refreshing session credentials (must be asynchronous)...
            new RefreshTokenTask(onCompletion).execute(logins);

        } else {

            // There's no immediate result ready, displays some progress indicator and waits for the
            // async callback.
            Log.v(TAG, "  -> got NO immediate result...");

            this.setProgressBarIndeterminateVisibility(true);
            pendingResult.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(@NonNull GoogleSignInResult result) {

                    if (result.isSuccess()) {

                        Log.v(TAG, "  -> got successful PENDING result...");
                        GoogleSignInAccount acct = result.getSignInAccount();
                        String token = acct.getIdToken();
                        Map<String, String> logins = new HashMap<>();
                        logins.put(GOOGLE_LOGIN, token);

                        Log.v(TAG, "  -> will refresh login for " + GOOGLE_LOGIN);

                        new RefreshTokenTask(onCompletion).execute(logins);

                    } else {
                        Log.v(TAG, "  -> got failed PENDING result: redirecting user to sign in...");
                        // It didn't work, we show the sign in, but things will get messy...
                        // TODO onCompletion is ignored (should be dealt with in onActivityResult)
                        Intent intent = new Intent(MainActivity.this, SignInActivity.class);
                        MainActivity.this.startActivity(intent);                    }
                }
            });
        }
    }


    /**
     * Updates the UI according to the current state.
     */
    private void updateUI() {

        // The credentials expiration
        Date expirationDate = AccountManager.getInstance().getSessionExpiration();
        Date now = new Date();
        String expiration;

        if (expirationDate != null) {
            expiration = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(expirationDate);

            if (now.after(expirationDate)) {
                expirationTxt.setTextColor(getResources().getColor(R.color.crimson));
            } else {
                expirationTxt.setTextColor(getResources().getColor(R.color.darkgreen));
            }

        } else {
            expiration = this.getString(R.string.unset);
        }

        expirationTxt.setText(expiration);

        phoneTxt.setText(AccountManager.getInstance().getPhone());
        whenTxt.setText(AccountManager.getInstance().getWhen());

    }

}
