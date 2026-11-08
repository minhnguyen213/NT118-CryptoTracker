package com.example.cryptotracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
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

public class AdminUsersFragment extends Fragment {

    private TextView tvTotalUsers;
    private RecyclerView rvUsersList;
    private SwipeRefreshLayout swipeRefreshUsers;
    private UserAdapter userAdapter;
    private List<UserModel> userList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_users, container, false);

        tvTotalUsers = view.findViewById(R.id.tvTotalUsers);
        rvUsersList = view.findViewById(R.id.rvUsersList);
        swipeRefreshUsers = view.findViewById(R.id.swipeRefreshUsers);

        rvUsersList.setLayoutManager(new LinearLayoutManager(getContext()));
        userAdapter = new UserAdapter(userList, this::updateUserRole);
        rvUsersList.setAdapter(userAdapter);

        swipeRefreshUsers.setOnRefreshListener(this::fetchUsersFromSupabase);

        fetchUsersFromSupabase();

        return view;
    }

    private void fetchUsersFromSupabase() {
        if (getContext() == null) return;
        swipeRefreshUsers.setRefreshing(true);
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
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
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        swipeRefreshUsers.setRefreshing(false);
                        Toast.makeText(getContext(), "Lỗi kết nối: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                boolean success = response.isSuccessful();

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        swipeRefreshUsers.setRefreshing(false);
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
                                Toast.makeText(getContext(), "Lỗi phân tích dữ liệu", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getContext(), "Không thể tải danh sách: " + responseBody, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }

    private void updateUserRole(UserModel user, String newRole) {
        if (getContext() == null) return;
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
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
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Lỗi kết nối: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                boolean success = response.isSuccessful();
                String resBody = response.body() != null ? response.body().string() : "";

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (success) {
                            Toast.makeText(getContext(), "Đã cập nhật quyền thành công!", Toast.LENGTH_SHORT).show();
                            fetchUsersFromSupabase();
                        } else {
                            Toast.makeText(getContext(), "Thất bại: " + resBody, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }
}
