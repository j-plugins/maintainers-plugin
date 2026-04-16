package com.github.xepozz.maintainers.model

import com.github.xepozz.maintainers.providers.composer.ComposerPackageManager
import com.github.xepozz.maintainers.providers.go.GoPackageManager
import com.github.xepozz.maintainers.providers.ide.IdePackageManager
import com.github.xepozz.maintainers.providers.npm.NpmPackageManager
import com.github.xepozz.maintainers.testutil.TestPackageManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * [SearchFilter.parse] reads the list of known [PackageManager]s from the
 * `com.github.xepozz.maintainers.maintainerProvider` extension point at call time,
 * so these tests boot the platform via [BasePlatformTestCase] — the real providers
 * registered in plugin.xml (Composer, NPM, Go, IDE Plugins) are available.
 */
class SearchFilterTest : BasePlatformTestCase() {

    fun `test parse returns empty filter for blank input`() {
        val filter = SearchFilter.parse("")

        assertEquals("", filter.textQuery)
        assertFalse(filter.fundingOnly)
        assertTrue(filter.packageManagers.isEmpty())
    }

    fun `test parse extracts is funding token and strips it from text`() {
        val filter = SearchFilter.parse("is:funding react")

        assertTrue(filter.fundingOnly)
        assertEquals("react", filter.textQuery)
        assertTrue(filter.packageManagers.isEmpty())
    }

    fun `test parse extracts pm token for composer`() {
        val filter = SearchFilter.parse("pm:Composer")

        assertEquals(setOf(ComposerPackageManager), filter.packageManagers)
        assertEquals("", filter.textQuery)
        assertFalse(filter.fundingOnly)
    }

    fun `test parse extracts multiple pm tokens`() {
        val filter = SearchFilter.parse("pm:NPM pm:Go")

        assertEquals(setOf(NpmPackageManager, GoPackageManager), filter.packageManagers)
        assertEquals("", filter.textQuery)
    }

    fun `test parse combines funding pm and free text query`() {
        val filter = SearchFilter.parse("is:funding pm:Composer symfony")

        assertTrue(filter.fundingOnly)
        assertEquals(setOf(ComposerPackageManager), filter.packageManagers)
        assertEquals("symfony", filter.textQuery)
    }

    fun `test parse normalizes text query to lowercase`() {
        val filter = SearchFilter.parse("React")

        assertEquals("react", filter.textQuery)
    }

    fun `test parse trims surrounding whitespace from text query`() {
        val filter = SearchFilter.parse("   symfony   ")

        assertEquals("symfony", filter.textQuery)
    }

    fun `test parse ignores unknown pm token and leaves it in text query`() {
        val filter = SearchFilter.parse("pm:Unknown react")

        assertTrue(filter.packageManagers.isEmpty())
        // Unknown token stays in the free-text query (lowercased).
        assertEquals("pm:unknown react", filter.textQuery)
    }

    fun `test parse recognises ide plugins package manager whose name contains a space`() {
        val filter = SearchFilter.parse("pm:IDE Plugins")

        assertEquals(setOf(IdePackageManager), filter.packageManagers)
        assertEquals("", filter.textQuery)
    }

    fun `test toText emits funding pm and query in canonical order`() {
        val filter = SearchFilter(
            textQuery = "react",
            fundingOnly = true,
            packageManagers = setOf(NpmPackageManager)
        )

        assertEquals("is:funding pm:NPM react", filter.toText())
    }

    fun `test toText omits missing components`() {
        assertEquals(
            "pm:Composer",
            SearchFilter(packageManagers = setOf(ComposerPackageManager)).toText()
        )
        assertEquals("is:funding", SearchFilter(fundingOnly = true).toText())
        assertEquals("query", SearchFilter(textQuery = "query").toText())
        assertEquals("", SearchFilter().toText())
    }

    fun `test parse then toText round trips for funding and pm tokens`() {
        val original = SearchFilter(
            textQuery = "react",
            fundingOnly = true,
            packageManagers = setOf(NpmPackageManager)
        )
        val roundTripped = SearchFilter.parse(original.toText())

        assertEquals(original.textQuery, roundTripped.textQuery)
        assertEquals(original.fundingOnly, roundTripped.fundingOnly)
        assertEquals(original.packageManagers, roundTripped.packageManagers)
    }

    fun `test toText does not include test-only package manager not registered in EP`() {
        // toText is independent of the EP registry — it writes whatever set it's given.
        val filter = SearchFilter(packageManagers = setOf(TestPackageManager))

        assertEquals("pm:test", filter.toText())
    }
}
