package com.dev.seongenie.geniecoin.Fragment;

/**
 * Created by seongjinlee on 2017. 10. 2..
 */

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.simplelist.MaterialSimpleListAdapter;
import com.afollestad.materialdialogs.simplelist.MaterialSimpleListItem;
import com.dev.seongenie.geniecoin.Api.CommonMessage;
import com.dev.seongenie.geniecoin.CoinSources.BasicCoin;
import com.dev.seongenie.geniecoin.CoinSources.ColorPrice;
import com.dev.seongenie.geniecoin.Fragment.OrderBook.TradeHistory;
import com.dev.seongenie.geniecoin.Fragment.OrderBook.TradeHistoryView;
import com.dev.seongenie.geniecoin.Layout.OrderbookTableLayout;
import com.dev.seongenie.geniecoin.MainActivity;
import com.dev.seongenie.geniecoin.R;
import com.dev.seongenie.geniecoin.ServerConnection.RestfulApi;
import com.dev.seongenie.geniecoin.View.OrderBookDataView;
import com.rey.material.app.Dialog;
import com.rey.material.widget.Button;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


/**
 * A simple {@link Fragment} subclass.
 */
public class OrderBookFragment extends Fragment {

    @BindView(R.id.ask_amount) OrderbookTableLayout askAmountLayout;
    @BindView(R.id.ask_price) OrderbookTableLayout askPriceLayout;
    @BindView(R.id.bid_amount) OrderbookTableLayout bidAmountLayout;
    @BindView(R.id.bid_price) OrderbookTableLayout bidPriceLayout;
    @BindView(R.id.total_left_qnty) TextView totalLeftQnty;
    @BindView(R.id.total_right_qnty) TextView totalRightQnty;
    @BindView(R.id.orderbook_coin_name) TextView coinNameTextView;
    @BindView(R.id.orderbook_exchange_name) TextView exchangeNameTextView;
    @BindView(R.id.orderbook_coin_price) TextView coinPriceTextView;
    @BindView(R.id.orderbook_diff_rate) TextView diffRateTextView;
    @BindView(R.id.orderbook_coin_icon) ImageView coinIcon;
    @BindView(R.id.orderbook_triangle) ImageView orderbookTriangle;
    @BindView(R.id.volume) TextView volumeTextView;
    @BindView(R.id.start_price) TextView startPriceTextView;
    @BindView(R.id.lowedst_price) TextView lowPriceTextView;
    @BindView(R.id.highest_price) TextView highPriceTextView;

    private OrderBookFragment thisFragment = this;
    private BasicCoin basicCoin = null;

    private Timer timer = null;
    private int REFRESH_INTERVAL = 3000;
    private int BLINK_DURATION = 150;
    private PriorityQueue<String> queue = new PriorityQueue<String>();
    AlphaAnimation mAnimation = null;
    Callback<OrderBookDataView> orderbookCallback;
    Callback<TradeHistoryView> tradeHistoryCallback;

    private static final String TAG_SELECT_ICON_DIALOG = "TAG_SELECT_ICON_DIALOG";
    public OrderBookFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_orderbook_container, container, false);
        ButterKnife.bind(this, v);

//        message = (TextView) v.findViewById(R.id.no_select);

        coinNameTextView.setTypeface(MainActivity.nanumgothicbold);
        exchangeNameTextView.setTypeface(MainActivity.nanumgothic);
        coinPriceTextView.setTypeface(MainActivity.nanumgothic);
        diffRateTextView.setTypeface(MainActivity.nanumgothic);
        volumeTextView.setTextColor(Color.BLACK);
        startPriceTextView.setTextColor(Color.BLACK);

        orderbookCallback = new Callback<OrderBookDataView>() {

            @Override
            public void onResponse(Call<OrderBookDataView> call, Response<OrderBookDataView> response) {
                if (response != null && response.code() == 200 ) {
                    Map<String, Double> ask = response.body().getOrderBook().getAsk();
                    Map<String, Double> bid = response.body().getOrderBook().getBid();
                    double lastPrice = response.body().getOrderBook().getLastPrice();
                    double prevPrice = response.body().getOrderBook().getPrevPrice();
                    double minPrice = response.body().getOrderBook().getMinPrice();
                    double maxPrice = response.body().getOrderBook().getMaxPrice();
                    double volume = response.body().getOrderBook().getVolume();

                    bidPriceLayout.setBorder(-1);
                    askPriceLayout.setBorder(-1);


                    if(minPrice < prevPrice) {
                        lowPriceTextView.setTextColor(Color.BLUE);
                    } else if (minPrice == prevPrice) {
                        lowPriceTextView.setTextColor(Color.BLACK);
                    } else {
                        lowPriceTextView.setTextColor(Color.RED);
                    }

                    if(maxPrice < prevPrice) {
                        highPriceTextView.setTextColor(Color.BLUE);
                    } else if (maxPrice == prevPrice) {
                        highPriceTextView.setTextColor(Color.BLACK);
                    } else {
                        highPriceTextView.setTextColor(Color.RED);
                    }

                    coinPriceTextView.setText(String.format("%,.0f", lastPrice));

                    Set<String> askKeys = ask.keySet();
                    Set<String> bidKeys = bid.keySet();

                    boolean USD = basicCoin.getExchange().equals("poloniex") ? true : false;
                    boolean TRON = basicCoin.getCoinName().equals("TRON") ? true : false;

                    int idx=0;
                    double total_ask_qnty = 0;
                    ColorPrice[] askPriceItems = new ColorPrice[10];
                    for( String askKey : askKeys ) {
                        int textColor = Color.BLACK;
                        if(prevPrice < Double.parseDouble(askKey))textColor = Color.RED;
                        else if (prevPrice > Double.parseDouble(askKey))textColor = Color.BLUE;
                        double amount = ask.get(askKey);
                        total_ask_qnty += amount;
                        askPriceItems[idx] = new ColorPrice(askKey, String.valueOf(amount), textColor);
                        idx ++;
                    }

                    for (int i=0; i<10; i++) {
                        if (askPriceItems[i] == null) askPriceItems[i] = new ColorPrice("0", "0", Color.BLACK);
                    }
                    Arrays.sort(askPriceItems);
                    askPriceLayout.setAllPrice(askPriceItems);
                    askAmountLayout.setAllAmount(askPriceItems);

                    idx=0;
                    double total_bid_qnty = 0;
                    ColorPrice[] bidPriceItems = new ColorPrice[10];
                    for( String bidKey : bidKeys ) {
                        int textColor = Color.BLACK;
                        if(prevPrice < Double.parseDouble(bidKey))textColor = Color.RED;
                        else if (prevPrice > Double.parseDouble(bidKey))textColor = Color.BLUE;
                        double amount = bid.get(bidKey);
                        total_bid_qnty += amount;
                        bidPriceItems[idx] = new ColorPrice(bidKey, String.valueOf(amount), textColor);
                        idx ++;
                    }

                    for (int i=0; i<10; i++) {
                        if (bidPriceItems[i] == null) bidPriceItems[i] = new ColorPrice("0", "0", Color.BLACK);
                    }
                    Arrays.sort(bidPriceItems);
                    bidPriceLayout.setAllPrice(bidPriceItems);
                    bidAmountLayout.setAllAmount(bidPriceItems);

                    for(int i=0; i<10; i++) {
                        if (Double.parseDouble(bidPriceItems[i].getContent()) == lastPrice) {
                            bidPriceLayout.setBorder(i);
                            askPriceLayout.setBorder(-1);
                            break;
                        } else if (Double.parseDouble(askPriceItems[i].getContent()) == lastPrice) {
                            askPriceLayout.setBorder(i);
                            bidPriceLayout.setBorder(-1);
                            break;
                        }
                    }

                    totalLeftQnty.setText(String.format("%.6f", total_ask_qnty));
                    totalRightQnty.setText(String.format("%.6f", total_bid_qnty));

                    String changeValue = String.format("%,.02f", lastPrice - prevPrice);
                    String changeRate = String.format("%,.2f", getChangeRate(lastPrice, prevPrice));

                    volumeTextView.setText(String.format("%,.3f", volume));
                    if(!USD) {
                        if(TRON) {
                            startPriceTextView.setText(String.format("%,.02f", prevPrice));
                            highPriceTextView.setText(String.format("%,.02f", maxPrice));
                            lowPriceTextView.setText(String.format("%,.02f", minPrice));
                            coinPriceTextView.setText(String.format("%,.02f", lastPrice) + " 원");
                            changeValue = String.format("%,.02f", lastPrice - prevPrice);
                        }
                        else {
                            startPriceTextView.setText(String.format("%,.0f", prevPrice));
                            highPriceTextView.setText(String.format("%,.0f", maxPrice));
                            lowPriceTextView.setText(String.format("%,.0f", minPrice));
                            coinPriceTextView.setText(String.format("%,.0f", lastPrice) + " 원");
                            changeValue = String.format("%,.0f", lastPrice - prevPrice);
                        }
                    }
                    else {
                        startPriceTextView.setText(String.format("%,.3f", prevPrice));
                        highPriceTextView.setText(String.format("%,.3f", maxPrice));
                        lowPriceTextView.setText(String.format("%,.3f", minPrice));
                        coinPriceTextView.setText("$ " + String.format("%,.3f", lastPrice));
                        changeValue = String.format("%,.3f", lastPrice - prevPrice);
                    }


                    int triangleSource = 0;
                    if(lastPrice < prevPrice) {
                        coinPriceTextView.setTextColor(Color.BLUE);
                        diffRateTextView.setTextColor(Color.BLUE);
                        triangleSource = R.drawable.blue_triangle;
                    } else if (lastPrice == prevPrice) {
                        coinPriceTextView.setTextColor(Color.BLACK);
                        diffRateTextView.setTextColor(Color.BLACK);
                    } else {
                        coinPriceTextView.setTextColor(Color.RED);
                        diffRateTextView.setTextColor(Color.RED);
                        triangleSource = R.drawable.red_triangle;
                        changeValue = '+' + changeValue;
                        changeRate = '+' + changeRate;
                    }
                    diffRateTextView.setText(changeValue + " (" + changeRate + "%)");
                    orderbookTriangle.setImageResource(triangleSource);

                }
                else {
                    CommonMessage.getInstance().displaySnackbar("서버연결에 실패했습니다.", thisFragment, CommonMessage.ERROR_MESSAGE_TYPE);
                }
            }

            @Override
            public void onFailure(Call<OrderBookDataView> call, Throwable t) {

            }
        };


        Button searchButton = (Button) v.findViewById(R.id.orderbook_search_button);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final MaterialSimpleListAdapter adapter = new MaterialSimpleListAdapter(new MaterialSimpleListAdapter.Callback() {
                    @Override
                    public void onMaterialListItemSelected(MaterialDialog dialog, int index, MaterialSimpleListItem item) {
                        // TODO
                    }
                });

                adapter.add(new MaterialSimpleListItem.Builder(getContext())
                        .content("username@gmail.com")
                        .icon(R.drawable.ic_btc)
                        .backgroundColor(Color.WHITE)
                        .build());
                adapter.add(new MaterialSimpleListItem.Builder(getContext())
                        .content("user02@gmail.com")
                        .icon(R.drawable.ic_xrp)
                        .backgroundColor(Color.WHITE)
                        .build());
                adapter.add(new MaterialSimpleListItem.Builder(getContext())
                        .content(R.string.bithumb)
                        .icon(R.drawable.ic_eth)
                        .backgroundColor(Color.WHITE)
                        .build());


                new MaterialDialog.Builder(getContext())
                        .title("거래소 선택")
                        .adapter(adapter, null)
                        .show();
            }
        });

        searchButton.setTypeface(MainActivity.materialIconFont);



        //Defatul value
        if (this.basicCoin == null ) {
            setOrderBookCoin(new BasicCoin("bithumb", "BTC"));
        }

        return v;
    }


    private String convertKRW(double value) {
        return String.format("%,.0f", value);
    }

    private String convertUSD(double value) {
        return String.format("%.06f", value);
    }


    public void setTextKRW(TextView textView, double value) {
        textView.setText(String.format("%,.0f", value));
    }

    public void setTextKRWFloat(TextView textView, double value) {
        textView.setText(String.format("%,.02f", value));
    }

    public void setTextUSD(TextView textView, double value) {
        textView.setText(String.format("%.06f", value));
    }

    private void blinkAnimation(View view, int duration, int repeat) {
        if(mAnimation == null) { mAnimation = new AlphaAnimation(1,0); }
        mAnimation.setDuration(duration);
        mAnimation.setInterpolator(new LinearInterpolator());
        mAnimation.setRepeatCount(repeat);
        mAnimation.setRepeatMode(Animation.REVERSE);
        view.startAnimation(mAnimation);
    }


    public void setOrderBookCoin(BasicCoin basicCoin) {
        this.basicCoin = basicCoin;

        if(basicCoin != null) {
            coinNameTextView.setText(basicCoin.getCoinName());
            exchangeNameTextView.setText(MainActivity.exchangeToKorean(basicCoin.getExchange()));
            coinIcon.setImageResource(MainActivity.getCoinIcon(basicCoin.getCoinName()));
            if(timer != null) {
                timer.cancel();
                timer = null;
            }

            timer = new Timer();
            doSomethingRepeatedly();
        }
    }

    private void doSomethingRepeatedly() {
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                RestfulApi.getInstance().getOrderBook(basicCoin, orderbookCallback);
//                RestfulApi.getInstance().getTradeHistory(basicCoin, tradeHistoryCallback);
                Log.d("seongenie", "orderbook network call!!");
                Log.d("seongenie", "tradeHistory network call!!");
            }
        }, 0, REFRESH_INTERVAL);
    }

    @Override
    public void onResume() {
        super.onResume();
        setOrderBookCoin(basicCoin);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private double getChangeRate(double lastPrice, double firstPrice) {
        double diffence = lastPrice - firstPrice;
        return firstPrice != 0 ? diffence * 100 / firstPrice : 0;
    }

    private class TradeAdapter extends BaseAdapter {
        private ArrayList<TradeHistory> item;
        private TradeHistory temp;

        public TradeAdapter (ArrayList<TradeHistory> item) {
            this.item = item;
        }

        public View getView(final int position, View convertView, ViewGroup parent) {

            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.item_tradehistory, null);
            }

            temp = item.get(position);
            TextView price = (TextView) v.findViewById(R.id.trade_price);
            TextView qnty = (TextView) v.findViewById(R.id.trade_qnty);

            price.setText(String.format("%,.0f", temp.getPrice()));
            qnty.setText(String.format("%.4f", temp.getQnty()));

            if (position % 2 == 0) {
//                v.setBackgroundColor(R.color.message_ok);
            }

            return v;
        }

        @Override
        public long getItemId(int position) {
            return position ;
        }

        @Override
        public TradeHistory getItem(int position) {
            return item.get(position) ;
        }

        @Override
        public int getCount() {
            return item.size();
        }

        public void addItem(TradeHistory history) {
            if(item.size() == 0 || item.get(0).getIndex() < history.getIndex()) {
                item.add(0, history);
                if (item.size() > 40) {
                    for(int i=40; i<item.size(); i++){
                        item.remove(i);
                    }
                }
                this.notifyDataSetChanged();
            } else return;
        }
    }
}