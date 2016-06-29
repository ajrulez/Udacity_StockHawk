package com.sam_chordas.android.stockhawk.rest;

/**
 * Created by XX on 21/6/2016.
 */

public class Stock {
    public final String mSymbol;
    public final String mDate;
    public final double mBid;

    public Stock(String symbol, String date, double bid) {
        mSymbol = symbol;
        mDate = date;
        mBid = bid;
    }

    public String getSymbol() {
        return mSymbol;
    }

    public String getDate() {
        return mDate;
    }

    public double getBid() {
        return mBid;
    }

}
