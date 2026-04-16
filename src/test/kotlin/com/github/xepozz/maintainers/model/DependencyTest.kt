package com.github.xepozz.maintainers.model

import com.github.xepozz.maintainers.testutil.TestPackageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DependencyTest {

    private fun dep(name: String) = Dependency(
        name = name,
        version = "1.0.0",
        source = TestPackageManager
    )

    @Test
    fun `test groupPrefix returns scope for npm-style scoped package`() {
        assertEquals("@angular", dep("@angular/core").groupPrefix)
    }

    @Test
    fun `test groupPrefix returns nested path for scoped package with extra segments`() {
        // @scope/sub/pkg → substringBeforeLast("/") → "@scope/sub"
        assertEquals("@babel/plugin", dep("@babel/plugin/transform").groupPrefix)
    }

    @Test
    fun `test groupPrefix returns vendor for composer-style namespaced package`() {
        assertEquals("symfony", dep("symfony/console").groupPrefix)
    }

    @Test
    fun `test groupPrefix returns first segment for go module path`() {
        // Go module paths contain multiple slashes; prefix is the first segment only.
        assertEquals("github.com", dep("github.com/google/uuid").groupPrefix)
    }

    @Test
    fun `test groupPrefix is null when name has no slash`() {
        assertNull(dep("lodash").groupPrefix)
    }

    @Test
    fun `test groupPrefix is null for scoped name without slash`() {
        // Starts with @ but no / → treated as plain name (no slash condition fails first branch).
        assertNull(dep("@solo").groupPrefix)
    }

    @Test
    fun `test groupPrefix handles empty name defensively`() {
        assertNull(dep("").groupPrefix)
    }
}
