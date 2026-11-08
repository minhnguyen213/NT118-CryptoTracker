package com.example.cryptotracker;

import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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

public class RegisterFragment extends Fragment {

    public static final String SUPABASE_URL =
            "https://fcqhzgqgqskallbhmpoo.supabase.co";

    public static final String SUPABASE_KEY =
            "sb_publishable_jikpuVbEvGxpSL2qu5fmDQ_UQqVJ_kj";

    private EditText etRegEmail, etRegPassword, etRegConfirmPassword;
    private Button btnDoRegister;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        etRegEmail = view.findViewById(R.id.etRegEmail);
        etRegPassword = view.findViewById(R.id.etRegPassword);
        etRegConfirmPassword = view.findViewById(R.id.etRegConfirmPassword);
        btnDoRegister = view.findViewById(R.id.btnDoRegister);
        ImageView btnBackRegister = view.findViewById(R.id.btnBackRegister);

        btnBackRegister.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        btnDoRegister.setOnClickListener(v -> handleRegistration());

        return view;
    }

    private void handleRegistration() {
        String email = etRegEmail.getText().toString().trim();
        String pass = etRegPassword.getText().toString().trim();
        String confirmPass = etRegConfirmPassword.getText().toString().trim();

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(getContext(), "Vui lòng nhập Email hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pass.length() < 6) {
            Toast.makeText(getContext(), "Mật khẩu phải có ít nhất 6 ký tự!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!pass.equals(confirmPass)) {
            Toast.makeText(getContext(), "Mật khẩu xác nhận không khớp!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnDoRegister.setEnabled(false);
        btnDoRegister.setText("Đang khởi tạo...");

        OkHttpClient client = new OkHttpClient();
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("email", email);
            jsonBody.put("password", pass);

            RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonBody.toString());
            Request request = new Request.Builder()
                    .url(SUPABASE_URL + "/auth/v1/signup")
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    resetUI("Lỗi kết nối mạng: " + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String responseData = response.body().string();
                    if (response.isSuccessful()) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Tạo tài khoản thành công! Hãy đăng nhập.", Toast.LENGTH_LONG).show();
                                Navigation.findNavController(getView()).navigateUp();
                            });
                        }
                    } else {
                        try {
                            JSONObject errJson = new JSONObject(responseData);
                            String msg = errJson.optString("msg", errJson.optString("error_description", "Lỗi tạo tài khoản"));
                            resetUI("Lỗi: " + msg);
                        } catch (Exception e) {
                            resetUI("Đăng ký thất bại!");
                        }
                    }
                }
            });
        } catch (Exception e) {
            resetUI("Lỗi hệ thống: " + e.getMessage());
        }
    }

    private void resetUI(String errorMsg) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                btnDoRegister.setEnabled(true);
                btnDoRegister.setText("Đăng ký ngay");
                if (errorMsg != null) {
                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}