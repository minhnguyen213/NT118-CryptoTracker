package com.example.cryptotracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class PortfolioAdapter extends RecyclerView.Adapter<PortfolioAdapter.PortfolioViewHolder> {

    private final List<PortfolioItem> list;

    public PortfolioAdapter(List<PortfolioItem> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public PortfolioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_portfolio, parent, false);
        return new PortfolioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PortfolioViewHolder holder, int position) {
        PortfolioItem item = list.get(position);
        holder.tvPortfolioSymbol.setText(item.getSymbol());
        holder.tvPortfolioAmount.setText(String.format("Số lượng: %.4f", item.getAmount()));
        double val = item.getAmount() * item.getCurrentPrice();
        holder.tvPortfolioVal.setText(String.format("$%,.2f", val));
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    static class PortfolioViewHolder extends RecyclerView.ViewHolder {
        TextView tvPortfolioSymbol, tvPortfolioAmount, tvPortfolioVal;

        public PortfolioViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPortfolioSymbol = itemView.findViewById(R.id.tvPortfolioSymbol);
            tvPortfolioAmount = itemView.findViewById(R.id.tvPortfolioAmount);
            tvPortfolioVal = itemView.findViewById(R.id.tvPortfolioVal);
        }
    }
}
