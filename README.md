# UniManager

<p align="center">
  <img src="images/UniPatchesIcon3.png" alt="UniPatches" width="160">
</p>

<p align="center"><strong>The companion app for UniPatches-enabled APKs.</strong></p>

<p align="center">
  <a href="https://github.com/Zanuaimi/UniManager/releases/latest"><strong>⬇ Download the latest APK</strong></a>
</p>

UniManager is a companion Android app for managing configuration shared by
UniPatches-enabled APKs. It keeps persistent settings outside the patched app,
so supported configuration can survive an APK uninstall, repatch, or temporary
loss of communication with UniManager.

UniManager is optional. Patched apps continue to work with their embedded
patch-time settings when UniManager is not installed or cannot be reached.

## Supported UniPatches integrations

UniManager currently supports these patches from [UniPatches](https://github.com/Zanuaimi/UniPatches):

- **Universal Overlay Patch** - Manage supported overlay appearance, module,
  and runtime settings.
- **Ads Block Patch** - Manage supported ad-format and host-blocking settings.

Integration is enabled from the patch settings in Morphe. Only settings and
capabilities included by the patch are exposed in UniManager.

## What it does

- Discovers installed APKs patched with UniPatches integration.
- Adds each supported app to the Apps list using its app name, package name,
  version, and icon.
- Keeps one app entry per package name, so repatching updates the existing entry.
- Stores patch settings and versioned capabilities such as
  `overlay.config.v2` and `block_ads.v1`.
- Provides per-app configuration for supported settings, including switches,
  colors, file and folder inputs, nested groups, and list editors.
- Includes a dedicated editor for Universal Overlay multi-part icon strings.
- Provides startup configuration and runtime synchronization for supported
  UniPatches integrations.
- Falls back safely to the settings embedded during patching when UniManager is
  unavailable or an app entry has been removed.

## Getting started

1. Install UniManager on the Android device.
2. Patch an APK with a UniPatches patch that supports UniManager integration.
3. Enable UniManager integration in the patch settings.
4. Install the patched APK.
5. Open UniManager and refresh the Apps list if the app does not appear
   automatically.
6. Open the app entry to adjust its supported settings.

Universal Overlay and Ads Block Patch can use UniManager for persistent startup
configuration. Universal Overlay remains optional. A patch continues to work
without UniManager by using the settings selected during patching.

## App sections

- **Apps** - View registered patched apps and open their configuration pages.
- **Backups** - Reserved for backup and restore features.
- **Settings** - Configure pull-to-refresh and automatic refresh behavior.
- **About** - View UniManager information, repository details, and available
  updates.

## Refresh behavior

The Apps list supports manual pull-to-refresh and optional automatic refreshing.
Both behaviors can be enabled or disabled in Settings. The automatic refresh
cooldown accepts seconds, minutes, hours, or days, with a minimum of 30 seconds.

## Compatibility and fallback behavior

UniManager is optional. If it is not installed, is unavailable, or cannot be
reached, the patched APK uses its embedded patch settings. Removing an app entry
from UniManager has the same fallback behavior.

Registrations are keyed by Android package name. Repatching the same package
updates its existing registration and can add newly available patch capabilities.
Cloned APKs with different package names are tracked as separate apps.

## Download and installation

Use the **Download the latest APK** link above, or open the [latest GitHub
release](https://github.com/Zanuaimi/UniManager/releases/latest). Each release
uses a versioned asset name such as `UniManager-1.3.1.apk`, so the latest-release
link always resolves to the newest published version without hardcoding a
version in this README.

Android may require permission to install apps from unknown sources when
installing a release APK outside Google Play.

## Related project

UniManager works with [UniPatches](https://github.com/Zanuaimi/UniPatches), the
Morphe patch collection that provides Universal Overlay, Ads Block Patch, and
other Android patch features.

## For contributors

The project is an Android application written in Kotlin. Build a debug APK with:

```bash
../UniPatches/gradlew assembleDebug
```

The release workflow builds a signed `UniManager-<version>.apk` when the repository's
release signing secrets are configured.
