package com.udacity.stockhawk.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.EntryXComparator;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.utils.Constants;
import com.udacity.stockhawk.utils.MyMarkerView;
import com.udacity.stockhawk.utils.Utils;


import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

public class DetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<HistoricalQuote>>{

    @BindView(R.id.chart) LineChart mLineChart;
    @BindView(R.id.tvXMax) TextView mLabelX;
    @BindView(R.id.tvYMax) TextView mLabelY;
    @BindView(R.id.pb_loading_bar)
    ProgressBar mLoadingBar;
    private static final int DETAIL_LOADER_ID = 5000;
    private XAxis xAxis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        ButterKnife.bind(this);

        String symbol;
        Intent intent = getIntent();
        if(intent.hasExtra(Constants.STOCK_NAME))
        {
            symbol = intent.getStringExtra(Constants.STOCK_NAME);
            setTitle(symbol);
            Bundle bundle = new Bundle();
            bundle.putString(Constants.STOCK_NAME,symbol);
            setupLineChart(symbol);
            setupXAxis();
            setupYAxis();
            setMarkerViewOnValues();
            if(Utils.networkUp(this))
            {
                showProgressBar();
                getSupportLoaderManager().initLoader(DETAIL_LOADER_ID, bundle,DetailActivity.this);
            }
            else {
                mLabelY.setVisibility(View.INVISIBLE);
                mLabelX.setVisibility(View.INVISIBLE);
                mLoadingBar.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void showDataView(){
        mLineChart.setVisibility(View.VISIBLE);
        mLabelY.setVisibility(View.VISIBLE);
        mLabelX.setVisibility(View.VISIBLE);
        mLoadingBar.setVisibility(View.INVISIBLE);
    }

    private void showProgressBar(){
        mLineChart.setVisibility(View.INVISIBLE);
        mLabelY.setVisibility(View.INVISIBLE);
        mLabelX.setVisibility(View.INVISIBLE);
        mLoadingBar.setVisibility(View.VISIBLE);
    }

    private void setupXAxis(){
        xAxis = mLineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(15f);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawAxisLine(true);
        xAxis.setDrawLabels(true);
        xAxis.setGranularityEnabled(true);
        xAxis.setGranularity(1f);
        xAxis.setEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            xAxis.setAxisLineColor(getColor(R.color.material_red_700));
        } else {
            xAxis.setAxisLineColor(getResources().getColor(R.color.material_red_700));
        }
        xAxis.setAxisLineWidth(2f);
    }

    private void setupYAxis(){
        YAxis leftAxis = mLineChart.getAxisLeft();
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART); //make INSIDE_CHART if you want to draw a line for the yAxis.
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setGranularityEnabled(true);

        leftAxis.setTextSize(15f);
        leftAxis.setDrawLabels(true);
        leftAxis.setDrawZeroLine(true); // draw a zero line
        leftAxis.setDrawAxisLine(true);
        leftAxis.setEnabled(true);
        leftAxis.setGranularity(1f);
        leftAxis.setDrawLimitLinesBehindData(true); // to draw grid lines behind data.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            leftAxis.setAxisLineColor(getColor(R.color.material_red_700));
        }else {
            leftAxis.setAxisLineColor(getResources().getColor(R.color.material_red_700));
        }
        leftAxis.setAxisLineWidth(2f);
    }

    private void setupLineChart(String symbol){
        mLineChart.setDrawGridBackground(false);
        mLineChart.setNoDataText(getString(R.string.no_history_data,symbol));
        Paint p = mLineChart.getPaint(Chart.PAINT_INFO);
        p.setTextSize(40f);

        mLineChart.setBackgroundColor(Color.TRANSPARENT);
        mLineChart.setDescription(null);
        mLineChart.animateX(1500);
    }

    private void setMarkerViewOnValues(){
        MyMarkerView mv = new MyMarkerView(this, R.layout.marker_view);
        mv.setChartView(mLineChart); // For bounds control
        mLineChart.setMarker(mv); // Set the marker to the chart
    }

    @Override
    public Loader<List<HistoricalQuote>> onCreateLoader(int id, final Bundle args) {
        return new AsyncTaskLoader<List<HistoricalQuote>>(this) {

            List<HistoricalQuote> mHistoryData = null;
            @Override
            protected void onStartLoading() {
                super.onStartLoading();
                if(mHistoryData!=null)
                {
                    deliverResult(mHistoryData);
                }
                else {
                    forceLoad();
                }
            }

            @Override
            public List<HistoricalQuote> loadInBackground() {
                String symbol = args.getString(Constants.STOCK_NAME);
                try {
                    Calendar from = Calendar.getInstance();
                    Calendar to = Calendar.getInstance();
                    from.add(Calendar.YEAR, -Constants.YEARS_OF_HISTORY);
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

            @Override
            public void deliverResult(List<HistoricalQuote> data) {
                mHistoryData = data;
                super.deliverResult(data);
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<List<HistoricalQuote>> loader, List<HistoricalQuote> data) {

        if (data != null && data.size() != 0) {
            showDataView();
            List<Entry> Enteries = new ArrayList<>();
            ArrayList<String> DateInMilis = new ArrayList<>();

            int index = 0;
            for (HistoricalQuote history : data) {
                float date = history.getDate().getTimeInMillis();
                String formatDate = changeDateFormat(date);
                DateInMilis.add(formatDate);
                float closingDate = history.getClose().floatValue();

                //This is required to show the correct values on the X Label.
                Enteries.add(new Entry(index++, closingDate));
            }
            //this library does not officially support drawing LineChart data from an Entry list not sorted by the x-position
            // of the entries in ascending manner. Adding entries in an unsorted way may result in correct drawing, but may
            // also lead to unexpected behaviour. A List of Entry objects can be sorted manually or using the EntryXComparator:
            Collections.sort(Enteries, new EntryXComparator());
            setEntryValuesInLineDataset(Enteries);
            setXAxisDataValues(DateInMilis);
        }
        else {
            mLoadingBar.setVisibility(View.INVISIBLE);
            mLineChart.setVisibility(View.VISIBLE);
        }
    }

    private void setXAxisDataValues(ArrayList<String> dateInMilis)
    {
        final String[] closeDates = dateInMilis.toArray(new String[dateInMilis.size()]);
        xAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return closeDates[(int) value];
            }
        });
    }

    private void setEntryValuesInLineDataset(List<Entry> Entries) {
        LineDataSet lineDataSet;
        if (mLineChart.getData() != null &&
                mLineChart.getData().getDataSetCount() > 0) {
            lineDataSet = (LineDataSet) mLineChart.getData().getDataSetByIndex(0);
            lineDataSet.setValues(Entries);
            mLineChart.getData().notifyDataChanged();
            mLineChart.notifyDataSetChanged();
        } else {
            lineDataSet = new LineDataSet(Entries, "Stock Data");
            lineDataSet.setDrawIcons(false);
            lineDataSet.setValueTextSize(9f);
            lineDataSet.setColor(Color.BLUE);
            lineDataSet.setCircleColor(Color.RED);
            lineDataSet.setValueTextColor(ColorTemplate.getHoloBlue());
            lineDataSet.setLineWidth(1.5f);
            lineDataSet.setDrawFilled(true);
            lineDataSet.setDrawCircles(true);
            lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);

            // fill drawable only supported on api level 18 and above
            Drawable drawable = ContextCompat.getDrawable(this, R.drawable.fade_red);
            lineDataSet.setFillDrawable(drawable);
            LineData lineData = new LineData(lineDataSet);
            mLineChart.setData(lineData);
            mLineChart.invalidate();
        }
    }

    @Override
    public void onLoaderReset(Loader<List<HistoricalQuote>> loader) {

    }

    private String changeDateFormat(float date){
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM-yy", Locale.getDefault());
        return(dateFormat.format(new Date((long)date)));
    }
}
