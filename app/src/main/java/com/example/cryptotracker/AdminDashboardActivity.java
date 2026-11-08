package com.example.cryptotracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView tvTotalUsers, tvAdminEmailInfo;
    private RecyclerView rvUsersList;
    private Button btnLogoutAdmin;
    private UserAdapter userAdapter;
    private List<UserModel> userList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        tvTotalUsers = findViewById(R.id.tvTotalUsers);
        tvAdminEmailInfo = findViewById(R.id.tvAdminEmailInfo);
        rvUsersList = findViewById(R.id.rvUsersList);
        btnLogoutAdmin = findViewById(R.id.btnLogoutAdmin);

        rvUsersList.setLayoutManager(new LinearLayoutManager(this));
        userAdapter = new UserAdapter(userList, this::updateUserRole);
        rvUsersList.setAdapter(userAdapter);

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String email = prefs.getString("user_email", "admin@uit.edu.vn");
        tvAdminEmailInfo.setText("Đang đăng nhập: " + email + " (Admin)");

        btnLogoutAdmin.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            Toast.makeText(this, "Đã đăng xuất!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        fetchUsersFromSupabase();
    }

    private void fetchUsersFromSupabase() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String accessToken = prefs.getString("access_token", "");

        String url = SupabaseClient.SUPABASE_URL + "/rest/v1/profiles?select=*&order=created_at.desc";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", SupabaseClient.SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Accept", "application/json")
                .build();

        SupabaseClient.client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(AdminDashboardActivity.this, "Lỗi kết nối: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                boolean success = response.isSuccessful();

                runOnUiThread(() -> {
                    if (success) {
                        try {
                            JSONArray array = new JSONArray(responseBody);
                            userList.clear();
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject obj = array.getJSONObject(i);
                                String id = obj.optString("id");
                                String email = obj.optString("email");
                                String fullName = obj.optString("full_name");
                                String role = obj.optString("role", "user");

                                userList.add(new UserModel(id, email, fullName, role));
                            }

                            userAdapter.updateData(userList);
                            tvTotalUsers.setText("Tổng số người dùng: " + userList.size());

                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(AdminDashboardActivity.this, "Lỗi phân tích dữ liệu", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(AdminDashboardActivity.this, "Không thể tải danh sách: " + responseBody, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void updateUserRole(UserModel user, String newRole) {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String accessToken = prefs.getString("access_token", "");

        String url = SupabaseClient.SUPABASE_URL + "/rest/v1/profiles?id=eq." + user.getId();

        JSONObject body = new JSONObject();
        try {
            body.put("role", newRole);
        } catch (Exception e) {
            e.printStackTrace();
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
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(AdminDashboardActivity.this, "Lỗi kết nối: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                boolean success = response.isSuccessful();
                String resBody = response.body() != null ? response.body().string() : "";

                runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(AdminDashboardActivity.this, "Đã cập nhật quyền thành công!", Toast.LENGTH_SHORT).show();
                        fetchUsersFromSupabase();
                    } else {
                        Toast.makeText(AdminDashboardActivity.this, "Thất bại: " + resBody, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}
