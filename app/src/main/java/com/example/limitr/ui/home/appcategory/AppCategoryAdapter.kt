package com.example.limitr.ui.home.appcategory

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter


class AppCategoryAdapter
    (activity: FragmentActivity) :
    FragmentStateAdapter(activity) {

    private val fragments = listOf(InstalledAppFragment(), SystemAppFragment())


    override fun getItemCount(): Int {
        return fragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }
}