package com.example.cryptotracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private Button btnAll, btnFavorites;
    private SwipeRefreshLayout swipeRefreshLayout;

    private List<CryptoCoin> fullCoinList = new ArrayList<>();
    private Set<String> favoriteIds = new HashSet<>();
    private boolean showingFavoritesOnly = false;

    private WebSocket binanceWebSocket;
    private OkHttpClient client;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_market, container, false);

        rvCryptoList = view.findViewById(R.id.rvCryptoList);
        etSearch = view.findViewById(R.id.etSearch);
        btnAll = view.findViewById(R.id.btnAll);
        btnFavorites = view.findViewById(R.id.btnFavorites);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        swipeRefreshLayout.setEnabled(false);

        // Load favorites from SharedPreferences
        loadFavorites();

        rvCryptoList.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CryptoAdapter(new ArrayList<>(), favoriteIds, (coin, isFavorite) -> {
            if (isFavorite) {
                favoriteIds.add(coin.getId());
                Toast.makeText(getContext(), "Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show();
            } else {
                favoriteIds.remove(coin.getId());
                Toast.makeText(getContext(), "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show();
            }
            saveFavorites();

            if (showingFavoritesOnly) {
                filterList(etSearch.getText().toString());
            }
        });
        rvCryptoList.setAdapter(adapter);

        btnAll.setOnClickListener(v -> {
            showingFavoritesOnly = false;
            btnAll.setBackgroundColor(android.graphics.Color.parseColor("#00C087"));
            btnAll.setTextColor(android.graphics.Color.WHITE);
            btnFavorites.setBackgroundColor(android.graphics.Color.parseColor("#CCCCCC"));
            btnFavorites.setTextColor(android.graphics.Color.BLACK);
            filterList(etSearch.getText().toString());
        });

        btnFavorites.setOnClickListener(v -> {
            showingFavoritesOnly = true;
            btnFavorites.setBackgroundColor(android.graphics.Color.parseColor("#00C087"));
            btnFavorites.setTextColor(android.graphics.Color.WHITE);
            btnAll.setBackgroundColor(android.graphics.Color.parseColor("#CCCCCC"));
            btnAll.setTextColor(android.graphics.Color.BLACK);
            filterList(etSearch.getText().toString());
        });

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

    private void loadFavorites() {
        if (getContext() == null) return;
        SharedPreferences userPrefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String userId = userPrefs.getString("user_id", "default_user");

        SharedPreferences prefs = requireContext().getSharedPreferences("watchlist_prefs", Context.MODE_PRIVATE);
        Set<String> savedFavs = prefs.getStringSet("favs_" + userId, null);
        if (savedFavs != null) {
            favoriteIds = new HashSet<>(savedFavs);
        } else {
            favoriteIds.clear();
        }
    }

    private void saveFavorites() {
        if (getContext() == null) return;
        SharedPreferences userPrefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String userId = userPrefs.getString("user_id", "default_user");

        SharedPreferences prefs = requireContext().getSharedPreferences("watchlist_prefs", Context.MODE_PRIVATE);
        prefs.edit().putStringSet("favs_" + userId, favoriteIds).apply();
    }

    private void fetchInitialDataFromCoinGecko() {
        swipeRefreshLayout.setRefreshing(true);
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        Call<List<CryptoCoin>> call = apiService.getCoins("usd", "market_cap_desc", 100, 1, false);

        call.enqueue(new Callback<List<CryptoCoin>>() {
            @Override
            public void onResponse(@NonNull Call<List<CryptoCoin>> call, @NonNull Response<List<CryptoCoin>> response) {
                swipeRefreshLayout.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    fullCoinList = response.body();
                    filterList(etSearch.getText().toString());
                    startBinanceWebSocket();
                } else if (response.code() == 429) {
                    Toast.makeText(getContext(), "CoinGecko bị kẹt Limit, xin chờ 1 lát.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<CryptoCoin>> call, @NonNull Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(getContext(), "Mất kết nối mạng!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startBinanceWebSocket() {
        client = new OkHttpClient();
        Request request = new Request.Builder().url("wss://stream.binance.com:9443/ws/!miniTicker@arr").build();

        binanceWebSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
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
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable okhttp3.Response response) {
                Log.e("WebSocket", "Mất kết nối WebSocket: " + t.getMessage());
            }
        });
    }

    private void filterList(String text) {
        List<CryptoCoin> filteredList = new ArrayList<>();
        for (CryptoCoin coin : fullCoinList) {
            boolean matchesSearch = coin.getName().toLowerCase().contains(text.toLowerCase()) ||
                    coin.getSymbol().toLowerCase().contains(text.toLowerCase());
            boolean matchesTab = !showingFavoritesOnly || favoriteIds.contains(coin.getId());

            if (matchesSearch && matchesTab) {
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
