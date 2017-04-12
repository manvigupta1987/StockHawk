package com.udacity.stockhawk.ui;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.EntryXComparator;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.StockHistoryData;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.sync.QuoteSyncJob;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

public class DetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<HistoricalQuote>>{
    private final static String TAG = DetailActivity.class.getSimpleName();

    @BindView(R.id.chart) LineChart mLineChart;
    @BindView(R.id.tvXMax) TextView mLabelX;
    @BindView(R.id.tvYMax) TextView mLabelY;
    private static final String[] projection = {Contract.Quote.COLUMN_HISTORY};
    private static final int YEARS_OF_HISTORY = 2;
    private static final int DETAIL_LOADER_ID = 5000;

    private final static int INDEX_HISTORY_ID =0;
    private List<HistoricalQuote> mHistoryData;
    private XAxis xAxis;
    private YAxis leftAxis;
    private List<Entry> mEnteries;
    private ArrayList<String> DateInMilis;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        ButterKnife.bind(this);

        String symbol;
        Intent intent = getIntent();
        if(intent.hasExtra(QuoteSyncJob.STOCK_NAME))
        {
            symbol = intent.getStringExtra(QuoteSyncJob.STOCK_NAME);
            Bundle bundle = new Bundle();
            bundle.putString(QuoteSyncJob.STOCK_NAME,symbol);
            mLineChart.setDrawGridBackground(false);
            mLineChart.getDescription().setEnabled(true);

            getSupportLoaderManager().initLoader(DETAIL_LOADER_ID, bundle,DetailActivity.this);



            xAxis = mLineChart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setTextSize(15f);
            xAxis.setTextColor(Color.WHITE);
            xAxis.setDrawAxisLine(true);
            xAxis.setTextColor(Color.rgb(255, 192, 56));
            //xAxis.setCenterAxisLabels(true);
            xAxis.setLabelCount(3);
            xAxis.setGranularity(1f);


            leftAxis = mLineChart.getAxisLeft();
            leftAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
            leftAxis.setTextColor(ColorTemplate.getHoloBlue());
            leftAxis.setDrawGridLines(true);
            leftAxis.setGranularityEnabled(true);
            leftAxis.setAxisMinimum(0f);
            leftAxis.setAxisMaximum(1000f);
            leftAxis.setYOffset(-9f);
            leftAxis.setTextColor(Color.rgb(255, 192, 56));
            leftAxis.setTextSize(15);

            YAxis rightAxis = mLineChart.getAxisRight();
            rightAxis.setEnabled(false);
        }
    }


    @Override
    public Loader<List<HistoricalQuote>> onCreateLoader(int id, final Bundle args) {
        return new AsyncTaskLoader<List<HistoricalQuote>>(this) {

            @Override
            protected void onStartLoading() {
                super.onStartLoading();
                forceLoad();
            }

            @Override
            public List<HistoricalQuote> loadInBackground() {
                String symbol = args.getString(QuoteSyncJob.STOCK_NAME);
                try {
                    Calendar from = Calendar.getInstance();
                    Calendar to = Calendar.getInstance();
                    from.add(Calendar.YEAR, -YEARS_OF_HISTORY);
                    Stock stock = YahooFinance.get(symbol);
                    if (stock.isValid()) {
                        mHistoryData = stock.getHistory(from, to, Interval.WEEKLY);
                        return mHistoryData;
                    }
                    else {
                        return null;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<List<HistoricalQuote>> loader, List<HistoricalQuote> data) {
        Log.e(TAG, "onLoadFinished" +data.get(0).getDate().toString());
        mEnteries = new ArrayList<Entry>();
        DateInMilis = new ArrayList<String>();
        float min = 1000;
        float max = 0;
        for(HistoricalQuote history: data)
        {
            float date = history.getDate().getTimeInMillis();
            DateInMilis.add(changeDateFormat(date));
            float closingDate = history.getClose().floatValue();
            if(closingDate < min)
            {
                min = closingDate;
            }
            if(closingDate > max)
            {
                max = closingDate;
            }
            mEnteries.add(new Entry(date,closingDate));
        }
        //this library does not officially support drawing LineChart data from an Entry list not sorted by the x-position
        // of the entries in ascending manner. Adding entries in an unsorted way may result in correct drawing, but may
        // also lead to unexpected behaviour. A List of Entry objects can be sorted manually or using the EntryXComparator:
        Collections.sort(mEnteries,new EntryXComparator());

        leftAxis.setAxisMinimum(min);
        leftAxis.setAxisMaximum(max);
        xAxis.setValueFormatter(new IAxisValueFormatter() {

            private SimpleDateFormat mFormat = new SimpleDateFormat("dd MMM yyyy");

            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                Log.e(TAG, "getFormattedValue"+ (long)value);

                long millis = TimeUnit.HOURS.toMillis((long) value);
                return mFormat.format(new Date(millis));
            }
        });
        //xAxis.setValueFormatter(new MyXAxisValueFormatter(DateInMilis));

//        xAxis.setValueFormatter(new IAxisValueFormatter() {
//            @Override
//            public String getFormattedValue(float value, AxisBase axis) {
//                return (DateInMilis.get((int)value));
//            }
//        });

        LineDataSet lineDataSet = new LineDataSet(mEnteries,"DataSet");
        lineDataSet.setColor(ColorTemplate.getHoloBlue());
        lineDataSet.setValueTextColor(ColorTemplate.getHoloBlue());
        lineDataSet.setLineWidth(1.5f);


        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        Log.e(TAG, "=================================================================================================");
//        lineDataSet.setValueFormatter(new IValueFormatter() {
//            @Override
//            public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
//                Log.e(TAG, "=======================================================getFormattedValue" + entry.getX());
//                return changeDateFormat(entry.getX());
//            }
//        });

        LineData lineData = new LineData(lineDataSet);
        mLineChart.setData(lineData);
        mLineChart.invalidate();
    }

    @Override
    public void onLoaderReset(Loader<List<HistoricalQuote>> loader) {

    }

    public String changeDateFormat(float date){
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM"); ///dd/yyyy");
        return(dateFormat.format(new Date((long)date)));
    }

//    public class MyXAxisValueFormatter implements IAxisValueFormatter{
//
//        private ArrayList<String> xLabels;
//        public MyXAxisValueFormatter(ArrayList<String> xLabels){
//            this.xLabels = xLabels;
//        }
//
//        @Override
//        public String getFormattedValue(float value, AxisBase axis) {
//            return xLabels.get((int)value);
//        }
//    }
}
