package com.sam_chordas.android.stockhawk.ui;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import com.sam_chordas.android.stockhawk.R;

public class StockDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_detail);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if(savedInstanceState == null){
            Bundle argument = new Bundle();
            argument.putString(StockDetailFragment.DETAIL_SYMBOL, getIntent().
                    getStringExtra(StockDetailFragment.DETAIL_SYMBOL));

            StockDetailFragment fragment = new StockDetailFragment();
            fragment.setArguments(argument);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.stock_detail_container, fragment)
                    .commit();
        }
    }
}
