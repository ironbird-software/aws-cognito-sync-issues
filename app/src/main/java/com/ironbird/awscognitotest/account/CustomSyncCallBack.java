package com.ironbird.awscognitotest.account;

import android.util.Log;

import com.amazonaws.mobileconnectors.cognito.Dataset;
import com.amazonaws.mobileconnectors.cognito.DefaultSyncCallback;
import com.amazonaws.mobileconnectors.cognito.Record;
import com.amazonaws.mobileconnectors.cognito.SyncConflict;
import com.amazonaws.mobileconnectors.cognito.exceptions.DataStorageException;

import java.util.List;

/**
 * Project AwsCognitoTest
 * Created by jmc on 14/02/16.
 */
public class CustomSyncCallBack extends DefaultSyncCallback {


    //***********************************************************************//
    //                           Inner classes                               //
    //***********************************************************************//


    //***********************************************************************//
    //                          Class variables                              //
    //***********************************************************************//
    private static final String TAG = CustomSyncCallBack.class.getSimpleName();


    //***********************************************************************//
    //                         Instance variables                            //
    //***********************************************************************//
    AccountManager.SyncObserver observer;


    //***********************************************************************//
    //                            Constructors                               //
    //***********************************************************************//

    public CustomSyncCallBack(AccountManager.SyncObserver observer) {
        this.observer = observer;
    }


    //***********************************************************************//
    //                         Getters and setters                           //
    //***********************************************************************//


    //***********************************************************************//
    //                               Interfaces                              //
    //***********************************************************************//

    /* Implements TheInterface */


    //***********************************************************************//
    //                               Overrides                               //
    //***********************************************************************//


    @Override
    public void onSuccess(Dataset dataset, List<Record> updatedRecords) {
        if (observer != null) {
            observer.onDatasetDidSync(dataset.getDatasetMetadata().getDatasetName());
        }
    }


    @Override
    public boolean onConflict(Dataset dataset, List<SyncConflict> conflicts) {
        boolean r = super.onConflict(dataset, conflicts);
        if (observer != null) {
            observer.onDatasetDidSync(dataset.getDatasetMetadata().getDatasetName());
        }
        return r;
    }


    @Override
    public boolean onDatasetDeleted(Dataset dataset, String datasetName) {
        return false;
    }


    @Override
    public boolean onDatasetsMerged(Dataset dataset, List<String> datasetNames) {
        if (observer != null) {
            observer.onDatasetDidSync(dataset.getDatasetMetadata().getDatasetName());
        }
        return super.onDatasetsMerged(dataset, datasetNames);
    }


    @Override
    public void onFailure(DataStorageException dse) {

        String message = String.format("Got %s - %s while synchronizing dataset...", dse.getClass().getSimpleName(), dse.getMessage());
        Log.e(TAG, message, dse);

        super.onFailure(dse);
    }

    //***********************************************************************//
    //                           Public methods                              //
    //***********************************************************************//


    //***********************************************************************//
    //                           Private methods                             //
    //***********************************************************************//


}
