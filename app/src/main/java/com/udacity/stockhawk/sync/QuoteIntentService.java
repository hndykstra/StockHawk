package com.udacity.stockhawk.sync;

import android.app.IntentService;
import android.content.Intent;

import timber.log.Timber;


public class QuoteIntentService extends IntentService {

    public QuoteIntentService() {
        super(QuoteIntentService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Timber.d("Intent handled");
        String action = intent.getAction();
        if (action.equals(QuoteSyncJob.ACTION_REFRESH)) {
            QuoteSyncJob.getQuotes(getApplicationContext());
        } else if (action.equals(QuoteSyncJob.ACTION_VALIDATE)) {
            String symbol = intent.getStringExtra(QuoteSyncJob.EXTRA_SYMBOL);
            QuoteSyncJob.validateStockQuote(getApplicationContext(), symbol);
        }
    }
}
