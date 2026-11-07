package com.example.cryptotracker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        TextView btnAccount = view.findViewById(R.id.btnAccount);
        TextView btnSettings = view.findViewById(R.id.btnSettings);
        TextView btnLogout = view.findViewById(R.id.btnLogout);

        btnAccount.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Tính năng Quản lý tài khoản đang phát triển", Toast.LENGTH_SHORT).show();
        });

        btnSettings.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Tính năng Cài đặt Alerts đang phát triển", Toast.LENGTH_SHORT).show();
        });

        btnLogout.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Đã đăng xuất!", Toast.LENGTH_SHORT).show();
            // Vị trí để chèn code Firebase Auth sign-out s
        });

        return view;
    }
}