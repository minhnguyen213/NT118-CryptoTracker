
package com.example.cryptotracker;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class TrendingAdapter extends RecyclerView.Adapter<TrendingAdapter.TrendingViewHolder> {

    private List<CryptoCoin> coinList;

    public TrendingAdapter(List<CryptoCoin> coinList) {
        this.coinList = coinList;
    }

    public void updateData(List<CryptoCoin> newList) {
        this.coinList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TrendingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trending, parent, false);
        return new TrendingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrendingViewHolder holder, int position) {
        CryptoCoin coin = coinList.get(position);

        holder.tvTrendingSymbol.setText(coin.getSymbol().toUpperCase());

        if (coin.getCurrentPrice() < 0.01) {
            holder.tvTrendingPrice.setText(
                    String.format("$%.6f", coin.getCurrentPrice())
            );
        } else {
            holder.tvTrendingPrice.setText(
                    String.format("$%,.2f", coin.getCurrentPrice())
            );
        }

        double change = coin.getPriceChangePercentage24h();

        if (change > 0) {
            holder.tvTrendingChange.setText(
                    String.format("+%.2f%%", change)
            );
            holder.tvTrendingChange.setTextColor(
                    Color.parseColor("#00C087")
            );
            setChangeBackground(holder.tvTrendingChange, "#2630C087");

        } else if (change < 0) {
            holder.tvTrendingChange.setText(
                    String.format("%.2f%%", change)
            );
            holder.tvTrendingChange.setTextColor(
                    Color.parseColor("#F6465D")
            );
            setChangeBackground(holder.tvTrendingChange, "#26F6465D");

        } else {
            holder.tvTrendingChange.setText("0.00%");
            holder.tvTrendingChange.setTextColor(
                    Color.parseColor("#929EAE")
            );
            setChangeBackground(holder.tvTrendingChange, "#26293645");
        }

        Glide.with(holder.itemView.getContext())
                .load(coin.getImageUrl())
                .placeholder(R.drawable.ic_market)
                .into(holder.ivTrendingLogo);

        holder.itemView.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("coinId", coin.getId());
            bundle.putString("coinName", coin.getName());
            bundle.putString("coinSymbol", coin.getSymbol().toUpperCase());
            bundle.putDouble("coinPrice", coin.getCurrentPrice());
            bundle.putString("coinImage", coin.getImageUrl());

            Navigation.findNavController(v)
                    .navigate(R.id.detailFragment, bundle);
        });
    }

    private void setChangeBackground(TextView textView, String color) {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setColor(Color.parseColor(color));
        background.setCornerRadius(6 * textView.getResources()
                .getDisplayMetrics().density);
        textView.setBackground(background);
    }

    @Override
    public int getItemCount() {
        return coinList == null ? 0 : coinList.size();
    }

    static class TrendingViewHolder extends RecyclerView.ViewHolder {

        ImageView ivTrendingLogo;
        TextView tvTrendingSymbol, tvTrendingPrice, tvTrendingChange;

        public TrendingViewHolder(@NonNull View itemView) {
            super(itemView);

            ivTrendingLogo = itemView.findViewById(R.id.ivTrendingLogo);
            tvTrendingSymbol = itemView.findViewById(R.id.tvTrendingSymbol);
            tvTrendingPrice = itemView.findViewById(R.id.tvTrendingPrice);
            tvTrendingChange = itemView.findViewById(R.id.tvTrendingChange);
        }
    }
}
