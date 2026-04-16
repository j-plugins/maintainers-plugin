package com.github.xepozz.maintainers.extension

import com.github.xepozz.maintainers.model.Dependency
import com.github.xepozz.maintainers.model.FundingSource
import com.github.xepozz.maintainers.model.Maintainer
import com.github.xepozz.maintainers.testutil.OtherTestPackageManager
import com.github.xepozz.maintainers.testutil.TestPackageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MaintainerProviderTest {

    @Test
    fun `test aggregate includes dependencies without maintainers`() {
        val dependencyWithNoMaintainers = Dependency(
            name = "no-maintainers",
            version = "1.0.0",
            source = TestPackageManager,
            maintainers = emptyList()
        )
        val maintainer = Maintainer(name = "John Doe")
        val dependencyWithMaintainer = Dependency(
            name = "has-maintainer",
            version = "2.0.0",
            source = TestPackageManager,
            maintainers = listOf(maintainer)
        )

        val aggregated = MaintainerProvider.aggregate(
            listOf(dependencyWithNoMaintainers, dependencyWithMaintainer)
        )

        val allDepsInResult = aggregated.allDependencies
        assertTrue(
            "Dependency with no maintainers should be present in allDependencies",
            allDepsInResult.any { it.name == "no-maintainers" }
        )
        assertTrue(
            "Dependency with maintainer should be present in allDependencies",
            allDepsInResult.any { it.name == "has-maintainer" }
        )
    }

    @Test
    fun `test aggregate empty input produces empty output`() {
        val aggregated = MaintainerProvider.aggregate(emptyList())

        assertTrue(aggregated.maintainerMap.isEmpty())
        assertTrue(aggregated.allDependencies.isEmpty())
    }

    @Test
    fun `test aggregate keys maintainers by name`() {
        val maintainer = Maintainer(name = "Jane Doe", email = "jane@example.com")
        val dependency = Dependency(
            name = "pkg-a",
            version = "1.0.0",
            source = TestPackageManager,
            maintainers = listOf(maintainer)
        )

        val aggregated = MaintainerProvider.aggregate(listOf(dependency))

        assertEquals(1, aggregated.maintainerMap.size)
        val key = aggregated.maintainerMap.keys.single()
        assertEquals("Jane Doe", key.name)
        assertEquals("jane@example.com", key.email)
    }

    @Test
    fun `test aggregate fills null fields from later occurrences`() {
        val partial = Maintainer(name = "Alice", email = "alice@example.com")
        val enriched = Maintainer(
            name = "Alice",
            homepage = "https://alice.dev",
            github = "alice",
            icon = "https://github.com/alice.png"
        )

        val aggregated = MaintainerProvider.aggregate(
            listOf(
                Dependency("pkg-1", "1.0.0", TestPackageManager, maintainers = listOf(partial)),
                Dependency("pkg-2", "1.0.0", TestPackageManager, maintainers = listOf(enriched))
            )
        )

        val merged = aggregated.maintainerMap.keys.single { it.name == "Alice" }
        assertEquals("alice@example.com", merged.email)
        assertEquals("https://alice.dev", merged.homepage)
        assertEquals("alice", merged.github)
        assertEquals("https://github.com/alice.png", merged.icon)
    }

    @Test
    fun `test aggregate keeps first non-null value when both occurrences have the field`() {
        val first = Maintainer(name = "Bob", email = "first@example.com")
        val second = Maintainer(name = "Bob", email = "second@example.com")

        val aggregated = MaintainerProvider.aggregate(
            listOf(
                Dependency("pkg-1", "1.0.0", TestPackageManager, maintainers = listOf(first)),
                Dependency("pkg-2", "1.0.0", TestPackageManager, maintainers = listOf(second))
            )
        )

        val merged = aggregated.maintainerMap.keys.single { it.name == "Bob" }
        assertEquals(
            "existing value should win — second occurrence only fills nulls",
            "first@example.com",
            merged.email
        )
    }

    @Test
    fun `test aggregate unions funding links and deduplicates by url`() {
        val patreon = FundingSource("patreon", "https://patreon.com/carol")
        val githubSponsorsA = FundingSource("github", "https://github.com/sponsors/carol")
        val githubSponsorsBDuplicate = FundingSource("custom", "https://github.com/sponsors/carol")
        val buyMeACoffee = FundingSource("buymeacoffee", "https://buymeacoffee.com/carol")

        val maintainerA = Maintainer(
            name = "Carol",
            fundingLinks = listOf(patreon, githubSponsorsA)
        )
        val maintainerB = Maintainer(
            name = "Carol",
            fundingLinks = listOf(githubSponsorsBDuplicate, buyMeACoffee)
        )

        val aggregated = MaintainerProvider.aggregate(
            listOf(
                Dependency("pkg-1", "1.0.0", TestPackageManager, maintainers = listOf(maintainerA)),
                Dependency("pkg-2", "1.0.0", TestPackageManager, maintainers = listOf(maintainerB))
            )
        )

        val merged = aggregated.maintainerMap.keys.single { it.name == "Carol" }
        assertEquals(3, merged.fundingLinks.size)
        assertEquals(
            setOf(
                "https://patreon.com/carol",
                "https://github.com/sponsors/carol",
                "https://buymeacoffee.com/carol"
            ),
            merged.fundingLinks.map { it.url }.toSet()
        )
    }

    @Test
    fun `test aggregate collapses maintainers across different package managers`() {
        val sharedMaintainer = Maintainer(name = "Dan")

        val aggregated = MaintainerProvider.aggregate(
            listOf(
                Dependency("pkg-a", "1.0.0", TestPackageManager, maintainers = listOf(sharedMaintainer)),
                Dependency("pkg-b", "2.0.0", OtherTestPackageManager, maintainers = listOf(sharedMaintainer))
            )
        )

        val entry = aggregated.maintainerMap.entries.single { it.key.name == "Dan" }
        assertEquals(2, entry.value.size)
        assertEquals(
            setOf(TestPackageManager, OtherTestPackageManager),
            entry.value.map { it.source }.toSet()
        )
    }

    @Test
    fun `test aggregate populates packages list from owned dependencies`() {
        val ellie = Maintainer(name = "Ellie")

        val aggregated = MaintainerProvider.aggregate(
            listOf(
                Dependency("pkg-a", "1.0.0", TestPackageManager, maintainers = listOf(ellie)),
                Dependency("pkg-b", "2.3.4", OtherTestPackageManager, maintainers = listOf(ellie))
            )
        )

        val merged = aggregated.maintainerMap.keys.single { it.name == "Ellie" }
        assertEquals(2, merged.packages.size)
        val byName = merged.packages.associateBy { it.name }
        assertEquals("1.0.0", byName.getValue("pkg-a").version)
        assertEquals(TestPackageManager, byName.getValue("pkg-a").packageManager)
        assertEquals("2.3.4", byName.getValue("pkg-b").version)
        assertEquals(OtherTestPackageManager, byName.getValue("pkg-b").packageManager)
    }

    @Test
    fun `test aggregate deduplicates dependency entries for the same maintainer`() {
        val frank = Maintainer(name = "Frank")
        val sameDep = Dependency("pkg-a", "1.0.0", TestPackageManager, maintainers = listOf(frank))

        val aggregated = MaintainerProvider.aggregate(listOf(sameDep, sameDep))

        val deps = aggregated.maintainerMap.entries.single().value
        assertEquals(
            "identical dependencies should be deduplicated in the per-maintainer list",
            1,
            deps.size
        )
        assertEquals(
            "allDependencies preserves every input entry (may include duplicates)",
            2,
            aggregated.allDependencies.size
        )
    }

    @Test
    fun `test aggregate treats maintainers with different names as distinct`() {
        val grace = Maintainer(name = "Grace", email = "grace@example.com")
        val heidi = Maintainer(name = "Heidi", email = "heidi@example.com")

        val aggregated = MaintainerProvider.aggregate(
            listOf(
                Dependency("pkg-a", "1.0.0", TestPackageManager, maintainers = listOf(grace, heidi))
            )
        )

        assertEquals(2, aggregated.maintainerMap.size)
        val byName = aggregated.maintainerMap.keys.associateBy { it.name }
        assertNotNull(byName["Grace"])
        assertNotNull(byName["Heidi"])
    }

    @Test
    fun `test aggregate preserves null fields when nothing to merge with`() {
        val sparse = Maintainer(name = "Ivan")

        val aggregated = MaintainerProvider.aggregate(
            listOf(Dependency("pkg-a", "1.0.0", TestPackageManager, maintainers = listOf(sparse)))
        )

        val merged = aggregated.maintainerMap.keys.single()
        assertNull(merged.email)
        assertNull(merged.homepage)
        assertNull(merged.github)
        assertNull(merged.icon)
        assertTrue(merged.fundingLinks.isEmpty())
    }
}
