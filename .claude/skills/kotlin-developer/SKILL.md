---
name: kotlin-developer
description: Writing idiomatic Kotlin for this IntelliJ plugin. Use when adding or refactoring Kotlin sources under src/main/kotlin/com/github/xepozz/maintainers/, designing data classes/models, writing collection pipelines, or integrating with the IntelliJ Platform from Kotlin. Covers stdlib bundling, allowed language features, and project conventions.
---

# Kotlin developer skill

Applies to all `src/**/*.kt` changes in this repository. The project is Kotlin 2.3.x on JVM toolchain 21, targeting IntelliJ Platform 2025.1.1 (`pluginSinceBuild = 251`). See `gradle/libs.versions.toml` and `gradle.properties` for exact versions.

## Non-negotiables

- **Do not bundle the Kotlin stdlib**. `gradle.properties` sets `kotlin.stdlib.default.dependency = false` — the platform already ships a compatible stdlib. Never add `org.jetbrains.kotlin:kotlin-stdlib*` to `dependencies`.
- **Do not add `kotlinx.coroutines` or other `kotlinx.*` libraries as a dependency.** The IntelliJ Platform bundles a specific `kotlinx-coroutines-core` version. Using our own would shade conflicting classes at runtime. If coroutines are needed, rely on the platform-provided version only.
- **Never use `object` for classes registered in `plugin.xml`.** The platform's extension/service machinery instantiates types reflectively — a Kotlin `object` produces a `INSTANCE` field and package-private constructor, which breaks DI. Providers, `ToolWindowFactory`, `ProjectActivity`, etc. must be `class`. `PackageManager` singletons (`ComposerPackageManager`, `NpmPackageManager`, …) *are* fine as `object` because they are plain data holders referenced from Kotlin code, not registered as extensions.
- **Avoid `companion object` in `plugin.xml`-registered classes** except for simple constants/loggers. It eagerly loads the outer class at IDE startup.

## Project conventions

- Package root: `com.github.xepozz.maintainers`. New packages mirror the functional area (`providers.<ecosystem>`, `toolWindow.<area>`, `model`, `services`, `util`).
- Models live in `model/Models.kt` as top-level `data class`es and are kept provider-agnostic. Add new shared shapes there; put provider-only types (e.g. `ComposerMetadata`, `NpmMetadata`, `IdePluginMetadata`) next to the provider that produces them and implement `DependencyMetadata`.
- User-facing strings go through `MaintainersBundle.message("key", ...)`. Keys live in `src/main/resources/messages/MaintainersBundle.properties`. Never hardcode UI strings.
- Icons come from `MaintainersIcons` (loaded via `IconLoader.getIcon`). Add new SVGs under `src/main/resources/icons/<area>/` and expose them as `val` properties on `MaintainersIcons`.
- Logging: `com.intellij.openapi.diagnostic.thisLogger()`. Don't introduce SLF4J, Logback, or `println`.
- JSON parsing uses `com.google.gson` (transitively available from the platform). Keep the existing style: `JsonParser.parseReader(InputStreamReader(stream))`, check `isJsonObject/isJsonArray`, use `?.asString`/`?.asJsonArray` with null-safe access.

## Idioms favored in this codebase

- Data classes with default values and `copy(...)` merging (see `MaintainerProvider.aggregate` for the canonical merge pattern: keep the first non-null field, union lists via `distinctBy { it.url }`).
- Immutable pipelines using `map/flatMap/filter/groupBy/distinctBy/sortedBy`. Prefer `mapNotNull` + early `return@mapNotNull null` over `if/else` for optional parsing.
- `when` expressions over long `if/else` ladders; use the smart-cast form (`when (element) { is GroupHeader -> ... }`).
- Property delegation for settings: `var groupByPackageManager: Boolean by settings::groupByPackageManager` (see `MaintainersTreeStructure`). For `BaseState`-backed state use the stock delegates (`property(defaultValue)`, `string(...)`, `list()`).
- Extension functions belong in `util/` only when reused. One-shot helpers stay as `private fun` inside their call site (see `extractGithubUsername`, `extractOrgIconUrl` duplicated locally per provider — intentional, keeps providers standalone).

## Threading & IntelliJ Platform interop

- **Never block the EDT.** Expensive work (VFS traversal, file parsing, index queries) must run on a background thread. Providers' `getDependencies(project)` is invoked from `MaintainersToolWindowPanel.refresh()` — check call sites before assuming threading context.
- Use `FilenameIndex.getVirtualFilesByName(name, GlobalSearchScope.projectScope(project))` for lock-file lookup instead of walking the VFS manually; it respects indexing and project scope.
- PSI/VFS reads need a read action (`runReadAction { ... }`). Index queries like `FilenameIndex` already wrap a read action internally but still require the project to be in smart mode — if you add a provider that runs at startup, check `DumbService.isDumb`.
- For UI updates dispatched from a background thread use `ApplicationManager.getApplication().invokeLater { ... }` with an appropriate `ModalityState`. For Swing trees, prefer the platform's `AsyncTreeModel` / `StructureTreeModel` pattern already in `MaintainersToolWindowPanel` over raw `DefaultTreeModel`.
- Nullable platform APIs (`descriptor.vendorUrl`, `pkg.get("funding")`) must be handled defensively — wrap JSON parsing in `try { ... } catch (e: Exception) { null }` and return an empty collection, matching existing providers.

## When writing new Kotlin code

1. Read the nearest existing file in the same package first — style and abstraction level should match.
2. Keep files short and single-purpose; new UI sections generally get their own file under `toolWindow/details/`.
3. Don't introduce `lateinit var` in services/providers — prefer constructor parameters or `by lazy {}`.
4. Don't add extension-heavy DSLs unless you're using Kotlin UI DSL v2 for a settings/dialog panel; the tool window is plain Swing + IntelliJ components by design.
5. If you touch `build.gradle.kts` or `gradle/libs.versions.toml`, run `./gradlew buildPlugin verifyPlugin` locally before considering the change done.

## References

- Kotlin for plugin authors: https://plugins.jetbrains.com/docs/intellij/using-kotlin.html
- Threading model: https://plugins.jetbrains.com/docs/intellij/threading-model.html
- IntelliJ Platform coding guidelines: https://plugins.jetbrains.com/docs/intellij/api-internal.html
