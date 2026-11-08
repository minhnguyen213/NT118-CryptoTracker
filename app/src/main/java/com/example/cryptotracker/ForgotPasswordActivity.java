package com.example.cryptotracker;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private Button btnResetPassword;
    private TextView tvBackToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        etEmail = findViewById(R.id.etEmail);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        btnResetPassword.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }

            sendPasswordReset(email);
        });

        tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void sendPasswordReset(String email) {
        String url = SupabaseClient.SUPABASE_URL + "/auth/v1/recover";

        JSONObject body = new JSONObject();
        try {
            body.put("email", email);
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

        btnResetPassword.setEnabled(false);

        SupabaseClient.client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    btnResetPassword.setEnabled(true);
                    Toast.makeText(ForgotPasswordActivity.this, "Lỗi kết nối: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                boolean success = response.isSuccessful();
                String resBody = response.body() != null ? response.body().string() : "";

                runOnUiThread(() -> {
                    btnResetPassword.setEnabled(true);
                    if (success) {
                        Toast.makeText(ForgotPasswordActivity.this, "Email khôi phục mật khẩu đã được gửi!", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(ForgotPasswordActivity.this, "Thất bại: " + resBody, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}
