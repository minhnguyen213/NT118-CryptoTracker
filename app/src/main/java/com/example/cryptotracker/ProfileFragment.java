package com.example.cryptotracker;

import android.content.Context;
import android.content.Intent;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        TextView tvProfileName = view.findViewById(R.id.tvProfileName);
        TextView tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        TextView btnAccount = view.findViewById(R.id.btnAccount);
        TextView btnSettings = view.findViewById(R.id.btnSettings);
        TextView btnLogout = view.findViewById(R.id.btnLogout);

        // Load user email & full name from SharedPreferences
        SharedPreferences prefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String email = prefs.getString("user_email", "student@uit.edu.vn");
        String fullName = prefs.getString("user_full_name", "");
        String userId = prefs.getString("user_id", "");
        String accessToken = prefs.getString("access_token", "");

        tvProfileEmail.setText(email);
        if (!fullName.isEmpty()) {
            tvProfileName.setText(fullName);
        } else if (email.contains("@")) {
            String name = email.substring(0, email.indexOf("@"));
            tvProfileName.setText(name);
        }

        // Fetch latest profile from Supabase server
        if (!userId.isEmpty() && !accessToken.isEmpty()) {
            fetchProfileFromSupabase(userId, accessToken, tvProfileName, tvProfileEmail);
        }

        btnAccount.setOnClickListener(v -> showAccountManagementDialog());

        btnSettings.setOnClickListener(v -> showPortfolioDialog());

        btnLogout.setOnClickListener(v -> {
            prefs.edit().clear().apply();

            Toast.makeText(getContext(), "Đã đăng xuất!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return view;
    }

    private void fetchProfileFromSupabase(String userId, String accessToken, TextView tvProfileName, TextView tvProfileEmail) {
        String url = SupabaseClient.SUPABASE_URL + "/rest/v1/profiles?select=full_name,email&id=eq." + userId;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", SupabaseClient.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Accept", "application/json")
                .build();

        SupabaseClient.client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Ignore
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String resStr = response.body().string();
                    try {
                        JSONArray array = new JSONArray(resStr);
                        if (array.length() > 0) {
                            JSONObject obj = array.getJSONObject(0);
                            String fullName = obj.optString("full_name", "");
                            String email = obj.optString("email", "");

                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    SharedPreferences prefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
                                    if (!fullName.isEmpty()) {
                                        prefs.edit().putString("user_full_name", fullName).apply();
                                        tvProfileName.setText(fullName);
                                    }
                                    if (!email.isEmpty()) {
                                        prefs.edit().putString("user_email", email).apply();
                                        tvProfileEmail.setText(email);
                                    }
                                });
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void showAccountManagementDialog() {
        if (getContext() == null) return;

        SharedPreferences prefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String currentEmail = prefs.getString("user_email", "");
        String currentName = prefs.getString("user_full_name", "");
        String accessToken = prefs.getString("access_token", "");
        String userId = prefs.getString("user_id", "");

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_manage_account, null);
        builder.setView(dialogView);

        EditText etFullName = dialogView.findViewById(R.id.etFullName);
        TextView tvEmailDisplay = dialogView.findViewById(R.id.tvEmailDisplay);
        Button btnSaveProfile = dialogView.findViewById(R.id.btnSaveProfile);
        Button btnChangePassword = dialogView.findViewById(R.id.btnChangePassword);

        etFullName.setText(currentName);
        tvEmailDisplay.setText(currentEmail);

        android.app.AlertDialog dialog = builder.create();

        btnSaveProfile.setOnClickListener(v -> {
            String newName = etFullName.getText().toString().trim();
            if (newName.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập họ tên", Toast.LENGTH_SHORT).show();
                return;
            }

            btnSaveProfile.setEnabled(false);
            updateProfileInSupabase(userId, accessToken, newName, dialog, btnSaveProfile);
        });

        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog(accessToken, dialog));

        dialog.show();
    }

    private void showChangePasswordDialog(String accessToken, android.app.AlertDialog parentDialog) {
        if (getContext() == null) return;

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);

        final EditText etNewPassword = new EditText(getContext());
        etNewPassword.setHint("Mật khẩu mới (ít nhất 6 ký tự)");
        etNewPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etNewPassword);

        builder.setTitle("Đổi mật khẩu")
                .setView(layout)
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    String newPass = etNewPassword.getText().toString().trim();
                    if (newPass.length() < 6) {
                        Toast.makeText(getContext(), "Mật khẩu cần ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    updatePasswordInSupabase(accessToken, newPass, parentDialog);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void updatePasswordInSupabase(String accessToken, String newPassword, android.app.AlertDialog parentDialog) {
        String url = SupabaseClient.SUPABASE_URL + "/auth/v1/user";

        JSONObject body = new JSONObject();
        try {
            body.put("password", newPassword);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        Request request = new Request.Builder()
                .url(url)
                .put(RequestBody.create(
                        body.toString(),
                        MediaType.parse("application/json")
                ))
                .addHeader("apikey", SupabaseClient.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json")
                .build();

        SupabaseClient.client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Lỗi kết nối: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                boolean success = response.isSuccessful();
                String resBody = response.body() != null ? response.body().string() : "";

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (success) {
                            Toast.makeText(getContext(), "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();
                            parentDialog.dismiss();
                        } else {
                            Toast.makeText(getContext(), "Đổi mật khẩu thất bại: " + resBody, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }

    private void updateProfileInSupabase(String userId, String accessToken, String newName, android.app.AlertDialog dialog, Button btnSave) {
        String url = SupabaseClient.SUPABASE_URL + "/rest/v1/profiles?id=eq." + userId;

        JSONObject body = new JSONObject();
        try {
            body.put("full_name", newName);
        } catch (Exception e) {
            e.printStackTrace();
            btnSave.setEnabled(true);
            return;
        }

        Request request = new Request.Builder()
                .url(url)
                .patch(RequestBody.create(
                        body.toString(),
                        MediaType.parse("application/json")
                ))
                .addHeader("apikey", SupabaseClient.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .build();

        SupabaseClient.client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        btnSave.setEnabled(true);
                        Toast.makeText(getContext(), "Lỗi kết nối: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                boolean success = response.isSuccessful();
                String responseBody = response.body() != null ? response.body().string() : "";

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        btnSave.setEnabled(true);
                        if (success) {
                            SharedPreferences prefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
                            prefs.edit().putString("user_full_name", newName).apply();

                            View view = getView();
                            if (view != null) {
                                TextView tvProfileName = view.findViewById(R.id.tvProfileName);
                                if (tvProfileName != null) {
                                    tvProfileName.setText(newName);
                                }
                            }

                            Toast.makeText(getContext(), "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        } else {
                            Toast.makeText(getContext(), "Cập nhật thất bại: " + responseBody, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }

    private void showPortfolioDialog() {
        if (getContext() == null) return;

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_portfolio, null);
        builder.setView(dialogView);

        EditText etCoinSymbol = dialogView.findViewById(R.id.etCoinSymbol);
        EditText etCoinAmount = dialogView.findViewById(R.id.etCoinAmount);
        Button btnAddHolding = dialogView.findViewById(R.id.btnAddHolding);
        TextView tvPortfolioSummary = dialogView.findViewById(R.id.tvPortfolioSummary);
        TextView tvHoldingsList = dialogView.findViewById(R.id.tvHoldingsList);

        android.app.AlertDialog dialog = builder.create();

        SharedPreferences prefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String holdingsJson = prefs.getString("portfolio_holdings", "{}");

        JSONObject tempObj;
        try {
            tempObj = new JSONObject(holdingsJson);
        } catch (Exception e) {
            tempObj = new JSONObject();
        }
        final JSONObject holdingsObj = tempObj;

        Runnable updateDisplay = () -> {
            StringBuilder sb = new StringBuilder();
            Iterator<String> keys = holdingsObj.keys();
            while (keys.hasNext()) {
                String symbol = keys.next();
                double amount = holdingsObj.optDouble(symbol, 0);
                sb.append("• ").append(symbol).append(": ").append(amount).append("\n");
            }
            if (sb.length() == 0) {
                tvHoldingsList.setText("Chưa có tài sản nào.");
            } else {
                tvHoldingsList.setText(sb.toString().trim());
            }
            tvPortfolioSummary.setText("Danh mục tài sản cá nhân");
        };

        updateDisplay.run();

        btnAddHolding.setOnClickListener(v -> {
            String symbol = etCoinSymbol.getText().toString().trim().toUpperCase();
            String amountStr = etCoinAmount.getText().toString().trim();

            if (symbol.isEmpty() || amountStr.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double amount = Double.parseDouble(amountStr);
                holdingsObj.put(symbol, amount);
                prefs.edit().putString("portfolio_holdings", holdingsObj.toString()).apply();
                etCoinSymbol.setText("");
                etCoinAmount.setText("");
                updateDisplay.run();
                Toast.makeText(getContext(), "Đã thêm tài sản thành công!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(getContext(), "Số lượng không hợp lệ", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }
}
