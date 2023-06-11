package com.example.limitr.ui.home.main_fragment.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.limitr.ui.home.apps.fragment.AppList
import com.example.limitr.ui.home.blockedApps.fragment.BlockedApps

class AppListViewPagerAdapter  (activity: FragmentActivity) :
    FragmentStateAdapter(activity) {

    private val fragments = listOf(AppList(), BlockedApps())

    override fun getItemCount(): Int {
        return fragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }
}