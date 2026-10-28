package com.khadka.limitr

import com.khadka.limitr.ui.home.model.App
import com.khadka.limitr.usage.AppUsageRanking
import org.junit.Assert.*
import org.junit.Test

class AppUsageRankingTest {
    private fun app(name: String, pkg: String = name) = App(appName = name, appPackageName = pkg)

    @Test fun mostUsedComesFirstEvenWhenRoundedLabelsMatch() {
        val rows = AppUsageRanking.rows(listOf(app("Alpha"), app("Zulu"), app("Unused")),
            mapOf("Alpha" to 60_001, "Zulu" to 60_002))
        assertEquals(listOf("Zulu", "Alpha", "Unused"), rows.map { it.appName })
        assertEquals("0 h 1 m", rows.first().usageDuration)
        assertTrue(rows.first().usagePercentage > rows[1].usagePercentage)
        assertEquals(0.0, rows.last().usagePercentage, 0.0)
    }

    @Test fun percentagesUseOnlyListedAppsAndRetainSmallNonzeroUsage() {
        val rows = AppUsageRanking.rows(listOf(app("Long"), app("Brief")),
            mapOf("Long" to 3_599_000, "Brief" to 1000, "launcher" to 60_000_000))
        assertEquals(100.0, rows.sumOf { it.usagePercentage }, 0.0001)
        assertEquals("<1 m", rows.last().usageDuration)
        assertEquals("<1%", AppUsageRanking.percentageLabel(rows.last().usagePercentage))
        assertTrue(rows.last().usagePercentage > 0)
    }

    @Test fun tiesAndUnusedAppsHaveStableNameThenPackageOrdering() {
        val rows = AppUsageRanking.rows(listOf(app("same", "z"), app("Same", "a"), app("Beta")), emptyMap())
        assertEquals(listOf("Beta", "a", "z"), rows.map { it.appPackageName })
        assertTrue(rows.all { it.usageDuration == "0 h 0 m" && it.usagePercentage == 0.0 })
    }

    @Test fun deniedAccessClearsPreviousIndicatorsAndFallsBackToAlphabeticalOrder() {
        val rows = AppUsageRanking.rows(listOf(app("Zulu").copy(usagePercentage = 90.0, usageDuration = "5 h 0 m"), app("Alpha")), null)
        assertEquals(listOf("Alpha", "Zulu"), rows.map { it.appName })
        assertTrue(rows.all { it.usageDuration == "—" && it.usagePercentage == 0.0 })
    }

    @Test fun invalidDurationsCannotMakeIndicatorsNegativeOrExceed100Percent() {
        val rows = AppUsageRanking.rows(listOf(app("A"), app("B")), mapOf("A" to Long.MAX_VALUE, "B" to -1))
        assertEquals("24 h 0 m", rows.first().usageDuration)
        assertEquals(100.0, rows.first().usagePercentage, 0.0)
        assertEquals(0.0, rows.last().usagePercentage, 0.0)
    }
}
