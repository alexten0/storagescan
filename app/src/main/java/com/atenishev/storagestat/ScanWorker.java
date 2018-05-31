package com.atenishev.storagestat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;

import androidx.work.Data;
import androidx.work.WorkManager;
import androidx.work.Worker;

public class ScanWorker extends Worker {
    private static final String TAG = ScanWorker.class.getSimpleName();

    private static final boolean isNotify = true;

    private ScanData data = new ScanData();

//    https://developer.android.com/training/notify-user/build-notification

    @NonNull
    @Override
    public WorkerResult doWork() {
        Log.d(TAG, "job: before start");
        makeStatusNotification("Started scan", "Scanning external data directory...");
        doStaringWork();
        Log.d(TAG, "job: finilizing");
        Data output = reportOutput();
        setOutputData(output);
        makeStatusNotification("Finished Scan", "Scan completed");
        return WorkerResult.SUCCESS;
    }

    private void doStaringWork() {
        final String state = Environment.getExternalStorageState();
        Log.d(TAG, "state: "+state);
        if ( Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state) ) {  // we can read the External Storage...
//            Log.d(TAG, "Environment.getExternalStorageDirectory(): "+Environment.getExternalStorageDirectory().getAbsolutePath());
            getAllFilesOfDir(Environment.getExternalStorageDirectory());
        }
    }
    private void getAllFilesOfDir(File directory) {
//        Log.d(TAG, "Directory: " + directory.getAbsolutePath());
        final File[] files = directory.listFiles();
        int count = 0;
        if ( files != null ) {
            for ( File file : files ) {
                if ( file != null ) {
                    if ( file.isDirectory() ) {  // it is a folder...
                        getAllFilesOfDir(file);
                    }
                    else {  // it is a file...
//                        Log.d(TAG, "File: " + file.getAbsolutePath());
                        data.process(file.getAbsolutePath(), file.getName(), file.length());
                    }
                }

                if(isStopped()) {
                    break;
                }
                if( isNotify ) {
                    if( count % 20 == 0 ) {
//                        makeStatusNotification("Scanning now", "file: " + file.getName());
                        report();
                    }
                }
                ++count;
            }
        }
    }

    private void report() {
        /*
         * Creates a new Intent containing a Uri object
         * BROADCAST_ACTION is a custom Intent action
         */
        final Intent localIntent =
                new Intent(Constants.BROADCAST_ACTION)
                // Puts the status into the Intent
                .putExtra(Constants.AVERAGE_FSIZE, data.getAverageFileSize())
                ;

        final ScanData.FileInfo[] exts = data.getMostFrequentExtensions();
        for( int i = 0; i < exts.length && exts[i] != null; ++ i ) {
            localIntent.putExtra(Constants.PREF_FREQ_EXT_NAME + i, exts[i].name);
            localIntent.putExtra(Constants.PREF_FREQ_EXT_FREQ + i, exts[i].size);
        }

        final ScanData.FileInfo[] bgFiles = data.getBiggestFiles();
        for( int i = 0; i < bgFiles.length && bgFiles[i] != null; ++ i ) {
            localIntent.putExtra(Constants.PREF_BIG_FILES_NAME + i, bgFiles[i].name);
            localIntent.putExtra(Constants.PREF_BIG_FILES_SIZE + i, bgFiles[i].size);
        }
        // Broadcast the Intent to receivers in this app.
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(localIntent);
    }

    private Data reportOutput() {
        final Data.Builder builder = new Data.Builder();

        builder.putLong(Constants.AVERAGE_FSIZE, data.getAverageFileSize());

        final ScanData.FileInfo[] exts = data.getMostFrequentExtensions();
        for( int i = 0; i < exts.length && exts[i] != null; ++ i ) {
            builder.putString(Constants.PREF_FREQ_EXT_NAME + i, exts[i].name);
            builder.putLong(Constants.PREF_FREQ_EXT_FREQ + i, exts[i].size);
        }

        final ScanData.FileInfo[] bgFiles = data.getBiggestFiles();
        for( int i = 0; i < bgFiles.length && bgFiles[i] != null; ++ i ) {
            builder.putString(Constants.PREF_BIG_FILES_NAME + i, bgFiles[i].name);
            builder.putLong(Constants.PREF_BIG_FILES_SIZE + i, bgFiles[i].size);
        }
        return builder.build();
    }

    private void makeStatusNotification(final String title, final String message) {
        final Context context = getApplicationContext();
        // Make a channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = Constants.VERBOSE_NOTIFICATION_CHANNEL_NAME;
            String description = Constants.VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION;
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel =
                    new NotificationChannel(Constants.CHANNEL_ID, name, importance);
            channel.setDescription(description);

            // Add the channel
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        // Create the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Constants.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVibrate(new long[0]);

        // Show the notification
        NotificationManagerCompat.from(context).notify(Constants.NOTIFICATION_ID, builder.build());
    }

    @Override
    public void onStopped() {
        super.onStopped();
        Log.e(TAG, "job: have to stop");
        setOutputData(new Data.Builder().putString(
                Constants.JOB_DATA, "job stopped").build());

    }
}
