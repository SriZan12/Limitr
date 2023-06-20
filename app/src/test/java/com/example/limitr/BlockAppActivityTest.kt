package com.example.limitr

import junit.framework.TestCase.assertEquals
import org.junit.Test
import java.util.Date

class BlockAppActivityTest {

    @Test
    fun checkConditionForTime() {

        val currentTime = System.currentTimeMillis()

//        First Condition(currentTime > StartTime)
        val startTime =
            Date(currentTime - 60000 * 8) // Start Time is Eight minutes greater than current Time.

        //        SecondCondition (currentTime > EndTime)
        val endTime = Date(currentTime - 60000 * 10) // End Time is after 10 minutes.

        val expectedStartTime = startTime.time + 86400000L
        val expectedEndTime = endTime.time + 86400000L

        val calculatedTimeBothCondition = checkConditionForTimeInterval(currentTime, startTime,endTime)
        assertEquals(expectedStartTime, calculatedTimeBothCondition.first) // Testing first condition
        assertEquals(expectedEndTime, calculatedTimeBothCondition.second) // Testing second condition

    }

    private fun checkConditionForTimeInterval(
        currentTime: Long,
        startTime: Date,
        endTime: Date
    ): Pair<Long, Long> {
        val newStartTime = if (currentTime > startTime.time) {
            startTime.time + 86400000L // 1 day in milliseconds
        } else {
            startTime.time
        }

        val newEndTime = if (currentTime > endTime.time) {
            endTime.time + 86400000L // 1 day in milliseconds
        } else {
            endTime.time
        }

        return Pair(newStartTime, newEndTime)
    }
}