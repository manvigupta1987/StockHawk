package com.udacity.stockhawk.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;

import com.udacity.stockhawk.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by manvi on 11/4/17.
 */

public class Utils {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef( {STOCK_STATUS_OK, STOCK_STATUS_SERVER_DOWN, STOCK_STATUS_NO_NETWORK,STOCK_STATUS_UNKNOWN})

    public @interface StockStatus {}

    @SuppressWarnings("WeakerAccess")
    public static final int STOCK_STATUS_OK =0;
    public static final int STOCK_STATUS_SERVER_DOWN = 1;
    public static final int STOCK_STATUS_NO_NETWORK = 2;
    public static final int STOCK_STATUS_UNKNOWN =3;


    @SuppressLint("ApplySharedPref")
    public static void setStockStatus(@StockStatus int status, Context context)
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor spe = sharedPreferences.edit();
        spe.putInt(context.getString(R.string.pref_stock_status_key),status);
        spe.commit();
    }

    @Utils.StockStatus
    public static int getStockStatus(Context context)
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        @StockStatus int status =  sharedPreferences.getInt(context.getString(R.string.pref_stock_status_key),STOCK_STATUS_UNKNOWN);
        return status;
    }

    public static boolean networkUp(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }

    public static void updateWidgets(Context context){
        Intent dataUpdatedIntent = new Intent(Constants.ACTION_DATA_UPDATED);
        context.sendBroadcast(dataUpdatedIntent);
    }
}
