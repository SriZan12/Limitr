package com.khadka.limitr

import com.khadka.limitr.blocking.*
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class ScheduleEvaluatorTest {
    private val timer = BlockingSession("a", "Same label", "one", "timer", 1000, 11000, 100, 10000, "boot")
    private fun now(wall: Long = 1000, elapsed: Long = 100, boot: String = "boot") = TimeSnapshot(wall, elapsed, boot)

    @Test fun timerUsesElapsedClockAcrossWallClockChanges() {
        assertEquals(7000, ScheduleEvaluator.evaluate(timer, now(wall = -90000, elapsed = 3100)).remaining)
        assertEquals(7000, ScheduleEvaluator.evaluate(timer, now(wall = 90000, elapsed = 3100)).remaining)
    }
    @Test fun timerExpiresAtExclusiveEnd() {
        assertEquals(Phase.ACTIVE, ScheduleEvaluator.evaluate(timer, now(elapsed = 10099)).phase)
        assertEquals(Phase.EXPIRED, ScheduleEvaluator.evaluate(timer, now(elapsed = 10100)).phase)
    }
    @Test fun rebootFallsBackToWallAndCannotExtendBeyondDuration() {
        assertEquals(5000, ScheduleEvaluator.evaluate(timer, now(wall = 6000, elapsed = 20, boot = "new")).remaining)
        assertEquals(10000, ScheduleEvaluator.evaluate(timer, now(wall = -10000, elapsed = 20, boot = "new")).remaining)
        assertEquals(Phase.EXPIRED, ScheduleEvaluator.evaluate(timer, now(wall = 12000, boot = "new")).phase)
    }
    @Test fun intervalsHaveInclusiveStartExclusiveEnd() {
        val interval = timer.copy(kind = "interval", startWall = 2000, endWall = 5000, duration = 3000)
        assertEquals(Phase.SCHEDULED, ScheduleEvaluator.evaluate(interval, now(wall = 1999)).phase)
        assertEquals(Phase.ACTIVE, ScheduleEvaluator.evaluate(interval, now(wall = 2000)).phase)
        assertEquals(Phase.EXPIRED, ScheduleEvaluator.evaluate(interval, now(wall = 5000)).phase)
        assertEquals(1L, ScheduleEvaluator.nextBoundary(listOf(interval), now(wall = 1999)))
    }
    @Test fun overlapIsIndependentAndMatchesPackagesInsteadOfLabels() {
        val second = timer.copy(packageName = "b", revision = "two", startElapsed = 200, duration = 500)
        assertEquals("one", ScheduleEvaluator.activeFor(listOf(timer, second), "a", now(elapsed = 600))?.revision)
        assertEquals("two", ScheduleEvaluator.activeFor(listOf(timer, second), "b", now(elapsed = 600))?.revision)
        assertNull(ScheduleEvaluator.activeFor(listOf(timer, second), "c", now()))
        assertEquals(100L, ScheduleEvaluator.nextBoundary(listOf(timer, second), now(elapsed = 600)))
    }
    @Test fun removingOrReplacingASessionMakesStaleRevisionInert() {
        assertNull(ScheduleEvaluator.activeFor(emptyList(), "a", now()))
        val replacement = timer.copy(revision = "replacement", kind = "interval", startWall = 5000)
        assertNull(ScheduleEvaluator.activeFor(listOf(replacement), "a", now()))
    }
    @Test fun invalidLegacyIntervalsExpireSafely() {
        assertEquals(Phase.EXPIRED, ScheduleEvaluator.evaluate(timer.copy(endWall = 1000), now()).phase)
        assertEquals(Phase.EXPIRED, ScheduleEvaluator.evaluate(timer.copy(duration = 0), now()).phase)
    }
    @Test fun overnightIntervalKeepsEndAfterStart() {
        val zone = ZoneId.of("Asia/Kathmandu")
        val now = ZonedDateTime.of(2026, 10, 12, 20, 0, 0, 0, zone).toInstant().toEpochMilli()
        val (start, end) = ScheduleEvaluator.interval(now, LocalTime.of(23, 0), LocalTime.of(1, 0), zone)
        assertEquals(2 * 3_600_000L, end - start)
        assertEquals(13, Instant.ofEpochMilli(end).atZone(zone).dayOfMonth)
    }
    @Test fun passedStartMeansTomorrowNotRecurring() {
        val zone = ZoneId.of("UTC")
        val now = Instant.parse("2026-10-12T20:00:00Z").toEpochMilli()
        val (start, end) = ScheduleEvaluator.interval(now, LocalTime.of(19, 0), LocalTime.of(21, 0), zone)
        assertEquals(Instant.parse("2026-10-13T19:00:00Z").toEpochMilli(), start)
        assertEquals(Instant.parse("2026-10-13T21:00:00Z").toEpochMilli(), end)
    }
    @Test fun calendarDaysRespectSpringAndAutumnDst() {
        val zone = ZoneId.of("America/New_York")
        for ((date, hours) in listOf("2026-03-07" to 3L, "2026-10-31" to 5L)) {
            val now = LocalDate.parse(date).atTime(20, 0).atZone(zone).toInstant().toEpochMilli()
            val (start, end) = ScheduleEvaluator.interval(now, LocalTime.of(23, 0), LocalTime.of(3, 0), zone)
            assertEquals(hours * 3_600_000, end - start)
        }
    }
    @Test fun equalLocalTimesMeanOneCalendarDay() {
        val now = Instant.parse("2026-10-12T08:00:00Z").toEpochMilli()
        val (start, end) = ScheduleEvaluator.interval(now, LocalTime.NOON, LocalTime.NOON, ZoneOffset.UTC)
        assertEquals(86_400_000L, end - start)
    }
}
