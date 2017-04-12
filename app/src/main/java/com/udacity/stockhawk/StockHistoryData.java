package com.udacity.stockhawk;

/**
 * Created by manvi on 11/4/17.
 */

public class StockHistoryData {

    private long mDate;
    private double mClosingValue;

    public StockHistoryData(long mDate,double mClosingValue)
    {
        this.mDate = mDate;
        this.mClosingValue = mClosingValue;
    }

    public long getmDate(){
        return this.mDate;
    }

    public double getmClosingValue(){
        return this.mClosingValue;
    }
}
