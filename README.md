# Mail Sound Alert (Android source project)

A simple Gmail IMAP monitor. It polls INBOX about once per minute and plays the device alarm ringtone when it detects a newer message. It does not rely on Gmail's own notification being shown.

## Requirements
- Android Studio with Android SDK 35 and JDK 17 or 21.
- A Gmail account with IMAP access enabled.
- A Google App Password (requires Google 2-Step Verification). Do not enter your normal Google password.

## Build APK
1. Open this `MailSound` folder in Android Studio.
2. Allow Gradle sync and install any prompted SDK components.
3. Select **Build > Build APK(s)**.
4. Debug APK is normally at `app/build/outputs/apk/debug/app-debug.apk`.

Or from a machine with Android SDK and Gradle available, run `gradle assembleDebug` from this folder.

## Use
1. Install the APK and open **Mail Sound Alert**.
2. Enter Gmail address and Google App Password, then tap **Start email sound monitoring**.
3. The first successful check sets a baseline, so old messages won't trigger an alarm. Later new messages should trigger a sound.
4. Disable battery optimization for this app if your phone keeps stopping it.

## Limitations and security
- This initial build is Gmail-only (IMAPS on port 993), not Outlook or arbitrary mail providers.
- The app checks about every 60 seconds; it is not instantaneous. Network availability, Android battery management, and manufacturer restrictions can delay checks.
- Android requires an ongoing foreground-service notification while monitoring. That is the app's service notification, not a Gmail new-mail notification.
- Force-stopping the app, rebooting the phone, disabling network access, or powering off prevents checks.
- Credentials are stored in the app's private SharedPreferences in this prototype. Use a dedicated Google App Password and revoke it if you stop using the app. Do not distribute this prototype with your credentials in it.
- Android 14+ special-use foreground-service behavior may vary by device and distribution channel.
