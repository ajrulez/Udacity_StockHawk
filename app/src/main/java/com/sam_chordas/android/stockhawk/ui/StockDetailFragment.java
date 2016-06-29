package com.sam_chordas.android.stockhawk.ui;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.db.chart.Tools;
import com.db.chart.model.LineSet;
import com.db.chart.view.AxisController;
import com.db.chart.view.LineChartView;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteHistoricalColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;

public class StockDetailFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private String LOG_TAG = StockDetailFragment.class.getSimpleName();
    static final String DETAIL_SYMBOL = "symbol";
    private static final int CURSOR_LOADER_ID = 1;
    private static final int CURSOR_LOADER_HISTORY_ID = 2;

    private String mSymbol;

    TextView mStockSymbolView;
    TextView mStockBidView;
    TextView mStockChangeView;
    LineChartView mLineChart;

    public StockDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSymbol = getArguments().getString(DETAIL_SYMBOL);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);
        getLoaderManager().initLoader(CURSOR_LOADER_HISTORY_ID, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.stock_detail, container, false);
        mStockSymbolView = (TextView) rootView.findViewById(R.id.stock_symbol);
        mStockBidView = (TextView) rootView.findViewById(R.id.stock_bid);
        mStockChangeView = (TextView) rootView.findViewById(R.id.stock_change);
        mLineChart = (LineChartView) rootView.findViewById(R.id.linechart);
        return rootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d(LOG_TAG, "oncreateLoader & id: " + id);

        if (id == CURSOR_LOADER_ID) {
            return new CursorLoader(getContext(), QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                            QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                    QuoteColumns.SYMBOL + " = \"" + mSymbol + "\"", null, null);
        } else if (id == CURSOR_LOADER_HISTORY_ID) {
            //CursorLoader(Context context, Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
            return new CursorLoader(getActivity(), QuoteProvider.QuotesHistorical.CONTENT_URI,
                    new String[]{QuoteHistoricalColumns._ID, QuoteHistoricalColumns.SYMBOL,
                            QuoteHistoricalColumns.BIDPRICE, QuoteHistoricalColumns.DATE},
                    QuoteColumns.SYMBOL + " = \"" + mSymbol + "\"", null, null);
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.d(LOG_TAG, "onLoaderFinished: " + data.getCount());
        Log.d(LOG_TAG, "loader id: " + loader.getId());
        if (loader.getId() == CURSOR_LOADER_ID && data != null && data.moveToFirst()) {
            String symbol = data.getString(data.getColumnIndex(QuoteColumns.SYMBOL));
            mStockSymbolView.setText(symbol);
            String bidPrice = data.getString(data.getColumnIndex(QuoteColumns.BIDPRICE));
            mStockBidView.setText(Utils.truncateBidPrice(bidPrice));
            String change = data.getString(data.getColumnIndex(QuoteColumns.CHANGE));
            String percentChange = data.getString(data.getColumnIndex(QuoteColumns.PERCENT_CHANGE));
            String showedChange = Utils.truncateChange(change, false) + "(" +
                    Utils.truncateChange(percentChange, true) + ")";
            mStockChangeView.setText(showedChange);
            Log.d(LOG_TAG, "onloader finished" + symbol + bidPrice + showedChange);
        } else if (loader.getId() == CURSOR_LOADER_HISTORY_ID && data != null && data.moveToFirst()) {
            showChart(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    public void showChart(Cursor data) {
        float minBidPrice = Float.MAX_VALUE;
        float maxBidPrice = Float.MIN_VALUE;
        LineSet lineSet = new LineSet();
        Log.d(LOG_TAG, "data.size: " + data.getCount());

        for (data.moveToLast(); !data.isBeforeFirst(); data.moveToPrevious()) {
            Log.d(LOG_TAG, "TEST");
            String date = data.getString(data.getColumnIndex(QuoteHistoricalColumns.DATE));
            String bidPrice = data.getString(data.getColumnIndex(QuoteHistoricalColumns.BIDPRICE));
            float price = Float.parseFloat(bidPrice);
            Log.d(LOG_TAG, "date price: " + date + bidPrice);
            lineSet.addPoint(date, price);
            minBidPrice = Math.min(minBidPrice, price);
            maxBidPrice = Math.max(maxBidPrice, price);
        }
        lineSet.setColor(getResources().getColor(R.color.white))
                .setDotsColor(getResources().getColor(R.color.white))
                .setThickness(2)
                .setSmooth(false);

        mLineChart.setBorderSpacing(Tools.fromDpToPx(20))
                .setXAxis(true)
                .setYAxis(true)
                .setXLabels(AxisController.LabelPosition.OUTSIDE)
                .setYLabels(AxisController.LabelPosition.OUTSIDE)

                .setAxisLabelsSpacing((maxBidPrice - minBidPrice) / 2f)
                .setAxisBorderValues(Math.round(minBidPrice - (maxBidPrice - minBidPrice) / 5f),
                        Math.round(maxBidPrice + (maxBidPrice - minBidPrice) / 5f))
                .addData(lineSet);

        mLineChart.show();
    }
}
