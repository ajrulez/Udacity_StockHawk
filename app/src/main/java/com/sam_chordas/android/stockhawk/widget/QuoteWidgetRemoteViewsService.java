package com.sam_chordas.android.stockhawk.widget;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.Build;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class QuoteWidgetRemoteViewsService extends RemoteViewsService {
    public final String LOG_TAG = QuoteWidgetRemoteViewsService.class.getSimpleName();

//    public QuoteWidgetRemoteViewsService() {
//    }

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            private Cursor data = null;
            @Override
            public void onCreate() {
            }

            @Override
            public void onDataSetChanged() {
                if(data!=null){
                    data.close();
                }

                final long identityToken = Binder.clearCallingIdentity();
                data = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                                new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                        QuoteColumns.ISCURRENT + "=?", new String[]{"1"}, null);
                Binder.restoreCallingIdentity(identityToken);
                Log.d(LOG_TAG, data.toString());
            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if(position == AdapterView.INVALID_POSITION || data == null || !data.moveToPosition(position)){
                    return null;
                }
                RemoteViews views = new RemoteViews(getPackageName(), R.layout.list_item_quote);
                String symbol = data.getString(data.getColumnIndex(QuoteColumns.SYMBOL));
                String bidPrice = data.getString(data.getColumnIndex(QuoteColumns.BIDPRICE));
                String change = data.getString(data.getColumnIndex(QuoteColumns.CHANGE));
                String change_percent = data.getString(data.getColumnIndex(QuoteColumns.PERCENT_CHANGE));
                boolean isUp = data.getInt(data.getColumnIndex(QuoteColumns.ISUP)) == 1;
                String showedChange = Utils.truncateChange(change, false) + "(" +
                        Utils.truncateChange(change_percent, true) + ")";
                Log.d(LOG_TAG, showedChange);
                views.setTextViewText(R.id.stock_symbol, symbol);
                views.setTextViewText(R.id.bid_price, bidPrice);
                views.setTextViewText(R.id.change, showedChange);
                if(isUp)
                    views.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_green);
               else
                    views.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_red);
                final Intent fillIntent = new Intent();
//                fillIntent.putExtra(MyStocksActivity.INTENT_SYMBOL, symbol);
//                views.setOnClickFillInIntent(R.id.widget_list_item, fillIntent);
                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return null;
                //return new RemoteViews(getPackageName(), R.layout.widget_detail_list_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (data !=null && data.moveToPosition(position))

                return data.getLong(data.getColumnIndex(QuoteColumns._ID));
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
