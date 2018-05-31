package com.atenishev.storagestat;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.util.Log;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkStatus;

public class ScanViewModel extends ViewModel {
    private static final String TAG = ScanViewModel.class.getSimpleName();

    private LiveData<List<WorkStatus>> savedWorkStatusList;
    private UUID jobId;

    public ScanViewModel() {
    }
    @Override
    protected void onCleared() {
        super.onCleared();
        // It is useful when ViewModel observes some data
        // and you need to clear this subscription
        // to prevent a leak of this ViewModel.
        Log.d(TAG, "onCleared");
    }

    public UUID startScan() {
        OneTimeWorkRequest scanWork =
                new OneTimeWorkRequest.Builder(ScanWorker.class)
                        .addTag(Constants.JOB_TAG)
                        .setInitialDelay(0, TimeUnit.SECONDS)
//                        .setInputData(myData) // https://developer.android.com/topic/libraries/architecture/workmanager#params
                        .build();
        jobId = scanWork.getId();
        WorkManager.getInstance().enqueue(scanWork);
        return jobId;
    }

    public void stopScan() {
        WorkManager.getInstance().cancelAllWorkByTag(Constants.JOB_TAG);
        jobId = null;
    }

    public UUID getJobId() {
        return jobId;
    }

    public LiveData<List<WorkStatus>> getLiveData() {
        if( savedWorkStatusList == null ) {
            savedWorkStatusList = WorkManager.getInstance().getStatusesByTag(Constants.JOB_TAG);
        }
        return savedWorkStatusList;
    }
}
