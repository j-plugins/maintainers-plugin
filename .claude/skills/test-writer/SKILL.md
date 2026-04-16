---
name: test-writer
description: Writing unit and IntelliJ Platform tests for the Maintainers plugin. Use when adding tests under src/test/kotlin/, testing a new MaintainerProvider, testing aggregation/filter logic, or deciding between plain JUnit and BasePlatformTestCase. Covers the JUnit 4 + IntelliJ test framework stack this repo uses.
---

# Test writer skill

Test stack for this repository:

- **JUnit 4** (`libs.junit` = 4.13.2) + `opentest4j` as the runner.
- **IntelliJ Platform test framework** pulled in via `intellijPlatform { testFramework(TestFrameworkType.Platform) }` in `build.gradle.kts`. That provides `BasePlatformTestCase`, `HeavyPlatformTestCase`, `LightPlatformTestCase`, `CodeInsightTestFixture`, etc.
- **Kover** runs on every `./gradlew check` and writes `build/reports/kover/report.xml` (uploaded to Codecov in CI).
- No Mockito, no MockK, no Kotest — keep it vanilla JUnit and real platform fixtures.

## Choosing the right test style

Walk down this list. Stop at the first match.

1. **Pure function / data class / collection pipeline** (no `Project`, no VFS, no PSI, no extension points).
   → Plain JUnit 4 test class in `src/test/kotlin/`. Fastest, no platform boot. See `util/LinkUtilsTest.kt` and `extension/MaintainerProviderTest.kt` — the latter tests `MaintainerProvider.Companion.aggregate(list)` directly without any project fixture.
2. **Needs a real `Project` but no Java PSI or custom SDK** (most provider tests that read a VirtualFile, indexing, settings services).
   → Extend `BasePlatformTestCase`. Access the test project via `myFixture.project`, write lock files with `myFixture.tempDirFixture.createFile("composer.lock", json)` or `myFixture.configureByText(...)` for in-memory files. Light tests reuse the same project across runs via `LightProjectDescriptor` — much faster than heavy tests.
3. **Multi-module projects or full SDK setup** (not currently needed here).
   → Extend `HeavyPlatformTestCase`. Prefer only if you can prove a light test can't do it.
4. **UI driven flows** (tool window, tree interactions).
   → `runIdeForUiTests` task + the robot-server plugin (configured in `build.gradle.kts`). Writing UI tests is out of scope for most changes — if you think you need one, confirm with the user first.

## Running tests

- All tests: `./gradlew check`.
- One class: `./gradlew test --tests "com.github.xepozz.maintainers.util.LinkUtilsTest"`.
- One method: append `.methodName` (e.g. `"...LinkUtilsTest.test deduplicateLinks case insensitivity"` — backticks in Kotlin test names map to spaces here).
- Coverage report: open `build/reports/kover/html/index.html` after `./gradlew koverHtmlReport`.

## Conventions for this repo

- Source layout mirrors production: a class at `com.github.xepozz.maintainers.foo.Bar` has its test at `src/test/kotlin/com/github/xepozz/maintainers/foo/BarTest.kt`.
- Test method names use Kotlin backticks with plain-English sentences: `` `test aggregate includes dependencies without maintainers` ``. Do not prefix with `test` in camelCase.
- Use `org.junit.Assert.assertEquals`/`assertTrue`/`assertFalse` imports (not JUnit 5 `Assertions`). `opentest4j` is on the classpath but assertions come from JUnit 4.
- Keep assertions close to the behavior, not the implementation: assert on `AggregatedData.maintainerMap` shape and `allDependencies` contents, not on internal mutable map calls.

## Testing `MaintainerProvider` implementations

The canonical strategy for a parser-style provider (Composer, NPM, Go):

1. Test `parseLockFile`-equivalent logic by extracting it into a `private fun` that takes a `String`/`ByteArray` and returns `Collection<Dependency>`. Then exercise it with raw test fixtures — no `Project` needed.
2. Integration-test the full `getDependencies(project)` flow with `BasePlatformTestCase` + `myFixture.addFileToProject("composer.lock", fixtureJson)` and assert on the flattened result.
3. Test `MaintainerProvider.Companion.aggregate(...)` independently when you change merge semantics — `MaintainerProviderTest` is the template.

When writing a fixture JSON, keep it to the minimal field set the parser actually reads (`name`, `version`, `authors[].name/email/homepage`, `funding[].type/url`, `source.url` for Composer). Don't paste real `composer.lock` files of thousands of lines.

## `BasePlatformTestCase` cheat sheet

```kotlin
class MyProviderTest : BasePlatformTestCase() {
    fun `test parses composer lock`() {
        val file = myFixture.addFileToProject("composer.lock", """{"packages":[...]}""")
        val deps = ComposerMaintainerProvider().getDependencies(myFixture.project)
        assertEquals(1, deps.size)
        assertEquals("vendor/pkg", deps.first().name)
    }
}
```

Key points:

- JUnit 4 test methods on `BasePlatformTestCase` need the `test` prefix (either `fun testFoo()` or `fun \`test foo\`()`) — the framework discovers them by name.
- Call `super.setUp()` / `super.tearDown()` if you override those.
- `myFixture.project` is reused across tests; any mutable state you add (settings service values, registered extensions) must be cleaned in `tearDown`.
- To register a test-only extension, use `com.intellij.testFramework.PlatformTestUtil.registerExtension(EP_NAME, impl, testRootDisposable)`.

## What to test in this codebase

- Aggregation merge rules in `MaintainerProvider.aggregate`: `null`-coalescing of fields, funding de-dup by URL, cross-provider maintainer collapsing by name.
- `SearchFilter.parse(...)` tokenisation of `is:funding` and `pm:<name>`; round-tripping via `toText()`.
- Tree grouping invariants in `MaintainersTreeStructure` (group-by-package-manager × group-by-prefix combinations, filter interactions). This one needs `BasePlatformTestCase` because the structure pulls a project-level settings service.
- Provider parsers: malformed JSON returns empty; dev vs prod buckets in Composer; NPM `node_modules/` vs `packages/` workspace prefixes; Go module-path prefix dispatch table.

## Things to avoid

- Don't add mocking libraries. If a function is hard to test without mocks, refactor it to take plain data.
- Don't ship fixture files under `src/test/resources` unless you have many cases reusing the same payload; inline small JSON strings in the test instead.
- Don't rely on file-system ordering or `Set<T>` iteration order — sort before asserting.
- Don't print to stdout in tests; use `assertEquals` with readable messages.

## References

- Testing overview: https://plugins.jetbrains.com/docs/intellij/testing-plugins.html
- Light vs heavy tests: https://plugins.jetbrains.com/docs/intellij/light-and-heavy-tests.html
- Test fixtures / `CodeInsightTestFixture`: https://plugins.jetbrains.com/docs/intellij/test-project-and-testdata-directories.html
