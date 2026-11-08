package com.example.cryptotracker;

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
import androidx.navigation.Navigation;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProfileFragment extends Fragment {

    public static final String SUPABASE_URL =
            "https://fcqhzgqgqskallbhmpoo.supabase.co";

    public static final String SUPABASE_KEY =
            "sb_publishable_jikpuVbEvGxpSL2qu5fmDQ_UQqVJ_kj";

    private LinearLayout layoutGuest, layoutLoggedIn;
    private EditText etEmail, etPassword;
    private Button btnLogin, btnRegister;
    private TextView tvProfileEmail, btnWatchlist, btnLogout;

    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        layoutGuest = view.findViewById(R.id.layoutGuest);
        layoutLoggedIn = view.findViewById(R.id.layoutLoggedIn);
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        btnLogin = view.findViewById(R.id.btnLogin);
        btnRegister = view.findViewById(R.id.btnRegister);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        btnWatchlist = view.findViewById(R.id.btnWatchlist);
        btnLogout = view.findViewById(R.id.btnLogout);

        sharedPreferences = requireActivity().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);

        checkLoginState();
        setupListeners();

        return view;
    }

    private void checkLoginState() {
        String savedEmail = sharedPreferences.getString("user_email", null);
        String savedToken = sharedPreferences.getString("access_token", null);

        if (savedEmail != null && savedToken != null) {
            layoutGuest.setVisibility(View.GONE);
            layoutLoggedIn.setVisibility(View.VISIBLE);
            tvProfileEmail.setText(savedEmail);
        } else {
            layoutGuest.setVisibility(View.VISIBLE);
            layoutLoggedIn.setVisibility(View.GONE);
        }
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.registerFragment));

        btnLogin.setOnClickListener(v -> performLogin());

        btnLogout.setOnClickListener(v -> {
            sharedPreferences.edit().clear().apply();
            checkLoginState();
            Toast.makeText(getContext(), "Đã đăng xuất", Toast.LENGTH_SHORT).show();
        });

        btnWatchlist.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Tính năng Watchlist đang phát triển", Toast.LENGTH_SHORT).show();
        });
    }

    private void performLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập Email và Mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);
        btnRegister.setEnabled(false);
        btnLogin.setText("Đang Đăng nhập...");

        OkHttpClient client = new OkHttpClient();
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("email", email);
            jsonBody.put("password", password);

            RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonBody.toString());

            Request request = new Request.Builder()
                    .url(SUPABASE_URL + "/auth/v1/token?grant_type=password")
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    resetButtons("Lỗi mạng: " + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String responseData = response.body().string();

                    if (response.isSuccessful()) {
                        try {
                            JSONObject jsonRes = new JSONObject(responseData);
                            String accessToken = jsonRes.getString("access_token");
                            String userEmail = jsonRes.getJSONObject("user").getString("email");

                            sharedPreferences.edit()
                                    .putString("access_token", accessToken)
                                    .putString("user_email", userEmail)
                                    .apply();

                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                                    checkLoginState();
                                    resetButtons(null);
                                    etEmail.setText("");
                                    etPassword.setText("");
                                });
                            }
                        } catch (Exception e) {
                            resetButtons("Lỗi đọc dữ liệu máy chủ: " + e.getMessage());
                        }
                    } else {
                        try {
                            JSONObject errJson = new JSONObject(responseData);
                            String msg = errJson.optString("error_description", errJson.optString("msg", "Sai thông tin đăng nhập"));
                            resetButtons("Lỗi: " + msg);
                        } catch (Exception e) {
                            resetButtons("Đăng nhập thất bại (HTTP " + response.code() + ")");
                        }
                    }
                }
            });
        } catch (Exception e) {
            resetButtons("Lỗi tạo yêu cầu: " + e.getMessage());
        }
    }

    private void resetButtons(String errorMessage) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                btnLogin.setEnabled(true);
                btnRegister.setEnabled(true);
                btnLogin.setText("Đăng Nhập");
                if (errorMessage != null) {
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}