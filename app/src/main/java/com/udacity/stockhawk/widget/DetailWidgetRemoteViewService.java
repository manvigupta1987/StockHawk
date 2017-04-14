package com.udacity.stockhawk.widget;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.Build;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;
import com.udacity.stockhawk.sync.QuoteSyncJob;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Created by manvi on 13/4/17.
 */

public class DetailWidgetRemoteViewService extends RemoteViewsService {

    private final static String TAG = DetailWidgetRemoteViewService.class.getSimpleName();
    private final String[] projection = {Contract.Quote.TABLE_NAME + "." + Contract.Quote._ID,
            Contract.Quote.COLUMN_SYMBOL,
            Contract.Quote.COLUMN_PRICE,
            Contract.Quote.COLUMN_ABSOLUTE_CHANGE,
            Contract.Quote.COLUMN_PERCENTAGE_CHANGE};

    static final int INDEX_STOCK_ID = 0;
    static final int INDEX_STOCK_SYMBOL = 1;
    static final int INDEX_STOCK_PRICE = 2;
    static final int INDEX_STOCK_ABS_CHANGE = 3;
    static final int INDEX_STOCK_PER_CHANGE = 4;

    private  DecimalFormat dollarFormatWithPlus;
    private  DecimalFormat dollarFormat;
    private  DecimalFormat percentageFormat;


    @Override
    public RemoteViewsFactory onGetViewFactory(final Intent intent) {
        return new RemoteViewsFactory() {
            private Cursor cursor =null;
            @Override
            public void onCreate() {
                //Nothing to do here
                dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
                dollarFormatWithPlus = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
                dollarFormatWithPlus.setPositivePrefix("+$");
                percentageFormat = (DecimalFormat) NumberFormat.getPercentInstance(Locale.getDefault());
                percentageFormat.setMaximumFractionDigits(2);
                percentageFormat.setMinimumFractionDigits(2);
                percentageFormat.setPositivePrefix("+");
            }

            @Override
            public void onDataSetChanged() {
                if(cursor!=null)
                {
                    cursor.close();
                }
                // This method is called by the app hosting the widget (e.g., the launcher)
                // However, our ContentProvider is not exported so it doesn't have access to the
                // data. Therefore we need to clear (and finally restore) the calling identity so
                // that calls use our process and permission
                final long identityToken = Binder.clearCallingIdentity();
                cursor = getContentResolver().query(Contract.Quote.URI,projection,null,null,Contract.Quote.COLUMN_SYMBOL);
                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if(cursor!=null)
                {
                    cursor.close();
                    cursor = null;
                }
            }

            @Override
            public int getCount() {
                if(cursor == null)
                {
                    return 0;
                }else{
                    return cursor.getCount();
                }
            }

            @Override
            public RemoteViews getViewAt(int position) {

                if(position == AdapterView.INVALID_POSITION ||
                        cursor == null || !cursor.moveToPosition(position)){
                    return null;
                }

                RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.widget_detail_list_item);
                String symbol = cursor.getString(INDEX_STOCK_SYMBOL);
                float price = cursor.getFloat(INDEX_STOCK_PRICE);
                float rawAbsoluteChange = cursor.getFloat(INDEX_STOCK_ABS_CHANGE);
                float percentageChange = cursor.getFloat(INDEX_STOCK_PER_CHANGE);

                String stock_price = dollarFormat.format(price);
                remoteViews.setTextViewText(R.id.widget_stock_symbol, symbol);
                remoteViews.setTextViewText(R.id.widget_stock_price, stock_price);

                if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    remoteViews.setContentDescription(R.id.widget_stock_symbol,symbol);
                    remoteViews.setContentDescription(R.id.widget_stock_price, stock_price);
                }

                if (rawAbsoluteChange > 0) {
                    remoteViews.setInt(R.id.widget_stock_change, "setBackgroundResource", R.drawable.percent_change_pill_green);
                } else {
                    remoteViews.setInt(R.id.widget_stock_change,"setBackgroundResource",R.drawable.percent_change_pill_red);
                }

                String change = dollarFormatWithPlus.format(rawAbsoluteChange);
                String percentage = percentageFormat.format(percentageChange / 100);

                Context context = DetailWidgetRemoteViewService.this;
                if (PrefUtils.getDisplayMode(context)
                        .equals(context.getString(R.string.pref_display_mode_absolute_key))) {
                    remoteViews.setTextViewText(R.id.widget_stock_change, change);
                } else {
                    remoteViews.setTextViewText(R.id.widget_stock_change, percentage);
                }

                final Intent fillInIntent = new Intent();
                fillInIntent.putExtra(QuoteSyncJob.STOCK_NAME, symbol);
                remoteViews.setOnClickFillInIntent(R.id.widget_list_item,fillInIntent);
                return remoteViews;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(),R.layout.widget_detail_list_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if(cursor.moveToPosition(position)){
                    return cursor.getLong(INDEX_STOCK_ID);
                }
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
