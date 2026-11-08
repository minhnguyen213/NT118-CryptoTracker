package com.example.cryptotracker;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_market, container, false);

        rvCryptoList = view.findViewById(R.id.rvCryptoList);
        etSearch = view.findViewById(R.id.etSearch);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        swipeRefreshLayout.setEnabled(false);

        rvCryptoList.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CryptoAdapter(new ArrayList<>());
        rvCryptoList.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                filterList(s.toString());
            }
        });

        fetchInitialDataFromCoinGecko();

        return view;
    }

    private void fetchInitialDataFromCoinGecko() {
        SharedViewModel viewModel = new androidx.lifecycle.ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        viewModel.getCoinList().observe(getViewLifecycleOwner(), allCoins -> {
            if (allCoins != null && !allCoins.isEmpty()) {
                swipeRefreshLayout.setRefreshing(false);
                fullCoinList = allCoins;
                adapter.updateData(fullCoinList);

                if (binanceWebSocket == null) {
                    startBinanceWebSocket();
                }
            }
        });
    }
    private void startBinanceWebSocket() {
        client = new OkHttpClient();
        Request request = new Request.Builder().url("wss://stream.binance.com:9443/ws/!miniTicker@arr").build();

        binanceWebSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    JSONArray jsonArray = new JSONArray(text);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        String symbol = obj.getString("s");
                        double price = obj.getDouble("c");

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                adapter.updatePriceRealTime(symbol, price);
                            });
                        }
                    }
                } catch (Exception e) {
                    Log.e("WebSocket", "Lỗi phân tích JSON: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
                Log.e("WebSocket", "Mất kết nối WebSocket: " + t.getMessage());
            }
        });
    }

    private void filterList(String text) {
        List<CryptoCoin> filteredList = new ArrayList<>();
        for (CryptoCoin coin : fullCoinList) {
            if (coin.getName().toLowerCase().contains(text.toLowerCase()) ||
                    coin.getSymbol().toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(coin);
            }
        }
        adapter.setFilteredList(filteredList);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (binanceWebSocket != null) {
            binanceWebSocket.cancel();
        }
    }
}