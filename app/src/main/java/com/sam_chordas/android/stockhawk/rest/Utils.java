package com.sam_chordas.android.stockhawk.rest;

import android.content.ContentProviderOperation;
import android.util.Log;

import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteHistoricalColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by sam_chordas on 10/8/15.
 */
public class Utils {

    private static String LOG_TAG = Utils.class.getSimpleName();
    public static String YAHOO_BASE_URL = "https://query.yahooapis.com/v1/public/yql?q=";
    public static String SEARCH_QUERY = "select * from yahoo.finance.quotes where symbol in (";
    public static String SEARCH_QUERY_DEFAULT = "\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")";
    public static String SEARCH_QUERY_PARAM = "&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
            + "org%2Falltableswithkeys&callback=";
    public static String SEARCH_QUERY_HISTORY = "select * from yahoo.finance.historicaldata where symbol in (";

    public static boolean showPercent = true;

    public static ArrayList<ContentProviderOperation> quoteJsonToContentVals(String JSON) {
        Log.d(LOG_TAG, "quoteJsonToContentVal");
        ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
        JSONObject jsonObject = null;
        JSONArray resultsArray = null;
        try {
            jsonObject = new JSONObject(JSON);
            if (jsonObject != null && jsonObject.length() != 0) {
                jsonObject = jsonObject.getJSONObject("query");
                int count = Integer.parseInt(jsonObject.getString("count"));
                if (count == 1) {
                    jsonObject = jsonObject.getJSONObject("results")
                            .getJSONObject("quote");
                    ContentProviderOperation cpo = buildBatchOperation(jsonObject);
                    if (cpo != null) {
                        batchOperations.add(cpo);
                    }
                } else {
                    resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");

                    if (resultsArray != null && resultsArray.length() != 0) {
                        for (int i = 0; i < resultsArray.length(); i++) {
                            jsonObject = resultsArray.getJSONObject(i);
                            batchOperations.add(buildBatchOperation(jsonObject));
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "String to JSON failed: " + e);
        }
        return batchOperations;
    }

    public static ArrayList<ContentProviderOperation> clearHistoricalJsonToContentVals(String JSON) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        JSONObject jsonObject = null;
        JSONArray resultsArray = null;
        try {
            jsonObject = new JSONObject(JSON);
            if (jsonObject != null && jsonObject.length() != 0) {
                jsonObject = jsonObject.getJSONObject("query").getJSONObject("results");
                resultsArray = jsonObject.getJSONArray("quote");
                for (int i = 0; i < resultsArray.length(); i++) {
                    jsonObject = resultsArray.getJSONObject(i);
                    ops.add(deleteHistoricalBatchOperation(jsonObject));
                }
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "String to JSON failed: " + e);
        }
        return ops;
    }

    public static ArrayList<ContentProviderOperation> quoteHistoricalJsonToContentVals(String JSON) {
        Log.d(LOG_TAG, "quotehistoryJsonToContentVal");
        ArrayList<ContentProviderOperation> historicalBatchOperations = new ArrayList<>();
        JSONObject jsonObject = null;
        JSONArray resultsArray = null;
        try {
            jsonObject = new JSONObject(JSON);
            if (jsonObject != null && jsonObject.length() != 0) {
                jsonObject = jsonObject.getJSONObject("query").getJSONObject("results");
                resultsArray = jsonObject.getJSONArray("quote");
                for (int i = 0; i < resultsArray.length(); i++) {
                    jsonObject = resultsArray.getJSONObject(i);
                    historicalBatchOperations.add(buildHistoricalBatchOperation(jsonObject));
                }
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "String to JSON failed: " + e);
        }
        return historicalBatchOperations;
    }


    public static String truncateBidPrice(String bidPrice) {
        bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));
        return bidPrice;
    }

    public static String truncateChange(String change, boolean isPercentChange) {
        String weight = change.substring(0, 1);
        String ampersand = "";
        if (isPercentChange) {
            ampersand = change.substring(change.length() - 1, change.length());
            change = change.substring(0, change.length() - 1);
        }
        change = change.substring(1, change.length());
        double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
        change = String.format("%.2f", round);
        StringBuffer changeBuffer = new StringBuffer(change);
        changeBuffer.insert(0, weight);
        changeBuffer.append(ampersand);
        change = changeBuffer.toString();
        return change;
    }

    public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject) {
        Log.d(LOG_TAG, "buildBatchOperation");
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
                QuoteProvider.Quotes.CONTENT_URI);
        String change = null;
        try {
            change = jsonObject.getString("Change");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (change != null && change != "null") {
            try {
                builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString("symbol"));
                builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(jsonObject.getString("Bid")));
                builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(
                        jsonObject.getString("ChangeinPercent"), true));
                builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
                builder.withValue(QuoteColumns.ISCURRENT, 1);
                if (change.charAt(0) == '-') {
                    builder.withValue(QuoteColumns.ISUP, 0);
                } else {
                    builder.withValue(QuoteColumns.ISUP, 1);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return builder.build();
        }
        return null;
    }

    public static ContentProviderOperation deleteHistoricalBatchOperation(JSONObject jsonObject) throws JSONException {
        String symbol = null;
        try {
            symbol = jsonObject.getString("Symbol");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        ContentProviderOperation.Builder builder = ContentProviderOperation.
                newDelete(QuoteProvider.QuotesHistorical.CONTENT_URI);
        builder.withSelection(QuoteHistoricalColumns.SYMBOL + "=?", new String[]{symbol});
        return builder.build();
    }

    public static ContentProviderOperation buildHistoricalBatchOperation(JSONObject jsonObject) throws JSONException {
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
                QuoteProvider.QuotesHistorical.CONTENT_URI);
        try {
            builder.withValue(QuoteHistoricalColumns.SYMBOL, jsonObject.getString("Symbol"));
            builder.withValue(QuoteHistoricalColumns.BIDPRICE, truncateBidPrice(
                    jsonObject.getString("Close")));
            builder.withValue(QuoteHistoricalColumns.DATE, getMMDate(jsonObject.getString("Date")));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return builder.build();
    }

    public static String getMMDate(String date) {
        //From 2016-06-23 to 06-23
        return date.substring(5);
    }
}
