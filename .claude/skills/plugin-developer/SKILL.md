---
name: plugin-developer
description: Building on IntelliJ Platform APIs in this plugin. Use when adding extension points, services, actions, tool window content, persistent settings, or integrating with the platform (VFS, PSI, indexing, PluginManager). Covers plugin.xml wiring, Gradle IntelliJ Platform plugin 2.x tasks, signing/publishing, and Marketplace expectations.
---

# Plugin developer skill

Guide for everything that touches the IntelliJ Platform surface area in this repo: `plugin.xml`, extension points, services, actions, tool windows, `build.gradle.kts` (IntelliJ Platform Gradle Plugin 2.x).

Target baseline: `platformVersion = 2025.1.1`, `pluginSinceBuild = 251`, JVM toolchain 21.

## Repository wiring at a glance

- Plugin manifest: `src/main/resources/META-INF/plugin.xml`. Single `<id>com.github.xepozz.maintainers</id>`, depends on `com.intellij.modules.platform` only (no Java-specific modules — works in all IntelliJ-based IDEs).
- Resource bundle: `messages.MaintainersBundle` declared via `<resource-bundle>`. All user-visible strings go through `MaintainersBundle.message("key")`.
- Gradle: `build.gradle.kts` uses the `org.jetbrains.intellij.platform` Gradle plugin. Versions live in `gradle/libs.versions.toml`. IDE and bundled plugin dependencies are driven by `gradle.properties` (`platformVersion`, `platformBundledPlugins`, `platformPlugins`, `platformBundledModules`).
- Plugin description for Marketplace is extracted from `README.md` between the `<!-- Plugin description -->` markers at build time. Edit the README, not `plugin.xml`.

## Extension point this plugin exposes

```xml
<extensionPoints>
    <extensionPoint
            name="maintainerProvider" dynamic="true"
            interface="com.github.xepozz.maintainers.extension.MaintainerProvider"/>
</extensionPoints>
```

- `dynamic="true"` lets third-party plugins register/unregister providers at runtime without restart. Keep it dynamic when adding new EPs to this plugin.
- The EP is retrieved via the companion constant:
  ```kotlin
  val EP_NAME = ExtensionPointName.create<MaintainerProvider>(
      "com.github.xepozz.maintainers.maintainerProvider"
  )
  ```
- Extensions are registered under the plugin's own namespace:
  ```xml
  <extensions defaultExtensionNs="com.github.xepozz.maintainers">
      <maintainerProvider implementation="..."/>
  </extensions>
  ```
  For platform extensions (tool window, startup activity, language extensions, etc.) use `defaultExtensionNs="com.intellij"` instead.
- Implementations must be stateless `class` types (never `object`) with a no-arg constructor. Expensive work belongs in method calls, not the constructor.

## Adding a new `MaintainerProvider`

1. Create `providers/<ecosystem>/<Name>PackageManager.kt` — an `object` implementing `model.PackageManager` with a `name: String` and `icon: Icon` (register the icon in `MaintainersIcons`, add SVGs under `src/main/resources/icons/<ecosystem>/`).
2. Create `providers/<ecosystem>/<Name>MaintainerProvider.kt` — a `class` implementing `extension.MaintainerProvider`:
   - `override val packageManager = <Name>PackageManager`
   - `override fun getDependencies(project: Project): Collection<Dependency>`
3. Use `FilenameIndex.getVirtualFilesByName("<lockfile>", GlobalSearchScope.projectScope(project))` to find lock files. Don't walk the VFS; it's slow and doesn't respect content roots.
4. Define a provider-local `data class <Name>Metadata(...) : DependencyMetadata` for ecosystem-specific flags (`isDev`, `isBundled`, etc.).
5. Register in `plugin.xml`:
   ```xml
   <maintainerProvider implementation="com.github.xepozz.maintainers.providers.<ecosystem>.<Name>MaintainerProvider"/>
   ```
6. Add localized strings to `MaintainersBundle.properties` if the UI surfaces new text.
7. Drop a sample fixture project under `playground/<ecosystem>-N/` for manual testing via `./gradlew runIde`.
8. Write unit tests (see the `test-writer` skill).

## Services pattern

This plugin uses **light services** (annotation-only, no `plugin.xml` entry). See `services.MaintainersSettingsService`:

```kotlin
@Service(Service.Level.PROJECT)
@State(name = "MaintainersSettings", storages = [Storage("maintainers_settings.xml")])
class MaintainersSettingsService : BaseState(), PersistentStateComponent<MaintainersSettingsService> {
    var groupByPackageManager by property(true)
    override fun getState() = this
    override fun loadState(state: MaintainersSettingsService) = copyFrom(state)

    companion object {
        fun getInstance(project: Project) = project.service<MaintainersSettingsService>()
    }
}
```

Rules:
- Light services must be `final` (Kotlin default), no constructor args other than `Project` for project-level services.
- Retrieve via `project.service<T>()` or `service<T>()` for app-level. Never cache a service reference across scopes.
- For persistent state extend `BaseState` and use its property delegates (`property(default)`, `string(default)`, `list<T>()`, `map<K,V>()`). `BaseState` tracks modifications and skips serialization when values match defaults.
- Don't put long-running work in service constructors — they can be instantiated during indexing, startup, or on any thread.

## Actions and toolbars

The tool window builds its toolbar programmatically via `ActionManager.getInstance().createActionToolbar(...)` in `MaintainersToolWindowPanel.setupToolbar()`. Pattern:

- Group-by toggles use `ToggleAction` with `isSelected(e)` reading state and `setSelected(e, state)` writing it back (usually via the settings service or the tree structure).
- One-shot commands use `AnAction` with `actionPerformed(e)`.
- Labels and descriptions come from the bundle: `MaintainersBundle.message("action.refresh.text")`, `"action.refresh.description"`.
- Set `targetComponent` on the `ActionToolbar` to enable keyboard shortcut resolution.
- If you add a persistent action (main menu, context menu), register it in `plugin.xml` under `<actions>` with an `id`, group, and `text`/`description` keyed against the bundle.

## Tool windows

Declared via `<toolWindow>` extension in `plugin.xml`:

```xml
<toolWindow id="Maintainers" anchor="bottom" secondary="true" canCloseContents="false"
            icon="/icons/maintainers/icon.svg"
            factoryClass="com.github.xepozz.maintainers.toolWindow.MaintainersToolWindowFactory"/>
```

- `factoryClass` implements `ToolWindowFactory.createToolWindowContent(project, toolWindow)` — lazy, runs only when the user opens it.
- Override `shouldBeAvailable(project)` to hide the tool window for irrelevant projects; currently `true` because any project could theoretically have lock files.
- Add content via `ContentFactory.getInstance().createContent(panel, displayName, isLockable)`.
- Make the root panel implement `Disposable` so tree models and toolbars get cleaned up when the tool window closes (see `MaintainersToolWindowPanel : SimpleToolWindowPanel, Disposable`).
- Trees should use `StructureTreeModel(AbstractTreeStructure, disposable)` wrapped in `AsyncTreeModel` — never block the EDT to build tree nodes.

## Threading rules

- **EDT**: Swing event handlers (action listeners, `TreeSelectionListener`, document listeners).
- **BGT**: file parsing, indexing queries, HTTP, image decoding.
- Parsing and aggregation in `MaintainerProvider.getAggregatedData(project)` should be invoked from a background thread. If you wire a new trigger, wrap it with `ApplicationManager.getApplication().executeOnPooledThread { ... }` or `ProgressManager.getInstance().run(Task.Backgroundable(...))` and marshal the result back to the EDT with `invokeLater { ... }` or `ApplicationManager.getApplication().invokeLater(..., ModalityState.stateForComponent(component))`.
- PSI/VFS reads need a read action: `ReadAction.compute<T, Throwable> { ... }` or `runReadAction { ... }`. `FilenameIndex` lookups already take care of this internally.
- Never write to the VFS from the EDT without `WriteCommandAction.runWriteCommandAction(project) { ... }`.

## Icons

- SVGs under `src/main/resources/icons/<area>/`. Use `icon_dark.svg` suffix for the dark theme variant — the platform picks it automatically.
- Loaded via `IconLoader.getIcon("/icons/<area>/icon.svg", MaintainersIcons::class.java)` inside `MaintainersIcons`.
- Marketplace plugin icon is `src/main/resources/META-INF/pluginIcon.svg`.

## Gradle plugin 2.x essentials

`build.gradle.kts` uses the `intellijPlatform` DSL:

```kotlin
intellijPlatform {
    intellijIdea(providers.gradleProperty("platformVersion"))   // target IDE
    bundledPlugins(listOf("com.intellij.java", ...))            // deps on bundled plugins
    plugins(listOf("org.intellij.scala:2023.3.27"))             // Marketplace plugin deps
    testFramework(TestFrameworkType.Platform)                   // unit test fixtures
}
```

Common tasks:
- `buildPlugin` — produces the distributable zip in `build/distributions/`.
- `runIde` — starts a sandbox IDE with the plugin. Configured project paths are isolated per Gradle invocation.
- `verifyPlugin` — runs IntelliJ Plugin Verifier against the IDE set returned by `pluginVerification.ides { recommended() }`. Required green for releases.
- `runIdeForUiTests` — sandbox IDE with the robot-server plugin on port 8082 (see `intellijPlatformTesting.runIde` registration).
- `check` — tests + Kover coverage (CI target).

Signing and publishing are already wired to env vars (`CERTIFICATE_CHAIN`, `PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD`, `PUBLISH_TOKEN`). Release channel is derived from the SemVer pre-release label.

## Compatibility

- Only use APIs that exist in `platformVersion` 2025.1.1 or later. If you need a newer API, bump `platformVersion` and `pluginSinceBuild` together.
- Don't use `@ApiStatus.Internal` APIs. The plugin should compile cleanly without `-Xjvm-default=all` hacks or reflection against internal classes.
- Plugin depends on `com.intellij.modules.platform` only. If you start using Java-specific APIs (PSI `PsiClass`, `JavaPsiFacade`), you must add `<depends>com.intellij.modules.java</depends>` and accept that the plugin will stop loading in non-Java IDEs (PhpStorm, GoLand, RubyMine, etc.). Prefer ecosystem-agnostic APIs — that's the point of the current provider design.

## Before opening a PR

- `./gradlew buildPlugin` succeeds.
- `./gradlew check` is green.
- `./gradlew verifyPlugin` reports no compatibility issues against the recommended IDE set.
- `./gradlew runIde` loads the sandbox IDE; opening a `playground/<ecosystem>-N/` project shows the Maintainers tool window populated.
- If `plugin.xml` changed, open it once in IntelliJ — the IDE validates schema and flags bad EP references inline.

## References

- Plugin structure: https://plugins.jetbrains.com/docs/intellij/plugin-structure.html
- Extension points: https://plugins.jetbrains.com/docs/intellij/plugin-extensions.html , https://plugins.jetbrains.com/docs/intellij/plugin-extension-points.html
- Services: https://plugins.jetbrains.com/docs/intellij/plugin-services.html
- Persisting state: https://plugins.jetbrains.com/docs/intellij/persisting-state-of-components.html
- Actions: https://plugins.jetbrains.com/docs/intellij/basic-action-system.html
- Tool windows: https://plugins.jetbrains.com/docs/intellij/tool-windows.html
- Threading: https://plugins.jetbrains.com/docs/intellij/threading-model.html
- Kotlin in plugins: https://plugins.jetbrains.com/docs/intellij/using-kotlin.html
- Gradle IntelliJ Platform plugin: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
- Marketplace publishing: https://plugins.jetbrains.com/docs/marketplace/
