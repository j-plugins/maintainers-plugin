# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

JetBrains IDE plugin (IntelliJ Platform) that parses project lock files and IDE plugin descriptors, aggregates maintainers behind each dependency, and renders them in a dedicated tool window with funding links. Plugin id `com.github.xepozz.maintainers`, distributed on JetBrains Marketplace as "Maintainers".

Built on the [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template). Kotlin + Gradle Kotlin DSL, JVM toolchain 21, target platform `2025.1.1` (`pluginSinceBuild = 251`).

## Common commands

All commands go through the Gradle wrapper:

- `./gradlew buildPlugin` — produce the distributable zip under `build/distributions/`.
- `./gradlew runIde` — launch a sandbox IDE with the plugin loaded (matches `.run/Run Plugin.run.xml`). Use the `playground/` subprojects (`go-1`, `npm-1`, `php-1`, `php-2`) as sample workspaces to exercise each provider.
- `./gradlew check` — runs unit tests and Kover coverage (`build/reports/kover/report.xml`). CI uses this task.
- `./gradlew test --tests "com.github.xepozz.maintainers.util.LinkUtilsTest"` — run a single test class. Append `.methodName` to run a single method.
- `./gradlew verifyPlugin` — runs plugin structure verification and the IntelliJ Plugin Verifier against the `recommended()` IDE set. Required green in CI.
- `./gradlew runIdeForUiTests` — launches the sandbox IDE with `robot-server` on port 8082 for UI tests (see `intellijPlatformTesting { runIde { ... } }` in `build.gradle.kts`).

Version catalog lives in `gradle/libs.versions.toml`; plugin metadata and supported IDE range come from `gradle.properties` (`pluginVersion`, `pluginSinceBuild`, `platformVersion`). Update these rather than hardcoding versions.

## Architecture

### Extension-point-driven provider model

The plugin exposes a single extension point `com.github.xepozz.maintainers.maintainerProvider` (declared in `src/main/resources/META-INF/plugin.xml`) with interface `extension.MaintainerProvider`. Each provider implements:

- `val packageManager: PackageManager` — identity + icon for grouping/filtering.
- `fun getDependencies(project: Project): Collection<Dependency>` — parses lock files / IDE state into `Dependency` objects carrying embedded `Maintainer`s.

Current providers (all registered in `plugin.xml`):

- `providers.composer.ComposerMaintainerProvider` — `composer.lock` via `FilenameIndex`; reads `authors[]` + `funding[]`.
- `providers.npm.NpmMaintainerProvider` — `package-lock.json` via `FilenameIndex`; NPM lock v2/v3 `packages{}` map (authors are currently not populated — lock files don't contain them).
- `providers.go.GoMaintainerProvider` — `go.sum`; derives maintainers heuristically from module path prefixes (`github.com/<org>`, `golang.org/x/`, `go.uber.org/`, etc.).
- `providers.ide.IdeMaintainerProvider` — iterates `PluginManager.getPlugins()` and maps vendor metadata.

Adding a new provider: implement `MaintainerProvider`, create a `PackageManager` singleton with an icon (register in `MaintainersIcons`), add a `<maintainerProvider>` entry in `plugin.xml` under the `com.github.xepozz.maintainers` namespace. Providers should use `FilenameIndex.getVirtualFilesByName(..., GlobalSearchScope.projectScope(project))` for lock-file lookup so they respect project scope and indexing.

### Aggregation pipeline

`MaintainerProvider.Companion.getAggregatedData(project)` is the single entry point used by the UI. It calls every registered provider, flattens results, and `aggregate()` merges duplicate maintainers by **name** — later occurrences fill `null` fields (email/homepage/github/icon) and union `fundingLinks` deduplicated by URL. The result is an `AggregatedData(maintainerMap: Map<Maintainer, List<Dependency>>, allDependencies: List<Dependency>)`.

Key invariant: aggregation is keyed on `maintainer.name`, so provider-specific name normalization directly affects grouping quality.

### Tool window UI (Swing)

Entry: `toolWindow.MaintainersToolWindowFactory` → `MaintainersToolWindowPanel(project)`. Layout:

- Left: JBSplitter with a `Tree` fed by `StructureTreeModel(MaintainersTreeStructure, …)` wrapped in `AsyncTreeModel`. The tree has two top-level `GroupHeader`s: "maintainers" and "dependencies". Dependency grouping is configurable by `groupByPackageManager` and `groupByPrefix` (both persisted per project via `services.MaintainersSettingsService`, a `@Service(Service.Level.PROJECT)` `PersistentStateComponent` stored in `maintainers_settings.xml`).
- Right: details container that swaps between `details.MaintainerDetailsPanel` and `details.PackageDetailsPanel` depending on selection; shows `EmptyStatePanel` with `MaintainersStats` (top maintainers, counts) when nothing is selected.
- Toolbar: search field + action group (funding-only toggle, refresh, details toggle, group-by submenu). Search uses `model.SearchFilter.parse(text)` which recognises tokens `is:funding` and `pm:<name>` and mixes them with free-text query; `SearchFilterController` wires the text field and the `PackageManagerFilterPanel` chips to a single filter.

Filtering is applied in two places — `MaintainersTreeStructure.setFilter(...)` for the tree, and `MaintainersToolWindowPanel.applyFilter()` for the status-bar counts. Keep these two paths in sync when changing filter semantics.

### i18n / bundle

User-visible strings live in `src/main/resources/messages/MaintainersBundle.properties` and are accessed via `MaintainersBundle.message("key", ...)`. Action text, tree group titles, and status line all come from the bundle.

## Testing stack

JUnit 4 (`libs.junit` 4.13.2) + `opentest4j`. Tests that exercise the IntelliJ Platform extend the platform test framework (`intellijPlatform { testFramework(TestFrameworkType.Platform) }`). Pure logic tests (see `util/LinkUtilsTest`, `extension/MaintainerProviderTest`) are plain JUnit and don't need the platform fixture — prefer that for provider aggregation/parsing logic. Platform tests are required when you need a real `Project`, VFS, or indexing.

Kover XML report is generated on every `check` (configured in `build.gradle.kts`).

## CI

`.github/workflows/build.yml` runs on push to `main` and all PRs: `buildPlugin` → parallel `check` (tests + Kover → Codecov), Qodana, and `verifyPlugin`. A draft GitHub release is created on `main` pushes. `.github/workflows/release.yml` publishes to Marketplace using `CERTIFICATE_CHAIN`, `PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD`, `PUBLISH_TOKEN` secrets (configured in `intellijPlatform { signing/publishing }`).

## Plugin description source of truth

`build.gradle.kts` extracts the Marketplace description from `README.md` between the `<!-- Plugin description -->` / `<!-- Plugin description end -->` markers at build time. Edit the README (not `plugin.xml`) to change the Marketplace listing; the build will fail if the markers go missing.
