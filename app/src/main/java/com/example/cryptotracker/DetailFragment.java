package com.example.cryptotracker;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;

import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class DetailFragment extends Fragment {

    private WebSocket detailWebSocket;
    private OkHttpClient webSocketClient;

    private TextView tvDetailPrice;
    private double lastPrice = 0.0;

    private static final int COLOR_PRIMARY = Color.parseColor("#F0F3F7");
    private static final int COLOR_GREEN = Color.parseColor("#00C087");
    private static final int COLOR_RED = Color.parseColor("#F6465D");

    @SuppressLint("SetJavaScriptEnabled")
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_detail, container, false);

        ImageView btnBack = view.findViewById(R.id.btnBack);
        ImageView ivDetailLogo = view.findViewById(R.id.ivDetailLogo);
        TextView tvDetailName = view.findViewById(R.id.tvDetailName);
        TextView tvDetailSymbol = view.findViewById(R.id.tvDetailSymbol);

        tvDetailPrice = view.findViewById(R.id.tvDetailPrice);

        WebView wvChart = view.findViewById(R.id.wvChart);

        btnBack.setOnClickListener(v ->
                Navigation.findNavController(v).navigateUp()
        );

        if (getArguments() != null) {
            String name = getArguments().getString("coinName");
            String symbol = getArguments().getString("coinSymbol");
            lastPrice = getArguments().getDouble("coinPrice");
            String image = getArguments().getString("coinImage");

            tvDetailName.setText(name);
            tvDetailSymbol.setText(symbol);

            tvDetailPrice.setText(
                    String.format("$%,.2f", lastPrice)
            );
            tvDetailPrice.setTextColor(COLOR_PRIMARY);

            Glide.with(this)
                    .load(image)
                    .into(ivDetailLogo);

            setupTradingViewChart(wvChart, symbol);

            startSingleCoinWebSocket(symbol);
        }

        return view;
    }

    private void setupTradingViewChart(WebView wvChart, String symbol) {

        WebSettings webSettings = wvChart.getSettings();

        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        wvChart.setBackgroundColor(Color.parseColor("#0B0E11"));
        wvChart.setWebViewClient(new WebViewClient());

        String chartSymbol = "BINANCE:" + symbol + "USDT";

        String htmlData =
                "<!DOCTYPE html><html><head>" +
                        "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\"/>" +

                        "<style>" +
                        "body, html {" +
                        "margin: 0;" +
                        "padding: 0;" +
                        "height: 100%;" +
                        "overflow: hidden;" +
                        "background-color: #0B0E11;" +
                        "}" +
                        "</style>" +

                        "</head><body>" +

                        "<div class=\"tradingview-widget-container\" style=\"height:100%;width:100%\">" +
                        "<div id=\"tradingview_chart\" style=\"height:100%;width:100%\"></div>" +

                        "<script type=\"text/javascript\" src=\"https://s3.tradingview.com/tv.js\"></script>" +

                        "<script type=\"text/javascript\">" +
                        "new TradingView.widget({" +
                        "\"autosize\": true," +
                        "\"symbol\": \"" + chartSymbol + "\"," +
                        "\"interval\": \"15\"," +
                        "\"timezone\": \"Asia/Ho_Chi_Minh\"," +
                        "\"theme\": \"dark\"," +
                        "\"style\": \"1\"," +
                        "\"locale\": \"vi\"," +
                        "\"enable_publishing\": false," +
                        "\"hide_top_toolbar\": false," +
                        "\"hide_legend\": true," +
                        "\"save_image\": false," +
                        "\"container_id\": \"tradingview_chart\"" +
                        "});" +
                        "</script>" +

                        "</div></body></html>";

        wvChart.loadDataWithBaseURL(
                "https://www.tradingview.com",
                htmlData,
                "text/html",
                "UTF-8",
                null
        );
    }

    private void startSingleCoinWebSocket(String symbol) {

        webSocketClient = new OkHttpClient();

        String streamSymbol = symbol.toLowerCase() + "usdt";

        String wsUrl =
                "wss://stream.binance.com:9443/ws/" +
                        streamSymbol + "@miniTicker";

        Request request = new Request.Builder()
                .url(wsUrl)
                .build();

        detailWebSocket = webSocketClient.newWebSocket(
                request,
                new WebSocketListener() {

                    @Override
                    public void onMessage(
                            @NonNull WebSocket webSocket,
                            @NonNull String text
                    ) {
                        try {
                            JSONObject obj = new JSONObject(text);

                            double newPrice = obj.getDouble("c");

                            if (newPrice == lastPrice) return;

                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {

                                    if (tvDetailPrice == null) return;

                                    if (newPrice > lastPrice) {
                                        tvDetailPrice.setTextColor(COLOR_GREEN);
                                    } else {
                                        tvDetailPrice.setTextColor(COLOR_RED);
                                    }

                                    tvDetailPrice.setText(
                                            String.format("$%,.2f", newPrice)
                                    );

                                    lastPrice = newPrice;

                                    tvDetailPrice.postDelayed(() -> {
                                        if (tvDetailPrice != null) {
                                            tvDetailPrice.setTextColor(COLOR_PRIMARY);
                                        }
                                    }, 500);
                                });
                            }

                        } catch (Exception e) {
                            Log.e(
                                    "DetailFragment",
                                    "Lỗi phân tích JSON: " + e.getMessage()
                            );
                        }
                    }
                }
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (detailWebSocket != null) {
            detailWebSocket.cancel();
            detailWebSocket = null;
        }

        if (webSocketClient != null) {
            webSocketClient.dispatcher().cancelAll();
            webSocketClient = null;
        }

        tvDetailPrice = null;
    }
}
