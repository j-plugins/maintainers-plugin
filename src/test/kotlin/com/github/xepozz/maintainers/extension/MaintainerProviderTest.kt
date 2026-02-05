package com.github.xepozz.maintainers.extension

import com.github.xepozz.maintainers.model.Dependency
import com.github.xepozz.maintainers.model.Maintainer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MaintainerProviderTest {

    @Test
    fun `test aggregate includes dependencies without maintainers`() {
        val dependencyWithNoMaintainers = Dependency(
            name = "no-maintainers",
            version = "1.0.0",
            source = "test",
            maintainers = emptyList()
        )
        val maintainer = Maintainer(name = "John Doe")
        val dependencyWithMaintainer = Dependency(
            name = "has-maintainer",
            version = "2.0.0",
            source = "test",
            maintainers = listOf(maintainer)
        )

        val allDependencies = listOf(dependencyWithNoMaintainers, dependencyWithMaintainer)
        val aggregated = MaintainerProvider.aggregate(allDependencies)

        // Проверяем, что в списке всех зависимостей есть обе
        val allDepsInResult = aggregated.allDependencies
        
        assertTrue("Dependency with no maintainers should be present in allDependencies", 
            allDepsInResult.any { it.name == "no-maintainers" })
        assertTrue("Dependency with maintainer should be present in allDependencies", 
            allDepsInResult.any { it.name == "has-maintainer" })
    }
}
