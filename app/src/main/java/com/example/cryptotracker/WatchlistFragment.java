package com.example.cryptotracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WatchlistFragment extends Fragment {

    private CryptoAdapter adapter;
    private SwipeRefreshLayout swipeRefreshWatchlist;
    private List<CryptoCoin> allCoins = new ArrayList<>();
    private Set<String> favoriteIds = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_watchlist, container, false);

        RecyclerView rvWatchlist = view.findViewById(R.id.rvWatchlist);
        swipeRefreshWatchlist = view.findViewById(R.id.swipeRefreshWatchlist);

        rvWatchlist.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CryptoAdapter(new ArrayList<>(), favoriteIds, (coin, isFavorite) -> {
            if (!isFavorite) {
                favoriteIds.remove(coin.getId());
                saveFavorites();
                loadWatchlistCoins();
                Toast.makeText(getContext(), "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show();
            }
        });
        rvWatchlist.setAdapter(adapter);

        swipeRefreshWatchlist.setOnRefreshListener(this::fetchData);

        fetchData();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFavorites();
        if (!allCoins.isEmpty()) {
            loadWatchlistCoins();
        }
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

    private void fetchData() {
        swipeRefreshWatchlist.setRefreshing(true);
        loadFavorites();

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        Call<List<CryptoCoin>> call = apiService.getCoins("usd", "market_cap_desc", 100, 1, false);

        call.enqueue(new Callback<List<CryptoCoin>>() {
            @Override
            public void onResponse(@NonNull Call<List<CryptoCoin>> call, @NonNull Response<List<CryptoCoin>> response) {
                swipeRefreshWatchlist.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    allCoins = response.body();
                    loadWatchlistCoins();
                } else {
                    Toast.makeText(getContext(), "Không thể tải dữ liệu", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<CryptoCoin>> call, @NonNull Throwable t) {
                swipeRefreshWatchlist.setRefreshing(false);
                Toast.makeText(getContext(), "Mất kết nối mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadWatchlistCoins() {
        loadFavorites();
        List<CryptoCoin> watchlistCoins = new ArrayList<>();
        for (CryptoCoin coin : allCoins) {
            if (favoriteIds.contains(coin.getId())) {
                watchlistCoins.add(coin);
            }
        }
        adapter.updateFavorites(favoriteIds);
        adapter.updateData(watchlistCoins);
    }
}
