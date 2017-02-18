package com.udacity.stockhawk.ui;

import android.annotation.TargetApi;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by hans.dykstra on 2/12/2017.
 */

public class StockHistoryActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final int HISTORY_LOADER = 12908;
    private static final int MAX_HISTORY_WEEKS = 12;

    private String quoteSymbol;

    @BindView(R.id.detail_chart)
    LineChart historyChart;
    @BindView(R.id.stock_symbol)
    TextView symbol;
    @BindView(R.id.stock_name)
    TextView stockName;
    @BindView(R.id.updated_date)
    TextView updateDate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stock_detail);
        ButterKnife.bind(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        quoteSymbol = getIntent().getStringExtra(MainActivity.EXTRA_SYMBOL);
        getSupportLoaderManager().initLoader(HISTORY_LOADER, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == HISTORY_LOADER) {
            return new CursorLoader(this, Contract.Quote.URI, Contract.Quote.QUOTE_COLUMNS.toArray(new String[Contract.Quote.QUOTE_COLUMNS.size()]),
                    Contract.Quote.COLUMN_SYMBOL + "=?", new String[]{quoteSymbol}, null);
        } else {
            return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        bindHistory(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private void bindHistory(Cursor data) {
        if (data.moveToFirst()) {
            String tickerSymbol = data.getString(Contract.Quote.POSITION_SYMBOL);
            String name = data.getString(Contract.Quote.POSITION_NAME);
            this.symbol.setText(tickerSymbol);
            this.stockName.setText(name);
            DateFormat fmt = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
            this.updateDate.setText(fmt.format(new Date()));

            String history = data.getString(Contract.Quote.POSITION_HISTORY);
            String[] historyLines = history.split("\n");
            int length = Math.min(MAX_HISTORY_WEEKS, historyLines.length);
            // history is most recent first
            if (length > 0) {
                ArrayList<Entry> entries = new ArrayList<>(length);
                float min = -1f;
                float max = -1f;
                for (int i = length - 1; i >= 0; --i) {
                    String historyLine = historyLines[i];
                    String[] historyFields = historyLine.split(", ");
                    float dateVal = Float.valueOf(historyFields[0]);
                    float close = Float.valueOf(historyFields[1]);
                    if (min < 0.f || close < min) {
                        min = close;
                    }
                    if (max < 0.f || close > max) {
                        max = close;
                    }
                    entries.add(new Entry(dateVal, close));
                }
                historyChart.setTouchEnabled(false);
                historyChart.getDescription().setEnabled(false);
                historyChart.setDragEnabled(false);
                historyChart.setScaleEnabled(false);
                historyChart.setPinchZoom(false);

                final DateFormat format = DateFormat.getDateInstance(DateFormat.SHORT);
                XAxis xaxis = historyChart.getXAxis();
                xaxis.setLabelCount(4);
                xaxis.setTextColor(Color.WHITE);
                xaxis.setAxisLineColor(Color.WHITE);
                xaxis.setValueFormatter(new IAxisValueFormatter() {
                    @Override
                    public String getFormattedValue(float value, AxisBase axis) {
                        long dateVal = (long)value;
                        Date asDate = new Date(dateVal);
                        return format.format(asDate);
                    }
                });


                YAxis yaxis = historyChart.getAxisLeft();
                yaxis.setAxisMinimum((float)Math.floor(min));
                yaxis.setAxisMaximum((float)Math.ceil(max));
                yaxis.setTextColor(Color.WHITE);
                yaxis.setAxisLineColor(Color.WHITE);
                yaxis.setLabelCount(3);

                LineDataSet set = new LineDataSet(entries, getString(R.string.history_date_set));
                set.setLineWidth(1f);
                set.setValueTextSize(9f);
                set.setCircleRadius(3f);
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                    setGraphColors(historyChart, set);
                }

                ArrayList<ILineDataSet> sets = new ArrayList<>();
                sets.add(set);
                LineData graphData = new LineData(sets);
                historyChart.setData(graphData);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void setGraphColors(LineChart chart, LineDataSet set) {
        int clr = getColor(R.color.colorAccent);
        set.setColor(clr);
        set.setCircleColor(clr);

        XAxis xaxis = chart.getXAxis();
        YAxis yaxis = chart.getAxisLeft();
        int lineColor = getColor(R.color.gridColor);
        xaxis.setAxisLineColor(lineColor);
        xaxis.setTextColor(lineColor);
        yaxis.setAxisLineColor(lineColor);
        yaxis.setTextColor(lineColor);
    }
}
