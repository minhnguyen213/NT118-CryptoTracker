package com.example.cryptotracker;

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

public class RegisterActivity extends AppCompatActivity {

    EditText etEmail, etPassword, etConfirmPassword;
    Button btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        TextView tvLogin = findViewById(R.id.tvLogin);

        tvLogin.setOnClickListener(v -> finish());

        btnRegister.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();
            String confirm = etConfirmPassword.getText().toString();

            if (email.isEmpty() || password.isEmpty()
                    || confirm.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS
                    .matcher(email).matches()) {
                Toast.makeText(this, "Email không hợp lệ",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(this, "Mật khẩu cần ít nhất 6 ký tự",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirm)) {
                Toast.makeText(this, "Mật khẩu không trùng khớp",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            registerUser(email, password);
        });
    }

    private void registerUser(String email, String password) {

        String url = SupabaseClient.SUPABASE_URL
                + "/auth/v1/signup";

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

        btnRegister.setEnabled(false);

        SupabaseClient.client.newCall(request).enqueue(
                new Callback() {

                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(() -> {
                            btnRegister.setEnabled(true);
                            Toast.makeText(
                                    RegisterActivity.this,
                                    "Lỗi kết nối: " + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response)
                            throws IOException {

                        String result = response.body().string();
                        boolean success = response.isSuccessful();

                        runOnUiThread(() -> {
                            btnRegister.setEnabled(true);

                            if (success) {
                                Toast.makeText(
                                        RegisterActivity.this,
                                        "Đăng ký thành công! Hãy đăng nhập.",
                                        Toast.LENGTH_LONG
                                ).show();
                                finish();
                            } else {
                                Toast.makeText(
                                        RegisterActivity.this,
                                        "Đăng ký thất bại: " + result,
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        });
                    }
                }
        );
    }
}
