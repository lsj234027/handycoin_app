package com.dev.seongenie.geniecoin;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import butterknife.ButterKnife;

/**
 * Created by seongjinlee on 2017. 10. 9..
 */

public class SettingActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_orderbook_container);

    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.enter_left, R.anim.leave_left);
    }
}