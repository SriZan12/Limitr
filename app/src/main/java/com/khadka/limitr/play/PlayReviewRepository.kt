package com.khadka.limitr.play

import android.app.Activity
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayReviewRepository @Inject constructor(private val manager: ReviewManager) {
    suspend fun request(): ReviewInfo = manager.requestReviewFlow().await()

    suspend fun launch(activity: Activity, info: ReviewInfo) {
        manager.launchReviewFlow(activity, info).await()
        // Completion tells us neither whether the card appeared nor whether a review was submitted.
    }
}
