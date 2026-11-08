package com.example.cryptotracker;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SupabaseClient {

    public static final String SUPABASE_URL =
            "https://fcqhzgqgqskallbhmpoo.supabase.co";

    public static final String SUPABASE_KEY =
            "sb_publishable_jikpuVbEvGxpSL2qu5fmDQ_UQqVJ_kj";

    public static final OkHttpClient client =
            new OkHttpClient();

    public interface RoleCallback {
        void onSuccess(String role);
        void onError(String message);
    }

    public static void getUserRole(
            String accessToken,
            String userId,
            RoleCallback callback
    ) {

        String url = SUPABASE_URL
                + "/rest/v1/profiles"
                + "?select=role&id=eq." + userId;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {

            Handler handler = new Handler(Looper.getMainLooper());

            @Override
            public void onFailure(Call call, IOException e) {
                handler.post(() ->
                        callback.onError("Lỗi kết nối: " + e.getMessage())
                );
            }

            @Override
            public void onResponse(Call call, Response response)
                    throws IOException {

                String responseBody = response.body() != null
                        ? response.body().string()
                        : "";

                if (!response.isSuccessful()) {
                    int code = response.code();
                    response.close();

                    handler.post(() ->
                            callback.onError(
                                    "Không lấy được quyền tài khoản. HTTP "
                                            + code
                            )
                    );
                    return;
                }

                response.close();

                try {
                    JSONArray array = new JSONArray(responseBody);

                    if (array.length() == 0) {
                        handler.post(() ->
                                callback.onError(
                                        "Không tìm thấy hồ sơ tài khoản."
                                )
                        );
                        return;
                    }

                    JSONObject profile = array.getJSONObject(0);
                    String role = profile.optString("role", "");

                    if (!role.equals("admin")
                            && !role.equals("user")) {
                        handler.post(() ->
                                callback.onError("Quyền tài khoản không hợp lệ.")
                        );
                        return;
                    }

                    handler.post(() -> callback.onSuccess(role));

                } catch (Exception e) {
                    handler.post(() ->
                            callback.onError("Lỗi đọc dữ liệu: " + e.getMessage())
                    );
                }
            }
        });
    }
}
