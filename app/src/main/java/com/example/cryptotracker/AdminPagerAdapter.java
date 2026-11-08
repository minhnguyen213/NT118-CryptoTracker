package com.example.cryptotracker;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class AdminPagerAdapter extends FragmentStateAdapter {

    public AdminPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new AdminUsersFragment();
            case 1:
                return new AdminCoinsFragment();
            case 2:
                return new AdminStatsFragment();
            default:
                return new AdminUsersFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
