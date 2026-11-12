package com.example.cryptotracker;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class MarketFragment extends Fragment {

    private RecyclerView rvCryptoList;
    private CryptoAdapter adapter;
    private EditText etSearch;
    private SwipeRefreshLayout swipeRefreshLayout;

    private List<CryptoCoin> fullCoinList = new ArrayList<>();

    private WebSocket binanceWebSocket;
    private OkHttpClient client;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_market,
                container,
                false
        );

        // Ánh xạ giao diện
        rvCryptoList = view.findViewById(R.id.rvCryptoList);
        etSearch = view.findViewById(R.id.etSearch);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        // Nút quay về Home
        ImageView btnBackMarket = view.findViewById(R.id.btnBackMarket);

        btnBackMarket.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.homeFragment)
        );

        // Cấu hình danh sách
        swipeRefreshLayout.setEnabled(false);

        rvCryptoList.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        adapter = new CryptoAdapter(new ArrayList<>());
        rvCryptoList.setAdapter(adapter);

        // Tìm kiếm coin
        etSearch.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                filterList(s.toString());
            }
        });

        fetchInitialDataFromCoinGecko();

        return view;
    }

    // Lấy dữ liệu từ SharedViewModel
    private void fetchInitialDataFromCoinGecko() {

        SharedViewModel viewModel = new ViewModelProvider(
                requireActivity()
        ).get(SharedViewModel.class);

        viewModel.getCoinList().observe(
                getViewLifecycleOwner(),
                allCoins -> {

                    if (allCoins != null && !allCoins.isEmpty()) {

                        swipeRefreshLayout.setRefreshing(false);

                        fullCoinList = allCoins;
                        adapter.updateData(fullCoinList);

                        if (binanceWebSocket == null) {
                            startBinanceWebSocket();
                        }
                    }
                }
        );
    }

    // Kết nối Binance WebSocket
    private void startBinanceWebSocket() {

        client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("wss://stream.binance.com:9443/ws/!miniTicker@arr")
                .build();

        binanceWebSocket = client.newWebSocket(
                request,
                new WebSocketListener() {

                    @Override
                    public void onMessage(
                            WebSocket webSocket,
                            String text) {

                        try {
                            JSONArray jsonArray = new JSONArray(text);

                            for (int i = 0; i < jsonArray.length(); i++) {

                                JSONObject obj = jsonArray.getJSONObject(i);

                                String symbol = obj.getString("s");
                                double price = obj.getDouble("c");

                                if (getActivity() != null) {

                                    requireActivity().runOnUiThread(() -> {

                                        if (adapter != null) {
                                            adapter.updatePriceRealTime(
                                                    symbol,
                                                    price
                                            );
                                        }

                                    });
                                }
                            }

                        } catch (Exception e) {
                            Log.e(
                                    "WebSocket",
                                    "Lỗi phân tích JSON: " + e.getMessage()
                            );
                        }
                    }

                    @Override
                    public void onFailure(
                            WebSocket webSocket,
                            Throwable t,
                            okhttp3.Response response) {

                        Log.e(
                                "WebSocket",
                                "Mất kết nối WebSocket: " + t.getMessage()
                        );
                    }
                }
        );
    }

    // Tìm kiếm coin theo tên hoặc symbol
    private void filterList(String text) {

        List<CryptoCoin> filteredList = new ArrayList<>();

        for (CryptoCoin coin : fullCoinList) {

            if (coin.getName().toLowerCase().contains(text.toLowerCase())
                    || coin.getSymbol().toLowerCase().contains(text.toLowerCase())) {

                filteredList.add(coin);
            }
        }

        adapter.setFilteredList(filteredList);
    }

    // Đóng WebSocket khi rời màn hình
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (binanceWebSocket != null) {
            binanceWebSocket.cancel();
            binanceWebSocket = null;
        }

        if (client != null) {
            client.dispatcher().executorService().shutdown();
            client = null;
        }

        adapter = null;
        rvCryptoList = null;
        etSearch = null;
        swipeRefreshLayout = null;
    }
}
