package com.khadka.limitr.usage

/** Streaming union of observed foreground intervals, clipped to [begin, end). */
internal class ForegroundUsage(private val begin: Long, private val end: Long, private val multiResume: Boolean) {
    enum class Kind { RESUME, PAUSE, STOP, SCREEN_OFF, SHUTDOWN, STARTUP }
    private data class Active(val since: Long, val activities: MutableSet<String?>)
    private val active = mutableMapOf<String, Active>()
    private val totals = mutableMapOf<String, Long>()

    fun event(at: Long, kind: Kind, pkg: String? = null, activity: String? = null) {
        if (at >= end) return
        when (kind) {
            Kind.SCREEN_OFF, Kind.SHUTDOWN -> active.keys.toList().forEach { close(it, at) }
            // Without a shutdown event, the closing time is unknown. Do not count powered-off time.
            Kind.STARTUP -> active.clear()
            Kind.RESUME -> if (!pkg.isNullOrBlank()) {
                if (!multiResume) active.keys.filter { it != pkg }.forEach { close(it, at) }
                active.getOrPut(pkg) { Active(at, mutableSetOf()) }.activities.add(activity)
            }
            Kind.PAUSE, Kind.STOP -> if (pkg != null) {
                val current = active[pkg] ?: return
                if (activity == null) current.activities.clear() else current.activities.remove(activity)
                if (current.activities.isEmpty()) close(pkg, at)
            }
        }
    }

    fun result(): Map<String, Long> {
        active.keys.toList().forEach { close(it, end) }
        return totals.toMap()
    }

    private fun close(pkg: String, at: Long) {
        val current = active.remove(pkg) ?: return
        val duration = (minOf(at, end) - maxOf(current.since, begin)).coerceAtLeast(0)
        if (duration > 0) totals[pkg] = (totals[pkg] ?: 0) + duration
    }
}
