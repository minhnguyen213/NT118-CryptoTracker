package com.example.cryptotracker;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SharedViewModel extends ViewModel {
    private final MutableLiveData<List<CryptoCoin>> coinList = new MutableLiveData<>();
    private boolean isDataLoaded = false;
    public LiveData<List<CryptoCoin>> getCoinList() {
        if (!isDataLoaded) {
            loadDataFromApi();
        }
        return coinList;
    }

    private void loadDataFromApi() {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        Call<List<CryptoCoin>> call = apiService.getCoins("usd", "market_cap_desc", 100, 1, false);

        call.enqueue(new Callback<List<CryptoCoin>>() {
            @Override
            public void onResponse(Call<List<CryptoCoin>> call, Response<List<CryptoCoin>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    coinList.setValue(response.body());
                    isDataLoaded = true;
                }
            }

            @Override
            public void onFailure(Call<List<CryptoCoin>> call, Throwable t) {
            }
        });
    }
}