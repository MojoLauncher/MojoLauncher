# Note about placements

This commit moves the main UI activity, navigation and core Compose screens and essential resources into the app module (app/src/main/...). It also sets the AndroidManifest package to com.xl.launcher and points the launcher icon to @mipmap/ic_launcher.

If you still see the old icon after installing, please uninstall the previous app from your device first (adb uninstall <old.package.name>) then reinstall the new debug APK. Android may cache icons for apps with the same signature but different package.
