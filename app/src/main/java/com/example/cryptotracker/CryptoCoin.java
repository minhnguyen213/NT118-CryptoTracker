package com.example.cryptotracker;

import com.google.gson.annotations.SerializedName;

public class CryptoCoin {
    private String id;
    private String symbol;
    private String name;

    @SerializedName("current_price")
    private double currentPrice;

    @SerializedName("price_change_percentage_24h")
    private double priceChangePercentage24h;

    @SerializedName("image")
    private String imageUrl;
    @SerializedName("total_volume")
    private double totalVolume;

    public double getTotalVolume() { return totalVolume; }
    public void setTotalVolume(double totalVolume) { this.totalVolume = totalVolume; }

    public CryptoCoin(String id, String symbol, String name, double currentPrice, double priceChangePercentage24h, String imageUrl) {
        this.id = id;
        this.symbol = symbol;
        this.name = name;
        this.currentPrice = currentPrice;
        this.priceChangePercentage24h = priceChangePercentage24h;
        this.imageUrl = imageUrl;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public double getPriceChangePercentage24h() { return priceChangePercentage24h; }
    public void setPriceChangePercentage24h(double priceChangePercentage24h) { this.priceChangePercentage24h = priceChangePercentage24h; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}