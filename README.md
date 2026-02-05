# Maintainers

![Build](https://github.com/j-plugins/maintainers-plugin/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/30063-maintainers.svg)](https://plugins.jetbrains.com/plugin/30063-maintainers)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/30063-maintainers.svg)](https://plugins.jetbrains.com/plugin/30063-maintainers)

<!-- Plugin description -->

[Github](https://github.com/j-plugins/maintainers-plugin) | [Telegram](https://t.me/jb_plugins/755) | [Donation](https://github.com/xepozz/xepozz?tab=readme-ov-file#become-a-sponsor)

## Maintainers

You trust dozens of packages. Do you know who's behind them?

Maintainers plugin shows you the people and organizations behind every dependency in your project — their GitHub profiles, how many packages they maintain, and how to support them.

One abandoned package can break your build. One mass-resignation can tank your stack. Know your supply chain.

**What you get:**

- Automatic parsing of lock files:
  - `composer.lock` for PHP
  - `package-lock.json` for JavaScript
  - `go.sum` for Go
- GitHub avatars and org icons for instant recognition
- Two views: browse by package or by maintainer
- Direct links to funding pages — sponsor the people who keep your code running

**Why it matters:**

That mass layoff at a tech company? Some of those engineers maintained packages you depend on. That solo dev who burned out? Maybe they wrote your HTTP client.

Maintainers plugin turns your dependency tree from a list of strings into a map of humans.

## Support the Project

If you find this plugin useful, you can support its development:

[<img height="28" src="https://github.githubassets.com/assets/patreon-96b15b9db4b9.svg"> Patreon](https://patreon.com/xepozz)
|
[<img height="28" src="https://github.githubassets.com/assets/buy_me_a_coffee-63ed78263f6e.svg"> Buy me a coffee](https://buymeacoffee.com/xepozz)
|
[<img height="28" src="https://boosty.to/favicon.ico"> Boosty](https://boosty.to/xepozz)


<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "Maintainers"</kbd> >
  <kbd>Install</kbd>

- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/30063-maintainers) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/30063-maintainers/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/j-plugins/maintainers-plugin/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
