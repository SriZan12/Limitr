package com.khadka.limitr.blocking

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/** A single current session per package. Legacy rows are preserved separately. */
@Entity(tableName = "BlockingSession")
data class BlockingSession(
    @PrimaryKey val packageName: String,
    val appName: String,
    val revision: String,
    val kind: String,
    val startWall: Long,
    val endWall: Long,
    val startElapsed: Long?,
    val duration: Long,
    val bootId: String?,
    val blockNotifications: Boolean = false
)

data class TimeSnapshot(val wall: Long, val elapsed: Long, val bootId: String)
enum class Phase { SCHEDULED, ACTIVE, EXPIRED }
data class Evaluation(val phase: Phase, val remaining: Long, val nextBoundaryIn: Long?)

object ScheduleEvaluator {
    fun evaluate(session: BlockingSession, now: TimeSnapshot): Evaluation {
        val remaining = if (session.kind == "timer" && session.bootId == now.bootId &&
            session.startElapsed != null && now.elapsed >= session.startElapsed) {
            session.duration - (now.elapsed - session.startElapsed)
        } else {
            // After reboot/restore only wall time is available; never extend beyond original duration.
            (session.endWall - now.wall).coerceAtMost(session.duration)
        }
        if (remaining <= 0 || session.duration <= 0 || session.endWall <= session.startWall) {
            return Evaluation(Phase.EXPIRED, 0, null)
        }
        if (session.kind == "interval" && now.wall < session.startWall) {
            return Evaluation(Phase.SCHEDULED, remaining, session.startWall - now.wall)
        }
        return Evaluation(Phase.ACTIVE, remaining, remaining)
    }

    fun activeFor(sessions: List<BlockingSession>, packageName: String?, now: TimeSnapshot): BlockingSession? =
        sessions.firstOrNull { it.packageName == packageName && evaluate(it, now).phase == Phase.ACTIVE }

    fun nextBoundary(sessions: List<BlockingSession>, now: TimeSnapshot): Long? =
        sessions.mapNotNull { evaluate(it, now).nextBoundaryIn }.minOrNull()

    /** Next local start, then first local end strictly after it; uses calendar days across DST. */
    fun interval(now: Long, start: LocalTime, end: LocalTime, zone: ZoneId): Pair<Long, Long> {
        val instant = Instant.ofEpochMilli(now)
        var date = instant.atZone(zone).toLocalDate()
        var from = date.atTime(start).atZone(zone)
        if (from.toInstant() < instant) {
            date = date.plusDays(1)
            from = date.atTime(start).atZone(zone)
        }
        var until = date.atTime(end).atZone(zone)
        if (!until.toInstant().isAfter(from.toInstant())) until = date.plusDays(1).atTime(end).atZone(zone)
        return from.toInstant().toEpochMilli() to until.toInstant().toEpochMilli()
    }
}
