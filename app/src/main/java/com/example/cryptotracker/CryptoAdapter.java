package com.example.cryptotracker;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CryptoAdapter extends RecyclerView.Adapter<CryptoAdapter.CryptoViewHolder> {

    private List<CryptoCoin> coinList;
    private Map<String, Integer> symbolToIndexMap = new HashMap<>();
    private Map<String, Long> lastUpdateMap = new HashMap<>();

    public CryptoAdapter(List<CryptoCoin> coinList) {
        this.coinList = coinList;
    }

    public void updateData(List<CryptoCoin> newList) {
        this.coinList = newList;
        buildIndexMap();
        notifyDataSetChanged();
    }

    public void setFilteredList(List<CryptoCoin> filteredList) {
        this.coinList = filteredList;
        buildIndexMap();
        notifyDataSetChanged();
    }

    private void buildIndexMap() {
        symbolToIndexMap.clear();
        for (int i = 0; i < coinList.size(); i++) {
            String binanceSymbol = coinList.get(i).getSymbol().toUpperCase() + "USDT";
            symbolToIndexMap.put(binanceSymbol, i);
        }
    }

    public void updatePriceRealTime(String binanceSymbol, double newPrice) {
        Integer index = symbolToIndexMap.get(binanceSymbol);
        if (index != null && index < coinList.size()) {

            long currentTime = System.currentTimeMillis();
            if (lastUpdateMap.containsKey(binanceSymbol)) {
                if (currentTime - lastUpdateMap.get(binanceSymbol) < 1000) {
                    return;
                }
            }
            lastUpdateMap.put(binanceSymbol, currentTime);

            CryptoCoin coin = coinList.get(index);
            double oldPrice = coin.getCurrentPrice();

            if (oldPrice == newPrice) return;

            String payload = newPrice > oldPrice ? "PRICE_UP" : "PRICE_DOWN";

            coin.setCurrentPrice(newPrice);
            notifyItemChanged(index, payload);
        }
    }

    @NonNull
    @Override
    public CryptoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_crypto, parent, false);
        return new CryptoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CryptoViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty()) {
            String payload = (String) payloads.get(0);
            CryptoCoin coin = coinList.get(position);

            holder.tvCoinPrice.setText(String.format("$%,.2f", coin.getCurrentPrice()));

            if ("PRICE_UP".equals(payload)) {
                holder.tvCoinPrice.setTextColor(Color.parseColor("#4CAF50"));
            } else if ("PRICE_DOWN".equals(payload)) {
                holder.tvCoinPrice.setTextColor(Color.parseColor("#F44336"));
            }

            holder.tvCoinPrice.postDelayed(() -> {
                holder.tvCoinPrice.setTextColor(Color.parseColor("#000000"));
            }, 500);

        } else {
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull CryptoViewHolder holder, int position) {
        CryptoCoin coin = coinList.get(position);

        holder.tvCoinName.setText(coin.getName());
        holder.tvCoinSymbol.setText(coin.getSymbol().toUpperCase());
        holder.tvCoinPrice.setText(String.format("$%,.2f", coin.getCurrentPrice()));

        holder.tvCoinPrice.setTextColor(Color.parseColor("#000000"));

        double change = coin.getPriceChangePercentage24h();
        if (change >= 0) {
            holder.tvCoinChange.setText(String.format("+%.2f%%", change));
            holder.tvCoinChange.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            holder.tvCoinChange.setText(String.format("%.2f%%", change));
            holder.tvCoinChange.setTextColor(Color.parseColor("#F44336"));
        }

        Glide.with(holder.itemView.getContext())
                .load(coin.getImageUrl())
                .placeholder(R.drawable.ic_market)
                .into(holder.ivCoinLogo);
        holder.itemView.setOnClickListener(v -> {
            android.os.Bundle bundle = new android.os.Bundle();
            bundle.putString("coinId", coin.getId());
            bundle.putString("coinName", coin.getName());
            bundle.putString("coinSymbol", coin.getSymbol().toUpperCase());
            bundle.putDouble("coinPrice", coin.getCurrentPrice());
            bundle.putString("coinImage", coin.getImageUrl());

            androidx.navigation.Navigation.findNavController(v).navigate(R.id.detailFragment, bundle);
        });
    }

    @Override
    public int getItemCount() {
        return coinList == null ? 0 : coinList.size();
    }

    static class CryptoViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCoinLogo;
        TextView tvCoinName, tvCoinSymbol, tvCoinPrice, tvCoinChange;

        public CryptoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCoinLogo = itemView.findViewById(R.id.ivCoinLogo);
            tvCoinName = itemView.findViewById(R.id.tvCoinName);
            tvCoinSymbol = itemView.findViewById(R.id.tvCoinSymbol);
            tvCoinPrice = itemView.findViewById(R.id.tvCoinPrice);
            tvCoinChange = itemView.findViewById(R.id.tvCoinChange);
        }
    }
}