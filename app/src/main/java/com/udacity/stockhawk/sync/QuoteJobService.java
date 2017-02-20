package com.udacity.stockhawk.sync;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;

import timber.log.Timber;

public class QuoteJobService extends JobService {


    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Timber.d("Intent handled");
        String action = jobParameters.getExtras().getString(QuoteSyncJob.EXTRA_ACTION);
        if (action == null)
            action = QuoteSyncJob.ACTION_REFRESH;
        Intent nowIntent = new Intent(action, null, getApplicationContext(), QuoteIntentService.class);
        if (QuoteSyncJob.ACTION_VALIDATE.equals(action)) {
            String symbol = jobParameters.getExtras().getString(QuoteSyncJob.EXTRA_SYMBOL);
            nowIntent.putExtra(QuoteSyncJob.EXTRA_SYMBOL, symbol);
        }
        getApplicationContext().startService(nowIntent);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }


}
