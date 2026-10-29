package com.khadka.limitr

import com.khadka.limitr.usage.ForegroundUsage
import com.khadka.limitr.usage.ForegroundUsage.Kind.*
import org.junit.Assert.*
import org.junit.Test

class ForegroundUsageTest {
    private fun usage(multiResume: Boolean = true) = ForegroundUsage(1000, 2000, multiResume)

    @Test fun clipsCarryInAndStillOpenActivityToRequestedWindow() {
        val usage = usage()
        usage.event(500, RESUME, "a", "Main")
        usage.event(1200, PAUSE, "a", "Main")
        usage.event(1500, RESUME, "b", "Main")
        assertEquals(mapOf("a" to 200L, "b" to 500L), usage.result())
    }

    @Test fun excludesOldUsageAndEventsAtExclusiveEnd() {
        val usage = usage()
        usage.event(100, RESUME, "old", "Main")
        usage.event(900, PAUSE, "old", "Main")
        usage.event(2000, RESUME, "future", "Main")
        assertTrue(usage.result().isEmpty())
    }

    @Test fun activityHandoffsAndDuplicateEventsDoNotDoubleCountPackage() {
        val usage = usage()
        usage.event(1100, RESUME, "a", "First")
        usage.event(1100, RESUME, "a", "First")
        usage.event(1200, RESUME, "a", "Second")
        usage.event(1300, PAUSE, "a", "First")
        usage.event(1400, STOP, "a", "First")
        usage.event(1500, PAUSE, "a", "Second")
        usage.event(1600, PAUSE, "a", "Second")
        assertEquals(mapOf("a" to 400L), usage.result())
    }

    @Test fun multipleResumedPackagesCanAccumulateWithoutDoubleCountingTheirActivities() {
        val usage = usage()
        usage.event(1000, RESUME, "a", "Main")
        usage.event(1200, RESUME, "b", "Main")
        usage.event(1400, PAUSE, "a", "Main")
        usage.event(1600, PAUSE, "b", "Main")
        assertEquals(mapOf("a" to 400L, "b" to 400L), usage.result())
    }

    @Test fun olderAndroidSwitchesClosePreviousForegroundPackage() {
        val usage = usage(multiResume = false)
        usage.event(1000, RESUME, "a", "Main")
        usage.event(1200, RESUME, "b", "Main")
        usage.event(1600, PAUSE, "b", "Main")
        assertEquals(mapOf("a" to 200L, "b" to 400L), usage.result())
    }

    @Test fun screenOffClosesUsageUntilAnotherResume() {
        val usage = usage()
        usage.event(1000, RESUME, "a", "Main")
        usage.event(1100, RESUME, "b", "Main")
        usage.event(1200, SCREEN_OFF)
        usage.event(1700, RESUME, "a", "Main")
        usage.event(1800, PAUSE, "a", "Main")
        assertEquals(mapOf("a" to 300L, "b" to 100L), usage.result())
    }

    @Test fun shutdownClosesIntervalsAndStartupDropsUnmatchedOpens() {
        val usage = usage()
        usage.event(1000, RESUME, "a", "Main")
        usage.event(1200, SHUTDOWN)
        usage.event(1300, RESUME, "unknown", "Main")
        usage.event(1600, STARTUP)
        usage.event(1700, RESUME, "a", "Main")
        assertEquals(mapOf("a" to 500L), usage.result())
    }

    @Test fun missingLaunchDoesNotInventUsageAndMissingClassCanClosePackage() {
        val usage = usage()
        usage.event(1100, PAUSE, "unknown", "Main")
        usage.event(1200, RESUME, "a", "Main")
        usage.event(1300, PAUSE, "a")
        assertEquals(mapOf("a" to 100L), usage.result())
    }
}
