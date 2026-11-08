package com.example.cryptotracker;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PortfolioFragment extends Fragment {

    private TextView tvTotalPortfolioValue;
    private RecyclerView rvPortfolio;
    private SwipeRefreshLayout swipeRefreshPortfolio;
    private Button btnAddAsset;
    private PortfolioAdapter adapter;
    private final List<PortfolioItem> portfolioList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_portfolio, container, false);

        tvTotalPortfolioValue = view.findViewById(R.id.tvTotalPortfolioValue);
        rvPortfolio = view.findViewById(R.id.rvPortfolio);
        swipeRefreshPortfolio = view.findViewById(R.id.swipeRefreshPortfolio);
        btnAddAsset = view.findViewById(R.id.btnAddAsset);

        rvPortfolio.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PortfolioAdapter(portfolioList);
        rvPortfolio.setAdapter(adapter);

        swipeRefreshPortfolio.setOnRefreshListener(this::loadPortfolioData);
        btnAddAsset.setOnClickListener(v -> showAddAssetDialog());

        loadPortfolioData();

        return view;
    }

    private void loadPortfolioData() {
        if (getContext() == null) return;
        swipeRefreshPortfolio.setRefreshing(true);

        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String holdingsJson = prefs.getString("portfolio_holdings", "{}");

        portfolioList.clear();
        try {
            JSONObject obj = new JSONObject(holdingsJson);
            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String symbol = keys.next();
                double amount = obj.optDouble(symbol, 0);
                portfolioList.add(new PortfolioItem(symbol, amount));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        Call<List<CryptoCoin>> call = apiService.getCoins("usd", "market_cap_desc", 100, 1, false);

        call.enqueue(new Callback<List<CryptoCoin>>() {
            @Override
            public void onResponse(@NonNull Call<List<CryptoCoin>> call, @NonNull Response<List<CryptoCoin>> response) {
                swipeRefreshPortfolio.setRefreshing(false);
                double totalValue = 0;
                if (response.isSuccessful() && response.body() != null) {
                    List<CryptoCoin> coins = response.body();
                    for (PortfolioItem item : portfolioList) {
                        for (CryptoCoin coin : coins) {
                            if (coin.getSymbol().equalsIgnoreCase(item.getSymbol())) {
                                item.setCurrentPrice(coin.getCurrentPrice());
                                totalValue += item.getAmount() * coin.getCurrentPrice();
                                break;
                            }
                        }
                    }
                }
                tvTotalPortfolioValue.setText(String.format("$%,.2f", totalValue));
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(@NonNull Call<List<CryptoCoin>> call, @NonNull Throwable t) {
                swipeRefreshPortfolio.setRefreshing(false);
                tvTotalPortfolioValue.setText("$0.00");
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void showAddAssetDialog() {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);

        final EditText etSymbol = new EditText(getContext());
        etSymbol.setHint("Mã Coin (VD: BTC, ETH)");
        layout.addView(etSymbol);

        final EditText etAmount = new EditText(getContext());
        etAmount.setHint("Số lượng (VD: 0.5)");
        etAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etAmount);

        builder.setTitle("Thêm tài sản mới")
                .setView(layout)
                .setPositiveButton("Thêm", (dialog, which) -> {
                    String symbol = etSymbol.getText().toString().trim().toUpperCase();
                    String amtStr = etAmount.getText().toString().trim();
                    if (!symbol.isEmpty() && !amtStr.isEmpty()) {
                        try {
                            double amount = Double.parseDouble(amtStr);
                            SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
                            String holdingsJson = prefs.getString("portfolio_holdings", "{}");
                            JSONObject obj = new JSONObject(holdingsJson);
                            obj.put(symbol, amount);
                            prefs.edit().putString("portfolio_holdings", obj.toString()).apply();
                            loadPortfolioData();
                            Toast.makeText(getContext(), "Đã thêm thành công!", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(getContext(), "Số lượng không hợp lệ", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
