package com.udacity.stockhawk.sync;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.PersistableBundle;

import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;
import yahoofinance.quotes.stock.StockQuote;

public final class QuoteSyncJob {

    private static final int ONE_OFF_ID = 2;
    public static final String ACTION_REFRESH = "com.udacity.stockhawk.ACTION_REFRESH";
    public static final String ACTION_VALIDATE = "com.udacity.stockhawk.ACTION_VALIDATE";
    public static final String ACTION_DATA_UPDATED = "com.udacity.stockhawk.ACTION_DATA_UPDATED";
    public static final String ACTION_STOCK_VALIDATED = "com.udacity.stockhawk.ACTION_STOCK_VALIDATED";
    public static final String ACTION_STOCK_VALIDATION_FAILED = "com.udacity.stockhawk.ACTION_STOCK_VALIDATION_FAILED";
    public static final String EXTRA_SYMBOL = "com.udacity.stockhawk.EXTRA_SYMBOL";
    public static final String EXTRA_ACTION = "com.udacity.stockhawk.EXTRA_ACTION";

    private static final int PERIOD = 300000;
    private static final int INITIAL_BACKOFF = 10000;
    private static final int PERIODIC_ID = 1;
    private static final int YEARS_OF_HISTORY = 2;

    private QuoteSyncJob() {
    }

    static void validateStockQuote(Context context, String symbol) {
        Timber.d("Running validation job");

        if (symbol == null || symbol.length() == 0)
            return;

        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();
        from.add(Calendar.DATE, -7);

        try {
            String[] array = { symbol };

            Map<String, Stock> quotes = YahooFinance.get(array);

            Stock stock = quotes.get(symbol);
            StockQuote quote = null;
            if (stock != null && (quote = stock.getQuote()) != null && quote.getPrice() != null) {
                float price = quote.getPrice().floatValue();
                float change = quote.getChange().floatValue();
                float percentChange = quote.getChangeInPercent().floatValue();
                // insert the quote and history - will update the cursorloader
                List<HistoricalQuote> history = stock.getHistory(from, to, Interval.WEEKLY);

                StringBuilder historyBuilder = new StringBuilder();

                for (HistoricalQuote it : history) {
                    historyBuilder.append(it.getDate().getTimeInMillis());
                    historyBuilder.append(", ");
                    historyBuilder.append(it.getClose());
                    historyBuilder.append("\n");
                }

                ContentValues quoteCV = new ContentValues();
                quoteCV.put(Contract.Quote.COLUMN_SYMBOL, symbol);
                quoteCV.put(Contract.Quote.COLUMN_PRICE, price);
                quoteCV.put(Contract.Quote.COLUMN_PERCENTAGE_CHANGE, percentChange);
                quoteCV.put(Contract.Quote.COLUMN_ABSOLUTE_CHANGE, change);
                quoteCV.put(Contract.Quote.COLUMN_NAME, stock.getName());

                quoteCV.put(Contract.Quote.COLUMN_HISTORY, historyBuilder.toString());

                context.getContentResolver().insert(Contract.Quote.URI, quoteCV);

                Intent intent = new Intent(ACTION_STOCK_VALIDATED);
                intent.putExtra(EXTRA_SYMBOL, symbol);
                context.sendBroadcast(intent);
            } else {
                PrefUtils.removeStock(context, symbol);
                Intent intent = new Intent(ACTION_STOCK_VALIDATION_FAILED);
                intent.putExtra(EXTRA_SYMBOL, symbol);
                context.sendBroadcast(intent);
            }

        } catch (Exception e) {
            Intent intent = new Intent(ACTION_STOCK_VALIDATION_FAILED);
            intent.putExtra(EXTRA_SYMBOL, symbol);
            context.sendBroadcast(intent);
        }
    }

    static void getQuotes(Context context) {

        Timber.d("Running sync job");

        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();
        from.add(Calendar.YEAR, -YEARS_OF_HISTORY);

        try {

            Set<String> stockPref = PrefUtils.getStocks(context);
            Set<String> stockCopy = new HashSet<>();
            stockCopy.addAll(stockPref);
            String[] stockArray = stockPref.toArray(new String[stockPref.size()]);

            Timber.d(stockCopy.toString());

            if (stockArray.length == 0) {
                return;
            }

            Map<String, Stock> quotes = YahooFinance.get(stockArray);
            Iterator<String> iterator = stockCopy.iterator();

            Timber.d(quotes.toString());

            ArrayList<ContentValues> quoteCVs = new ArrayList<>();

            while (iterator.hasNext()) {
                String symbol = iterator.next();


                Stock stock = quotes.get(symbol);
                StockQuote quote = null;
                if (stock != null && (quote = stock.getQuote()) != null && quote.getPrice() != null) {

                    float price = quote.getPrice().floatValue();
                    float change = quote.getChange().floatValue();
                    float percentChange = quote.getChangeInPercent().floatValue();

                    // WARNING! Don't request historical data for a stock that doesn't exist!
                    // The request will hang forever X_x
                    List<HistoricalQuote> history = stock.getHistory(from, to, Interval.WEEKLY);

                    StringBuilder historyBuilder = new StringBuilder();

                    for (HistoricalQuote it : history) {
                        historyBuilder.append(it.getDate().getTimeInMillis());
                        historyBuilder.append(", ");
                        historyBuilder.append(it.getClose());
                        historyBuilder.append("\n");
                    }

                    ContentValues quoteCV = new ContentValues();
                    quoteCV.put(Contract.Quote.COLUMN_SYMBOL, symbol);
                    quoteCV.put(Contract.Quote.COLUMN_PRICE, price);
                    quoteCV.put(Contract.Quote.COLUMN_PERCENTAGE_CHANGE, percentChange);
                    quoteCV.put(Contract.Quote.COLUMN_ABSOLUTE_CHANGE, change);
                    quoteCV.put(Contract.Quote.COLUMN_NAME, stock.getName());

                    quoteCV.put(Contract.Quote.COLUMN_HISTORY, historyBuilder.toString());

                    quoteCVs.add(quoteCV);
                }
            }

            long updatedTime = System.currentTimeMillis();
            ContentValues updateValues = new ContentValues();
            updateValues.put(Contract.StockUpdated.COLUMN_LAST_UPDATED, updatedTime);
            ContentResolver resolver = context.getContentResolver();
            resolver.bulkInsert(
                            Contract.Quote.URI,
                            quoteCVs.toArray(new ContentValues[quoteCVs.size()]));
            resolver.insert(Contract.StockUpdated.URI, updateValues);

            Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED);
            context.sendBroadcast(dataUpdatedIntent);

        } catch (IOException exception) {
            Timber.e(exception, "Error fetching stock quotes");
        }
    }

    private static void schedulePeriodic(Context context) {
        Timber.d("Scheduling a periodic task");


        JobInfo.Builder builder = new JobInfo.Builder(PERIODIC_ID, new ComponentName(context, QuoteJobService.class));


        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(PERIOD)
                .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);


        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        scheduler.schedule(builder.build());
    }


    public static synchronized void initialize(final Context context) {

        schedulePeriodic(context);
        syncImmediately(context);

    }

    public static synchronized void syncImmediately(Context context) {

        if (isNetworkUp(context)) {
            Intent nowIntent = new Intent(ACTION_REFRESH, null, context, QuoteIntentService.class);
            context.startService(nowIntent);
        } else {

            JobInfo.Builder builder = new JobInfo.Builder(ONE_OFF_ID, new ComponentName(context, QuoteJobService.class));
            PersistableBundle bundle = new PersistableBundle();
            bundle.putString(EXTRA_ACTION, ACTION_REFRESH);

            builder.setExtras(bundle);
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);


            JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

            scheduler.schedule(builder.build());
        }
    }

    public static synchronized void validateSymbol(Context context, String symbol) {
        if (isNetworkUp(context)) {
            Intent nowIntent = new Intent(ACTION_VALIDATE, null, context, QuoteIntentService.class);
            nowIntent.putExtra(EXTRA_SYMBOL, symbol);
            context.startService(nowIntent);
        } else {
            JobInfo.Builder builder = new JobInfo.Builder(ONE_OFF_ID, new ComponentName(context, QuoteJobService.class));
            PersistableBundle bundle = new PersistableBundle();
            bundle.putString(EXTRA_ACTION, ACTION_VALIDATE);
            bundle.putString(EXTRA_SYMBOL, symbol);

            builder.setExtras(bundle);
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);


            JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

            scheduler.schedule(builder.build());
        }
    }

    static boolean isNetworkUp(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }
}
