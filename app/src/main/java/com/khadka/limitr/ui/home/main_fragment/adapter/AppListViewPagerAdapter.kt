package com.khadka.limitr.ui.home.main_fragment.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.khadka.limitr.ui.home.apps.fragment.AppList
import com.khadka.limitr.ui.home.blockedApps.fragment.BlockedApps

class AppListViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount() = 2
    override fun createFragment(position: Int): Fragment = if (position == 0) AppList() else BlockedApps()
}
