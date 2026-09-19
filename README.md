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

The app is deliberately functional before visual polish. Settings screens,
backups, profiles, save slots, and richer capability editing are later phases.

The current basic UI includes an app details screen with patch and capability
versions, compatibility/repatch status, and startup controls for supported ad
capabilities. Unknown registration fields and capabilities are retained in the
stored JSON record so newer patch versions do not get erased by an older manager.

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
