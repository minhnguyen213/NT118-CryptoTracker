package com.example.cryptotracker;

public class PortfolioItem {
    private String symbol;
    private double amount;
    private double currentPrice;

    public PortfolioItem(String symbol, double amount) {
        this.symbol = symbol;
        this.amount = amount;
        this.currentPrice = 0.0;
    }

    public String getSymbol() { return symbol; }
    public double getAmount() { return amount; }
    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }
}
