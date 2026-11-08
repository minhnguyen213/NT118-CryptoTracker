package com.example.cryptotracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    Button btnLogin;
    TextView tvRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if already logged in
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String token = prefs.getString("access_token", null);
        String savedRole = prefs.getString("user_role", null);
        if (token != null) {
            if ("admin".equals(savedRole)) {
                startActivity(new Intent(LoginActivity.this, AdminDashboardActivity.class));
            } else {
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
            }
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }

            loginUser(email, password);
        });

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });
    }

    private void loginUser(String email, String password) {
        String url = SupabaseClient.SUPABASE_URL + "/auth/v1/token?grant_type=password";

        JSONObject body = new JSONObject();
        try {
            body.put("email", email);
            body.put("password", password);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(
                        body.toString(),
                        MediaType.parse("application/json")
                ))
                .addHeader("apikey", SupabaseClient.SUPABASE_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        btnLogin.setEnabled(false);

        SupabaseClient.client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    btnLogin.setEnabled(true);
                    Toast.makeText(LoginActivity.this, "Lỗi kết nối: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                boolean success = response.isSuccessful();

                runOnUiThread(() -> {
                    if (!success) {
                        btnLogin.setEnabled(true);
                        Toast.makeText(LoginActivity.this, "Đăng nhập thất bại: " + responseBody, Toast.LENGTH_LONG).show();
                        return;
                    }

                    try {
                        JSONObject jsonObject = new JSONObject(responseBody);
                        String accessToken = jsonObject.optString("access_token");
                        JSONObject userObj = jsonObject.optJSONObject("user");

                        if (userObj == null) {
                            btnLogin.setEnabled(true);
                            Toast.makeText(LoginActivity.this, "Không tìm thấy thông tin user", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String userId = userObj.optString("id");
                        String userEmail = userObj.optString("email", email);

                        // Fetch user role from profiles table
                        SupabaseClient.getUserRole(accessToken, userId, new SupabaseClient.RoleCallback() {
                            @Override
                            public void onSuccess(String role) {
                                btnLogin.setEnabled(true);

                                // Save session and role
                                SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                                prefs.edit()
                                        .putString("access_token", accessToken)
                                        .putString("user_email", userEmail)
                                        .putString("user_id", userId)
                                        .putString("user_role", role)
                                        .apply();

                                Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();

                                if ("admin".equals(role)) {
                                    startActivity(new Intent(LoginActivity.this, AdminDashboardActivity.class));
                                } else {
                                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                }
                                finish();
                            }

                            @Override
                            public void onError(String message) {
                                btnLogin.setEnabled(true);
                                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                            }
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                        btnLogin.setEnabled(true);
                        Toast.makeText(LoginActivity.this, "Lỗi xử lý dữ liệu phản hồi", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}
