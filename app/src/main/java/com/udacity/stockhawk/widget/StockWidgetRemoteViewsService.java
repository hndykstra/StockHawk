package com.udacity.stockhawk.widget;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Binder;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Created by hans.dykstra on 2/13/2017.
 */

public class StockWidgetRemoteViewsService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        final DecimalFormat dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        final DecimalFormat dollarFormatWithPlus = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
        dollarFormatWithPlus.setPositivePrefix(StockWidgetRemoteViewsService.this.getString(R.string.dollar_format_plus_pfx));
        final DecimalFormat pctFormat = (DecimalFormat) NumberFormat.getPercentInstance(Locale.getDefault());
        pctFormat.setMaximumFractionDigits(2);
        pctFormat.setMinimumFractionDigits(2);
        pctFormat.setPositivePrefix(StockWidgetRemoteViewsService.this.getString(R.string.pct_format_plus_pfx));

        return new RemoteViewsFactory() {
            private Cursor stockCursor = null;

            @Override
            public void onCreate() {

            }

            @Override
            public void onDataSetChanged() {
                Log.d(getClass().getSimpleName(), "onDataSetChanged");
                if (stockCursor != null) {
                    stockCursor.close();
                }

                final long identityToken = Binder.clearCallingIdentity();
                Uri quotesUri = Contract.Quote.URI;
                stockCursor = getContentResolver().query(quotesUri,
                        Contract.Quote.QUOTE_COLUMNS.toArray(new String[Contract.Quote.QUOTE_COLUMNS.size()]),
                        null,
                        null,
                        Contract.Quote.COLUMN_SYMBOL);
                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (stockCursor != null) {
                    stockCursor.close();
                    stockCursor = null;
                }
            }

            @Override
            public int getCount() {
                return (stockCursor == null ? 0 : stockCursor.getCount());
            }

            @Override
            public RemoteViews getViewAt(int position) {
                Log.d(StockWidgetRemoteViewsService.class.getSimpleName(), "getViewAt");
                if (position == AdapterView.INVALID_POSITION || stockCursor == null || !stockCursor.moveToPosition(position)) {
                    return null;
                }
                RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget_list_item);

                String symbol = stockCursor.getString(Contract.Quote.POSITION_SYMBOL);
                float price = stockCursor.getFloat(Contract.Quote.POSITION_PRICE);
                String mode = PrefUtils.getDisplayMode(StockWidgetRemoteViewsService.this);
                String pctMode = getString(R.string.pref_display_mode_percentage_key);
                boolean isPercent = mode.equals(pctMode);
                float changeAbs = stockCursor.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE);
                float changePct = stockCursor.getFloat(Contract.Quote.POSITION_PERCENTAGE_CHANGE);

                if (changeAbs < 0.f) {
                    views.setTextColor(R.id.change, Color.RED);
                } else {
                    views.setTextColor(R.id.change, Color.GREEN);
                }
                views.setTextViewText(R.id.symbol, symbol);
                views.setTextViewText(R.id.price, dollarFormat.format(price));
                if (isPercent) {
                    views.setTextViewText(R.id.change, pctFormat.format(changePct / 100.f));
                } else {
                    views.setTextViewText(R.id.change, dollarFormatWithPlus.format(changeAbs));
                }

                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_list_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (stockCursor.moveToPosition(position))
                    return stockCursor.getLong(Contract.Quote.POSITION_ID);
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
