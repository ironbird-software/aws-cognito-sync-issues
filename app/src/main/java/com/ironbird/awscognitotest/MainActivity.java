package com.ironbird.awscognitotest;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.facebook.FacebookSdk;
import com.ironbird.awscognitotest.account.AccountManager;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements AccountManager.SyncObserver{

    //***********************************************************************//
    //                           Inner classes                               //
    //***********************************************************************//


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
                AccountManager.getInstance().putValues();
            }
        });

        Button refreshValuesBtn = (Button) this.findViewById(R.id.main_refresh_values_btn);
        refreshValuesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AccountManager.getInstance().refreshValues();
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
