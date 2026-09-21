# UniManager

UniManager is the optional companion app for UniPatches. The first implementation
stores patched-app registrations locally, displays their label, package name, and
icon, and exposes a versioned Binder bridge for patched APKs.

The bridge is intentionally optional. UniPatches clients embed their patch-time
defaults and continue working when this app is stopped, uninstalled, incompatible,
or when a registered app is removed from this app.

## Bridge contract

- Service action: `com.zanuaimi.unimanager.BRIDGE`
- Bridge permission: `com.zanuaimi.unimanager.permission.BRIDGE` (normal protection,
  because patched APKs are signed by their original app owners rather than by
  UniManager)
- Protocol version: `1`
- Binder transactions: `1` register, `2` read, `3` update
- Registration payloads are JSON and include `package_name`, `app_label`, and
  independently versioned capability identifiers such as `overlay.config.v2`.
  The package name is the app identity: repeated registrations update the same
  entry, while cloned apps remain separate when their package names differ.

The app currently provides the first functional manager UI. Settings screens,
backups, profiles, and save slots remain later phases.

The current basic UI includes an app details screen with patch and capability
versions, compatibility/repatch status, and startup controls for supported ad
capabilities. Unknown registration fields and capabilities are retained in the
stored JSON record so newer patch versions do not get erased by an older manager.

Configuration values are rendered from the registration payload. Boolean values,
colors, file and folder inputs, and list-valued settings are supported. List
values such as Universal Overlay multi-part icon definitions open in a dedicated
editor where each string can be added, edited, or removed before saving.

When UniManager opens, it scans installed applications for the UniPatches
registration marker embedded in integrated APK manifests. The scanner throttles
package-manager scans, fingerprints each integrated APK, and skips unchanged
registrations. This lets an integrated app appear after installation even
before its first launch without repeatedly decoding and rewriting every app.
The runtime Binder bridge remains available for startup configuration and live
runtime synchronization.

Patched apps use the read transaction during normal launches. Registration is
performed from embedded APK metadata during the manager scan, so launch-time
reads cannot overwrite manager settings. A newly patched APK or newly added
capability can extend an existing record, and registration is committed before
the bridge acknowledges it so a process crash cannot acknowledge an unsaved
registration.

## Signed releases

The release workflow requires these GitHub Actions secrets:

- `UNIMANAGER_KEYSTORE_BASE64`
- `UNIMANAGER_KEYSTORE_PASSWORD`
- `UNIMANAGER_KEY_ALIAS`
- `UNIMANAGER_KEY_PASSWORD`

Create the keystore once, convert it to Base64, and add the resulting values as
repository secrets. Keep the keystore and passwords outside the repository. The
workflow decodes the keystore into the temporary runner directory, signs the
release APK, and uploads the signed APK to the GitHub release.
