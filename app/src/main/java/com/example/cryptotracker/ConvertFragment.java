package com.example.cryptotracker;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ConvertFragment extends Fragment {

    private Spinner spinnerFrom, spinnerTo;
    private EditText etAmountFrom;
    private TextView tvAmountTo, tvExchangeRate;
    private CardView btnSwap;
    private LinearLayout layoutFiatRates;

    private Map<String, Double> ratesMap = new HashMap<>();
    private List<String> currencyList = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;

    private boolean isDataLoaded = false;

    private final String[] targetFiats = {
            "EUR", "GBP", "JPY", "AUD", "SGD", "CAD"
    };

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(
                R.layout.fragment_convert,
                container,
                false
        );

        // Back button
        ImageView btnBackConvert =
                view.findViewById(R.id.btnBackConvert);

        btnBackConvert.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.homeFragment)
        );

        // Ánh xạ giao diện
        spinnerFrom = view.findViewById(R.id.spinnerFrom);
        spinnerTo = view.findViewById(R.id.spinnerTo);

        etAmountFrom = view.findViewById(R.id.etAmountFrom);

        tvAmountTo = view.findViewById(R.id.tvAmountTo);
        tvExchangeRate = view.findViewById(R.id.tvExchangeRate);

        btnSwap = view.findViewById(R.id.btnSwap);
        layoutFiatRates = view.findViewById(R.id.layoutFiatRates);

        // Spinner adapter
        spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                currencyList
        );

        spinnerFrom.setAdapter(spinnerAdapter);
        spinnerTo.setAdapter(spinnerAdapter);

        setupListeners();
        fetchAllRates();

        return view;
    }

    private void setupListeners() {

        // Đổi chiều quy đổi
        btnSwap.setOnClickListener(v -> {
            int fromIndex = spinnerFrom.getSelectedItemPosition();
            int toIndex = spinnerTo.getSelectedItemPosition();

            spinnerFrom.setSelection(toIndex);
            spinnerTo.setSelection(fromIndex);

            calculateConversion();
        });

        // Tính lại khi nhập số tiền
        etAmountFrom.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after
            ) {}

            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count
            ) {}

            @Override
            public void afterTextChanged(Editable s) {
                calculateConversion();
            }
        });

        // Tính lại khi đổi loại tiền
        AdapterView.OnItemSelectedListener spinnerListener =
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {
                        calculateConversion();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                };

        spinnerFrom.setOnItemSelectedListener(spinnerListener);
        spinnerTo.setOnItemSelectedListener(spinnerListener);
    }

    private void fetchAllRates() {

        tvExchangeRate.setText("Đang tải dữ liệu tỷ giá...");

        ratesMap.put("USD", 1.0);

        SharedViewModel viewModel =
                new ViewModelProvider(requireActivity())
                        .get(SharedViewModel.class);

        viewModel.getCoinList().observe(
                getViewLifecycleOwner(),
                allCoins -> {

                    if (allCoins != null) {

                        for (CryptoCoin coin : allCoins) {

                            String symbol =
                                    coin.getSymbol().toUpperCase();

                            ratesMap.put(
                                    symbol,
                                    coin.getCurrentPrice()
                            );

                            if (!currencyList.contains(symbol)) {
                                currencyList.add(symbol);
                            }
                        }

                        updateUIOnDataLoaded();
                    }
                }
        );

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://api.exchangerate-api.com/v4/latest/USD")
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(
                    @NonNull Call call,
                    @NonNull IOException e
            ) {
                Log.e(
                        "Convert",
                        "Lỗi tải ngoại tệ: " + e.getMessage()
                );
            }

            @Override
            public void onResponse(
                    @NonNull Call call,
                    @NonNull Response response
            ) throws IOException {

                if (!response.isSuccessful() || response.body() == null) {
                    return;
                }

                try {
                    JSONObject obj = new JSONObject(
                            response.body().string()
                    );

                    JSONObject ratesObj = obj.getJSONObject("rates");

                    double vndRate = ratesObj.getDouble("VND");

                    synchronized (ratesMap) {
                        ratesMap.put("VND", 1.0 / vndRate);

                        for (String fiat : targetFiats) {
                            if (ratesObj.has(fiat)) {
                                ratesMap.put(
                                        fiat,
                                        1.0 / ratesObj.getDouble(fiat)
                                );
                            }
                        }
                    }

                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {

                            if (!isAdded() || getView() == null) {
                                return;
                            }

                            if (!currencyList.contains("USD")) {
                                currencyList.add(0, "USD");
                            }

                            if (!currencyList.contains("VND")) {
                                currencyList.add(
                                        Math.min(1, currencyList.size()),
                                        "VND"
                                );
                            }

                            for (String fiat : targetFiats) {
                                if (!currencyList.contains(fiat)) {
                                    currencyList.add(fiat);
                                }
                            }

                            updateUIOnDataLoaded();
                            buildFiatRatesTable(vndRate);
                        });
                    }

                } catch (Exception e) {
                    Log.e("Convert", "Lỗi xử lý tỷ giá", e);
                } finally {
                    response.close();
                }
            }
        });
    }

    private void updateUIOnDataLoaded() {

        if (spinnerAdapter == null) {
            return;
        }

        spinnerAdapter.notifyDataSetChanged();

        if (!isDataLoaded
                && currencyList.contains("BTC")
                && currencyList.contains("VND")) {

            spinnerFrom.setSelection(currencyList.indexOf("BTC"));
            spinnerTo.setSelection(currencyList.indexOf("VND"));

            isDataLoaded = true;
        }

        calculateConversion();
    }

    private void calculateConversion() {

        if (spinnerFrom == null || spinnerTo == null
                || spinnerFrom.getSelectedItem() == null
                || spinnerTo.getSelectedItem() == null) {
            return;
        }

        String fromSymbol =
                spinnerFrom.getSelectedItem().toString();

        String toSymbol =
                spinnerTo.getSelectedItem().toString();

        String amountStr =
                etAmountFrom.getText().toString().trim();

        if (amountStr.isEmpty()
                || !ratesMap.containsKey(fromSymbol)
                || !ratesMap.containsKey(toSymbol)) {

            tvAmountTo.setText("0.00");
            tvExchangeRate.setText("Sẵn sàng quy đổi");
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);

            double priceFromInUsd = ratesMap.get(fromSymbol);
            double priceToInUsd = ratesMap.get(toSymbol);

            if (priceToInUsd == 0) {
                return;
            }

            double result =
                    (amount * priceFromInUsd) / priceToInUsd;

            if (result < 0.01) {
                tvAmountTo.setText(
                        String.format("%.6f", result)
                );
            } else {
                tvAmountTo.setText(
                        String.format("%,.2f", result)
                );
            }

            double rate1To1 =
                    priceFromInUsd / priceToInUsd;

            tvExchangeRate.setText(
                    String.format(
                            "Tỷ giá: 1 %s ≈ %,.4f %s",
                            fromSymbol,
                            rate1To1,
                            toSymbol
                    )
            );

        } catch (NumberFormatException e) {
            tvAmountTo.setText("0.00");
        }
    }

    private void buildFiatRatesTable(double vndRatePerUsd) {

        if (layoutFiatRates == null) {
            return;
        }

        layoutFiatRates.removeAllViews();

        addFiatRow("USD", "Đô la Mỹ", vndRatePerUsd);

        for (String fiat : targetFiats) {

            if (ratesMap.containsKey(fiat)) {

                double priceInUsd = ratesMap.get(fiat);

                double priceInVnd =
                        priceInUsd * vndRatePerUsd;

                addFiatRow(
                        fiat,
                        getFiatFullName(fiat),
                        priceInVnd
                );
            }
        }
    }

    private void addFiatRow(
            String symbol,
            String fullName,
            double rateInVnd
    ) {

        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 16, 0, 16);
        row.setWeightSum(1f);

        LinearLayout leftCol = new LinearLayout(requireContext());
        leftCol.setOrientation(LinearLayout.VERTICAL);

        leftCol.setLayoutParams(
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0.5f
                )
        );

        TextView tvSymbol = new TextView(requireContext());
        tvSymbol.setText(symbol);
        tvSymbol.setTextSize(16);
        tvSymbol.setTextColor(Color.parseColor("#1E1E1E"));
        tvSymbol.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        TextView tvName = new TextView(requireContext());
        tvName.setText(fullName);
        tvName.setTextSize(13);
        tvName.setTextColor(Color.parseColor("#757575"));

        leftCol.addView(tvSymbol);
        leftCol.addView(tvName);

        TextView tvRate = new TextView(requireContext());

        tvRate.setLayoutParams(
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0.5f
                )
        );

        tvRate.setText(
                String.format("%,.0f ₫", rateInVnd)
        );

        tvRate.setTextSize(16);
        tvRate.setTextColor(Color.parseColor("#16A34A"));
        tvRate.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        tvRate.setTextAlignment(
                View.TEXT_ALIGNMENT_VIEW_END
        );

        row.addView(leftCol);
        row.addView(tvRate);

        layoutFiatRates.addView(row);

        View divider = new View(requireContext());

        divider.setLayoutParams(
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        1
                )
        );

        divider.setBackgroundColor(
                Color.parseColor("#E2E8F0")
        );

        layoutFiatRates.addView(divider);
    }

    private String getFiatFullName(String symbol) {

        switch (symbol) {
            case "EUR":
                return "Euro";

            case "GBP":
                return "Bảng Anh";

            case "JPY":
                return "Yên Nhật";

            case "AUD":
                return "Đô la Úc";

            case "SGD":
                return "Đô la Singapore";

            case "CAD":
                return "Đô la Canada";

            default:
                return "Ngoại tệ";
        }
    }
}
