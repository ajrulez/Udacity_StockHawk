package com.sam_chordas.android.stockhawk.service;

import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.data.QuoteHistoricalColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class StockHistoryTaskService extends GcmTaskService {

    private String LOG_TAG = StockHistoryTaskService.class.getSimpleName();
    public static final String ADD_TAG = "add";
    public static final String INIT_TAG = "init";
    public static final String PERIODIC_TAG = "periodic";
    public static final String UTF = "UTF-8";

    private OkHttpClient client = new OkHttpClient();
    private Context mContext;
    private StringBuilder mStoredSymbols = new StringBuilder();

    public StockHistoryTaskService() {
    }

    public StockHistoryTaskService(Context context) {
        mContext = context;
    }

    //Add the close connection header to solve "ProtocolException" error
    String fetchData(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    @Override
    public int onRunTask(TaskParams params) {
        Cursor initQueryCursor;
        if (mContext == null) {
            mContext = this;
        }
        StringBuilder urlStringBuilder = new StringBuilder();

        try {
            // Base URL for the Yahoo query
            urlStringBuilder.append(Utils.YAHOO_BASE_URL);
            urlStringBuilder.append(URLEncoder.encode(Utils.SEARCH_QUERY_HISTORY, UTF));
            Log.d(LOG_TAG, "urlStringBuilder: " + urlStringBuilder.toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (params.getTag().equals(INIT_TAG) || params.getTag().equals(PERIODIC_TAG)) {
            initQueryCursor = mContext.getContentResolver().
                    query(QuoteProvider.QuotesHistorical.CONTENT_URI,
                            new String[]{"Distinct " + QuoteHistoricalColumns.SYMBOL}, null,
                            null, null);
            if (initQueryCursor.getCount() == 0 || initQueryCursor == null) {
                // Init task. Populates DB with quotes for the symbols seen below
                try {
                    urlStringBuilder.append(
                            URLEncoder.encode(Utils.SEARCH_QUERY_DEFAULT, UTF));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            } else if (initQueryCursor != null) {
                DatabaseUtils.dumpCursor(initQueryCursor);
                initQueryCursor.moveToFirst();
                for (int i = 0; i < initQueryCursor.getCount(); i++) {
                    mStoredSymbols.append("\"" + initQueryCursor.getString(initQueryCursor.
                            getColumnIndex(QuoteHistoricalColumns.SYMBOL)) + "\",");
                    initQueryCursor.moveToNext();
                }
                mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), ")");
                try {
                    urlStringBuilder.append(URLEncoder.encode(mStoredSymbols.toString(), UTF));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        } else if (params.getTag().equals(ADD_TAG)) {
            // get symbol from params.getExtra and build query
            String stockInput = params.getExtras().getString("symbol");
            Log.d(LOG_TAG, "stockInput: " + stockInput);
            try {
                urlStringBuilder.append(URLEncoder.encode("\"" + stockInput + "\")", UTF));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        // finalize the URL for the API query.
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date currentDate = new Date();
        Calendar calStart = Calendar.getInstance();
        calStart.setTime(currentDate);
        calStart.add(Calendar.DATE, -6);
        String startDate = dateFormat.format(calStart.getTime());

        Calendar calEnd = Calendar.getInstance();
        calEnd.setTime(currentDate);
        calEnd.add(Calendar.DATE, 0);
        String endDate = dateFormat.format(calEnd.getTime());
        try {
            urlStringBuilder.append(URLEncoder.encode(
                    "and" + " startDate=\"" + startDate + "\" and" + " endDate=\"" + endDate + "\"", UTF));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        urlStringBuilder.append(Utils.SEARCH_QUERY_PARAM);
        String urlString;
        String getResponse;
        int result = GcmNetworkManager.RESULT_FAILURE;

        if (urlStringBuilder != null) {
            urlString = urlStringBuilder.toString();
            Log.d(LOG_TAG, "urlString: " + urlString);
            try {
                getResponse = fetchData(urlString);
                result = GcmNetworkManager.RESULT_SUCCESS;
                try {
                    mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                            Utils.clearHistoricalJsonToContentVals(getResponse));
                    mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                            Utils.quoteHistoricalJsonToContentVals(getResponse));
                } catch (RemoteException | OperationApplicationException e) {
                    Log.e(LOG_TAG, "Error applying historical batch insert", e);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}


