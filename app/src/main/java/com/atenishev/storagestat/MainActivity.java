package com.atenishev.storagestat;

import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.util.UUID;

import androidx.work.Data;
import androidx.work.WorkManager;
import androidx.work.WorkStatus;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    public static final String TAG = MainActivity.class.getSimpleName();

    private View btnStart;
    private View btnStop;
    private ShareActionProvider actionProvider;
    private ScanViewModel viewModel;
    private ProgressBar scanProgress;
    private TextView textProgress;
    private ScanProgressReceiver receiver;

    // decor
    private ForegroundColorSpan fgcsCaption;
    private ForegroundColorSpan fgcsFirst;
    private ForegroundColorSpan fgcsSecond;

    private UUID currentJobId;
    private Bundle jobDetails;

    private Intent shareIntent;
    private MenuItem menuItemShare;

    // Broadcast receiver for receiving status updates from the job running
    private class ScanProgressReceiver extends BroadcastReceiver
    {
        // Prevents instantiation from outside of the parent class
        private ScanProgressReceiver() {
        }
        // Called when the BroadcastReceiver gets an Intent it's registered to receive
        @Override
        public void onReceive(Context context, Intent intent) {
            jobDetails = intent.getExtras();
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showJobDetails();
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        btnStart = findViewById(R.id.btnStart);
        btnStart.setOnClickListener(this);

        btnStop = findViewById(R.id.btnStop);
        btnStop.setOnClickListener(this);
        btnStop.setEnabled(false);

        scanProgress = findViewById(R.id.scanProgress);
        textProgress = findViewById(R.id.textProgress);

        fgcsCaption = new ForegroundColorSpan(getResources().getColor(android.R.color.background_dark));
        fgcsFirst = new ForegroundColorSpan(getResources().getColor(android.R.color.holo_red_dark));
        fgcsSecond = new ForegroundColorSpan(getResources().getColor(android.R.color.holo_blue_dark));

        // Get the ViewModel
        viewModel = ViewModelProviders.of(this).get(ScanViewModel.class);

        receiver = new ScanProgressReceiver();

        showWorkFinished();
    }

    private void formatString(final SpannableStringBuilder ssb, final String text, final ForegroundColorSpan fgcs) {
        final int startPos = ssb.length();
        ssb.append(text);
        ssb.setSpan(fgcs,startPos, ssb.length(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);//SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menuItemShare = menu.findItem(R.id.menu_item_share);
        actionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItemShare);


        shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        final String text = textProgress.getText().toString();
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        shareIntent.setType("text/plain");
        setShareIntent(shareIntent);

        return true;
    }

    private void setShareIntent(Intent shareIntent) {
        if (actionProvider != null) {
            actionProvider.setShareIntent(shareIntent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.e(TAG, "onStart");

        // The filter's action is BROADCAST_ACTION
        IntentFilter statusIntentFilter = new IntentFilter(
                Constants.BROADCAST_ACTION);
        // Registers the DownloadStateReceiver and its intent filters
        LocalBroadcastManager.getInstance(this).registerReceiver(
                receiver,
                statusIntentFilter);
    }

    @Override
    protected void onStop() {
        Log.e(TAG, "onStop");
        if( receiver != null ) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, TAG+".onDestroy");
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        switch( view.getId() ) {
            case R.id.btnStart:
                btnStart.setEnabled(false);
                currentJobId = viewModel.startScan();
                WorkManager.getInstance().getStatusById(currentJobId)
                    .observe(this, status -> {
                        if (status != null) {
                            boolean finished = status.getState().isFinished();
                            if (!finished) {
                                showWorkInProgress();
                            } else {
                                showWorkFinished();
                            }
                        }
                    });
                btnStop.setEnabled(true);
                break;
            case R.id.btnStop:
                btnStop.setEnabled(false);
                viewModel.stopScan();
                btnStart.setEnabled(true);
                break;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.e(TAG, "--- on back pressed");
        viewModel.stopScan();
    }

    /**
     * Shows and hides views for when the Activity is processing an image
     */
    private void showWorkInProgress() {
        Log.e(TAG, "showWorkInProgress");
        btnStart.setEnabled(false);
        btnStop.setEnabled(true);
        scanProgress.setVisibility(View.VISIBLE);
        if( menuItemShare != null ) {
            menuItemShare.setEnabled(false);
        }
    }

    /**
     * Shows and hides views for when the Activity is done processing an image
     */
    private void showWorkFinished() {
        Log.e(TAG, "showWorkFinished");
        btnStart.setEnabled(true);
        scanProgress.setVisibility(View.INVISIBLE);
        btnStop.setEnabled(false);
        if( menuItemShare != null ) {
            menuItemShare.setEnabled(true);
        }
    }

    private void showOutputData(final WorkStatus workStatus) {
        Log.e(TAG, "showOutputData");
        final Data outputData = workStatus.getOutputData();

        final String status = outputData.getString(Constants.JOB_TAG, null);

        textProgress.setText(status != null ? status : "N/A");

        SpannableStringBuilder ssb = new SpannableStringBuilder();

        final Long avFS = outputData.getLong(Constants.AVERAGE_FSIZE, -1);
        if( avFS != -1 ) {
            formatString(ssb, "Average data\n", fgcsCaption);
            formatString(ssb, "Average File Size:  ", fgcsFirst);
            formatString(ssb, "" + avFS, fgcsSecond);
            ssb.append("\n\n");

            formatString(ssb, "Biggest files\n", fgcsCaption);
            for( int i = 0; i < Constants.BIGGEST_FILES_NUM; ++i ) {
                final String name = outputData.getString(Constants.PREF_BIG_FILES_NAME + i, null);
                if( name == null ) {
                    break;
                }
                final long size = outputData.getLong(Constants.PREF_BIG_FILES_SIZE+i, 0);
                ssb.append("Biggest File "+i+": "+ name+ " of size: " + size + "\n");
                formatString(ssb, name + "    ", fgcsFirst);
                formatString(ssb, "" + size + "\n", fgcsSecond);
            }
            ssb.append("\n");
            formatString(ssb, "Most Frequent Extensions\n", fgcsCaption);
            for( int i = 0; i < Constants.FREQ_EXTS_NUM; ++i ) {
                final String ext = outputData.getString(Constants.PREF_FREQ_EXT_NAME + i, null);
                if( ext == null ) {
                    break;
                }
                final long freq = outputData.getLong(Constants.PREF_FREQ_EXT_FREQ+i, 0);
                formatString(ssb, ""+ (i + 1) +". " + ext + "    ", fgcsFirst);
                formatString(ssb, "" + freq + "\n", fgcsSecond);
            }
        }

        textProgress.setText(ssb, TextView.BufferType.EDITABLE);
    }

    private void showJobDetails() {
        if( jobDetails == null ) {
            return;
        }
        final String status = jobDetails.getString(Constants.EXTENDED_DATA_STATUS);

//        final StringBuilder sb = new StringBuilder();
//        Long avFS = extras.getLong(Constants.AVERAGE_FSIZE);
//        sb.append("Average File Size: "+ avFS+"\n");
//        for( int i = 0; i < Constants.BIGGEST_FILES_NUM; ++i ) {
//            final String name = extras.getString(Constants.PREF_BIG_FILES_NAME + i);
//            if( name == null ) {
//                break;
//            }
//            final long size = extras.getLong(Constants.PREF_BIG_FILES_SIZE+i, 0);
//            sb.append("Biggest File "+i+": "+ name+ " of size: " + size + "\n");
//        }
//
//        for( int i = 0; i < Constants.FREQ_EXTS_NUM; ++i ) {
//            final String ext = extras.getString(Constants.PREF_FREQ_EXT_NAME + i);
//            if( ext == null ) {
//                break;
//            }
//            final long freq = extras.getLong(Constants.PREF_FREQ_EXT_FREQ+i, 0);
//            sb.append("Frequent extension "+i+": "+ ext+" met " + freq + " times\n");
//        }
//
//        final String status = outputData.getString(Constants.JOB_TAG, null);

        textProgress.setText(status != null ? status : "N/A");

        SpannableStringBuilder ssb = new SpannableStringBuilder();

        final Long avFS = jobDetails.getLong(Constants.AVERAGE_FSIZE, -1);
        if( avFS != -1 ) {
            formatString(ssb, "Average data\n", fgcsCaption);
            formatString(ssb, "Average File Size:  ", fgcsFirst);
            formatString(ssb, "" + avFS, fgcsSecond);
            ssb.append("\n\n");

            formatString(ssb, "Biggest files\n", fgcsCaption);
            for( int i = 0; i < Constants.BIGGEST_FILES_NUM; ++i ) {
                final String name = jobDetails.getString(Constants.PREF_BIG_FILES_NAME + i, null);
                if( name == null ) {
                    break;
                }
                final long size = jobDetails.getLong(Constants.PREF_BIG_FILES_SIZE+i, 0);
                final int lastPath = name.lastIndexOf(File.separator);
                if( lastPath != -1 ) {
                    String shortname = name.substring(lastPath + 1);
                    formatString(ssb, shortname + "    ", fgcsFirst);
                } else {
//                ssb.append("Biggest File "+i+": "+ name+ " of size: " + size + "\n");
                    formatString(ssb, name + "    ", fgcsFirst);
                }
                formatString(ssb, "" + size + "\n", fgcsSecond);
            }
            ssb.append("\n");
            formatString(ssb, "Most Frequent Extensions\n", fgcsCaption);
            for( int i = 0; i < Constants.FREQ_EXTS_NUM; ++i ) {
                final String ext = jobDetails.getString(Constants.PREF_FREQ_EXT_NAME + i, null);
                if( ext == null ) {
                    break;
                }
                final long freq = jobDetails.getLong(Constants.PREF_FREQ_EXT_FREQ+i, 0);
                formatString(ssb, ""+ (i + 1) +". " + ext + "    ", fgcsFirst);
                formatString(ssb, "" + freq + "\n", fgcsSecond);
            }
        }

        textProgress.setText(ssb, TextView.BufferType.EDITABLE);
        final String text = textProgress.getText().toString();
        if( shareIntent != null ) {
            shareIntent.putExtra(Intent.EXTRA_TEXT, text);
            setShareIntent(shareIntent);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if( jobDetails != null ) {
            outState.putBundle("job_bundle", jobDetails);
        }
        if( currentJobId != null ) {
            outState.putString("job_id", currentJobId.toString());
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        jobDetails = savedInstanceState.getBundle("job_bundle");
        final String savedJobIdStr = savedInstanceState.getString("job_id");
        if( savedJobIdStr != null ) {
            currentJobId = UUID.fromString(savedJobIdStr);
        }
        if( currentJobId != null ) {
            WorkManager.getInstance().getStatusById(currentJobId)
                    .observe(this, status -> {
                        if (status != null) {
                            boolean finished = status.getState().isFinished();
                            if (!finished) {
                                showWorkInProgress();
                            } else {
                                showWorkFinished();
                            }
                        }
                    });
        }
        showJobDetails();
    }
}
