package com.example.cryptotracker;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<UserModel> userList;
    private OnRoleChangeListener roleChangeListener;

    public interface OnRoleChangeListener {
        void onRoleChange(UserModel user, String newRole);
    }

    public UserAdapter(List<UserModel> userList, OnRoleChangeListener listener) {
        this.userList = userList;
        this.roleChangeListener = listener;
    }

    public void updateData(List<UserModel> newList) {
        this.userList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserModel user = userList.get(position);

        holder.tvUserEmail.setText(user.getEmail() != null ? user.getEmail() : "N/A");
        holder.tvUserFullName.setText(user.getFullName() != null && !user.getFullName().isEmpty() ? user.getFullName() : "(Chưa đặt tên)");
        holder.tvUserRole.setText(user.getRole());

        if ("admin".equals(user.getRole())) {
            holder.tvUserRole.setBackgroundColor(Color.parseColor("#4CAF50"));
            holder.btnToggleRole.setText("Hạ quyền User");
        } else {
            holder.tvUserRole.setBackgroundColor(Color.parseColor("#2196F3"));
            holder.btnToggleRole.setText("Nâng quyền Admin");
        }

        holder.btnToggleRole.setOnClickListener(v -> {
            String newRole = "admin".equals(user.getRole()) ? "user" : "admin";
            if (roleChangeListener != null) {
                roleChangeListener.onRoleChange(user, newRole);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList == null ? 0 : userList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserEmail, tvUserFullName, tvUserRole;
        Button btnToggleRole;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
            tvUserFullName = itemView.findViewById(R.id.tvUserFullName);
            tvUserRole = itemView.findViewById(R.id.tvUserRole);
            btnToggleRole = itemView.findViewById(R.id.btnToggleRole);
        }
    }
}
