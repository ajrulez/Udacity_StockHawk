package com.sam_chordas.android.stockhawk.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.R;


/**
 * Created by sam_chordas on 10/1/15.
 */
public class StockIntentService extends IntentService {
    private String LOG_TAG = StockIntentService.class.getSimpleName();

    public static final String TAG = "tag";
    public static final String ADD_TAG = "add";
    public static final String INTENT_SYMBOL = "symbol";

    public StockIntentService() {
        super(StockIntentService.class.getName());
    }

    public StockIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(LOG_TAG, "Stock Intent Service");
        StockTaskService stockTaskService = new StockTaskService(this);
        StockHistoryTaskService stockHistoryTaskService = new StockHistoryTaskService(this);
        Bundle args = new Bundle();
        if (intent.getStringExtra(TAG).equals(ADD_TAG)) {
            args.putString(INTENT_SYMBOL,
                    intent.getStringExtra(getString(R.string.symbol)));
        }
        // We can call OnRunTask from the intent service to force it to run immediately instead of
        // scheduling a task.
        Handler mHandler = new Handler(getMainLooper());
        if (stockTaskService.onRunTask(new TaskParams(intent.getStringExtra(TAG), args))
                == GcmNetworkManager.RESULT_FAILURE) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.invalid_input_toast),
                            Toast.LENGTH_LONG).show();
                }
            });
        }
        // stockTaskService.onRunTask(new TaskParams(intent.getStringExtra(TAG), args));
        stockHistoryTaskService.onRunTask(new TaskParams(intent.getStringExtra(TAG), args));
    }
}
