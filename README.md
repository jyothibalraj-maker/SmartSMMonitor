# Social Media Controller (Java Android project)

This is a starter Android Studio project for a transparent, parent-configured usage limiter.

## Intended behaviour
- Android 10+ (minSdk 29); initially target Android 10–12 devices.
- Combined foreground usage allowance: 5 minutes per clock hour.
- Selected app traffic is routed into a local VPN tunnel and dropped after the allowance is exhausted.
- At the next clock-hour boundary the VPN restriction is removed.
- An SMS report is sent to the configured parent's number after each completed hour.
- Usage access, SMS permission and VPN consent are required.

## Important limitations — read before installing
1. **This is a starter implementation, not a certified or device-tested parental-control product.** Build it, install it on a test phone, and validate every selected app and device model before relying on it.
2. Android requires an ongoing foreground-service notification while this kind of background monitoring runs. The app cannot truthfully promise invisible or notification-free monitoring.
3. The user must explicitly grant Usage Access, SMS permission and VPN consent. The VPN indicator is controlled by Android.
4. Automatic SMS may incur carrier charges. SMS permission is sensitive and distribution through Google Play is subject to Play policy; this project is intended for a privately installed, parent-configured build.
5. Some manufacturers aggressively stop background services. Disable battery optimization for this app where the phone's settings permit it. Android 12 and OEM boot restrictions may require reopening the app after a reboot.
6. UsageStats aggregation can be delayed by Android and OEM implementations. The 15-second polling interval means the threshold may be exceeded by a small amount before the next check.
7. VPN routing behaviour, DNS, IPv6 and per-app VPN handling vary by Android release. The sample establishes an IPv4 drop tunnel for the selected packages. Test IPv6 and each target app; do not assume this alone blocks every possible network path on every OEM.
8. This version supports a predefined list of package IDs. Apps not listed are not monitored. Some services (for example YouTube Shorts in a browser) are not counted as YouTube app usage.
9. This sample does not prevent uninstalling, force-stopping, changing permissions, using another device, or bypassing controls through a browser/VPN. For stronger child-device management, consider Android Enterprise/Family Link managed-device controls.

## Build requirements
- Android Studio (current stable release)
- JDK 17
- Android SDK Platform 35
- Android Gradle Plugin 8.7.3 (Gradle 8.9)

## Build APK
1. Extract the ZIP.
2. Open the `SocialMediaController` folder in Android Studio.
3. Allow Gradle sync and install any requested SDK components.
4. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. The debug APK will be at:
   `app/build/outputs/apk/debug/app-debug.apk`

For a command-line build from the project folder:
```bash
gradlew assembleDebug
```
On macOS/Linux use `./gradlew assembleDebug`. If the Gradle wrapper is not present, create it from Android Studio or run `gradle wrapper --gradle-version 8.9` with Gradle installed.

## First-time phone setup
1. Install the APK and open it.
2. Enter the parent's mobile number in international format, e.g. +91...
3. Select installed apps to monitor and save settings.
4. Tap **Grant required permissions**. Enable Usage Access in Android Settings and grant SMS permission.
5. Tap **Start monitoring** and approve Android's VPN connection request.
6. Keep the app enabled; allow its background operation in the phone manufacturer's battery settings.
7. Test with a short controlled session and confirm the SMS and network block on the actual phone.

## Current known implementation scope
The service checks aggregate foreground usage every 15 seconds. At the next clock-hour boundary it reports the completed hour and removes the VPN. If a phone is powered off or the process is killed for a long period, exact hourly reporting cannot be guaranteed. A production version should persist state, recover after reboot, add IPv6 handling and test OEM-specific behaviour.
