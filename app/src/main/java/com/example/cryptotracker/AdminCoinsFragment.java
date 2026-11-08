package com.example.cryptotracker;

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
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminCoinsFragment extends Fragment {

    private RecyclerView rvAdminCoins;
    private SwipeRefreshLayout swipeRefreshCoins;
    private CryptoAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_coins, container, false);

        rvAdminCoins = view.findViewById(R.id.rvAdminCoins);
        swipeRefreshCoins = view.findViewById(R.id.swipeRefreshCoins);

        rvAdminCoins.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CryptoAdapter(new ArrayList<>(), null, null);
        rvAdminCoins.setAdapter(adapter);

        swipeRefreshCoins.setOnRefreshListener(this::fetchCoins);

        fetchCoins();

        return view;
    }

    private void fetchCoins() {
        swipeRefreshCoins.setRefreshing(true);
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        Call<List<CryptoCoin>> call = apiService.getCoins("usd", "market_cap_desc", 50, 1, false);

        call.enqueue(new Callback<List<CryptoCoin>>() {
            @Override
            public void onResponse(Call<List<CryptoCoin>> call, Response<List<CryptoCoin>> response) {
                swipeRefreshCoins.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    adapter.updateData(response.body());
                } else {
                    Toast.makeText(getContext(), "Không thể tải danh sách coin", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<CryptoCoin>> call, Throwable t) {
                swipeRefreshCoins.setRefreshing(false);
                Toast.makeText(getContext(), "Mất kết nối mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
