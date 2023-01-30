package com.example.limitr.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.limitr.R
import com.example.limitr.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth

class FragmentHome : Fragment(R.layout.fragment_home) {

    private lateinit var fragmentHomeBinding: FragmentHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentHomeBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)
        return fragmentHomeBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fragmentHomeBinding.profile.setOnClickListener {
            val action = FragmentHomeDirections.actionFragmentHomeToFragmentEditProfile()
            findNavController().navigate(action)
        }

        Glide.with(requireContext())
            .load(FirebaseAuth.getInstance().currentUser?.photoUrl)
            .placeholder(R.drawable.user)
            .into(fragmentHomeBinding.profile)
    }
}