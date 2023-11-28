package com.khadka.limitr.utils

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.khadka.limitr.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.khadka.limitr.utils.Constants.REFERRAL_NODE
import timber.log.Timber

object FirebaseUtils {

    val USER_UID = FirebaseAuth.getInstance().uid
    private val firebaseDatabase = FirebaseDatabase.getInstance()

    fun loadProfilePhoto(imageView: ImageView, context: Context) {
        Glide.with(context)
            .load(FirebaseAuth.getInstance().currentUser?.photoUrl)
            .placeholder(R.drawable.user)
            .into(imageView)
    }

    fun setReferralStatus() {
        firebaseDatabase.getReference(USER_UID.toString()).child(REFERRAL_NODE)
            .setValue(false)
    }

    fun updateReferralStatus(userUID: String?, status: Boolean) {
        val referralStatus = firebaseDatabase.getReference(userUID.toString())

        val updates = mapOf("referralStatus" to status)

        referralStatus.updateChildren(updates)
            .addOnSuccessListener {
                Timber.d("Firebase", "Update successful!")
            }
            .addOnFailureListener { e ->
                Timber.e("Firebase", "Update failed!", e)
            }

    }

    fun getReferralStatus(callback: (Boolean) -> Unit) {
        firebaseDatabase.getReference(USER_UID.toString()).child(REFERRAL_NODE)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val status = snapshot.getValue(Boolean::class.java)
                    if(status != null) {
                        Timber.d("STATUS REFERRER = $status")
                        callback(status)
                    }else{
                        callback(false)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(false)
                }
            })
    }

}