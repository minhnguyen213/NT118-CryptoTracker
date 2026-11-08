package com.example.cryptotracker;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.tabs.TabLayout;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HomeFragment extends Fragment {

    private TabLayout tabLayoutHome;
    private TextView tvFearGreedValue, tvFearGreedText;
    private TextView tvGlobalMarketCap, tvGlobalVolume, tvAltcoinIndex;
    private FearGreedGaugeView gaugeFearGreed;
    private ProgressBar pbAltcoinSeason;
    private RecyclerView rvGainers, rvLosers;
    private TrendingAdapter gainersAdapter, losersAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        tabLayoutHome = view.findViewById(R.id.tabLayoutHome);
        tvFearGreedValue = view.findViewById(R.id.tvFearGreedValue);
        tvFearGreedText = view.findViewById(R.id.tvFearGreedText);
        rvGainers = view.findViewById(R.id.rvGainers);
        rvLosers = view.findViewById(R.id.rvLosers);

        tvGlobalMarketCap = view.findViewById(R.id.tvGlobalMarketCap);
        tvGlobalVolume = view.findViewById(R.id.tvGlobalVolume);
        tvAltcoinIndex = view.findViewById(R.id.tvAltcoinIndex);
        gaugeFearGreed = view.findViewById(R.id.gaugeFearGreed);
        pbAltcoinSeason = view.findViewById(R.id.pbAltcoinSeason);

        setupTabs();
        setupRecyclerViews();

        fetchMarketData();
        fetchFearAndGreedIndex();
        fetchGlobalMarketData();
        setupAltcoinSeasonMock();

        return view;
    }

    private void setupTabs() {
        tabLayoutHome.addTab(tabLayoutHome.newTab().setText("Overview"));
        tabLayoutHome.addTab(tabLayoutHome.newTab().setText("Liq. Map"));
        tabLayoutHome.addTab(tabLayoutHome.newTab().setText("Liquidation"));
        tabLayoutHome.addTab(tabLayoutHome.newTab().setText("Long/Short"));
        tabLayoutHome.addTab(tabLayoutHome.newTab().setText("Funding"));

        tabLayoutHome.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (!tab.getText().toString().equals("Overview")) {
                    Toast.makeText(getContext(), tab.getText() + " đang được phát triển...", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerViews() {
        rvGainers.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        gainersAdapter = new TrendingAdapter(new ArrayList<>());
        rvGainers.setAdapter(gainersAdapter);

        rvLosers.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        losersAdapter = new TrendingAdapter(new ArrayList<>());
        rvLosers.setAdapter(losersAdapter);
    }

    private void fetchMarketData() {
        SharedViewModel viewModel = new androidx.lifecycle.ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        viewModel.getCoinList().observe(getViewLifecycleOwner(), allCoins -> {
            if (allCoins != null && !allCoins.isEmpty()) {
                // Top 8 Gainers
                List<CryptoCoin> sortedForGainers = new ArrayList<>(allCoins);
                Collections.sort(sortedForGainers, (c1, c2) ->
                        Double.compare(c2.getPriceChangePercentage24h(), c1.getPriceChangePercentage24h())
                );
                List<CryptoCoin> gainers = new ArrayList<>();
                for (int i = 0; i < Math.min(8, sortedForGainers.size()); i++) {
                    gainers.add(sortedForGainers.get(i));
                }
                gainersAdapter.updateData(gainers);

                // Top 8 Losers
                List<CryptoCoin> sortedForLosers = new ArrayList<>(allCoins);
                Collections.sort(sortedForLosers, (c1, c2) ->
                        Double.compare(c1.getPriceChangePercentage24h(), c2.getPriceChangePercentage24h())
                );
                List<CryptoCoin> losers = new ArrayList<>();
                for (int i = 0; i < Math.min(8, sortedForLosers.size()); i++) {
                    losers.add(sortedForLosers.get(i));
                }
                losersAdapter.updateData(losers);
            }
        });
    }

    private void fetchFearAndGreedIndex() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url("https://api.alternative.me/fng/").build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("HomeFragment", "Lỗi tải F&G: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        JSONObject dataObj = jsonObject.getJSONArray("data").getJSONObject(0);

                        String value = dataObj.getString("value");
                        String classification = dataObj.getString("value_classification");

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                tvFearGreedValue.setText(value);
                                tvFearGreedText.setText(classification);

                                int score = Integer.parseInt(value);
                                if (gaugeFearGreed != null) {
                                    gaugeFearGreed.setScore(score);
                                }

                                if (score <= 25) tvFearGreedText.setTextColor(Color.parseColor("#F44336")); // Extreme Fear
                                else if (score <= 45) tvFearGreedText.setTextColor(Color.parseColor("#FF9800")); // Fear
                                else if (score <= 55) tvFearGreedText.setTextColor(Color.parseColor("#9E9E9E")); // Neutral
                                else if (score <= 75) tvFearGreedText.setTextColor(Color.parseColor("#8BC34A")); // Greed
                                else tvFearGreedText.setTextColor(Color.parseColor("#4CAF50")); // Extreme Greed
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void fetchGlobalMarketData() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url("https://api.coingecko.com/api/v3/global").build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("HomeFragment", "Lỗi tải Global Data: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        JSONObject dataObj = jsonObject.getJSONObject("data");

                        double totalMarketCap = dataObj.getJSONObject("total_market_cap").getDouble("usd");
                        double totalVolume = dataObj.getJSONObject("total_volume").getDouble("usd");

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                tvGlobalMarketCap.setText(formatVolume(totalMarketCap));
                                tvGlobalVolume.setText(formatVolume(totalVolume));
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void setupAltcoinSeasonMock() {
        int mockScore = 39;
        tvAltcoinIndex.setText(mockScore + " / 100");
        pbAltcoinSeason.setProgress(mockScore);
    }

    private String formatVolume(double volume) {
        if (volume >= 1_000_000_000_000L) {
            return String.format("$%.2fT", volume / 1_000_000_000_000L);
        } else if (volume >= 1_000_000_000) {
            return String.format("$%.2fB", volume / 1_000_000_000);
        } else if (volume >= 1_000_000) {
            return String.format("$%.2fM", volume / 1_000_000);
        } else {
            return String.format("$%,.0f", volume);
        }
    }
}