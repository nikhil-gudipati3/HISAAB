# Hisaab — Android app

A budgeting layer for UPI spending. Hisaab never holds, moves, or has access to
your money. Payments are completed in the UPI app you choose.

> "Hisaab doesn't hold or move your money. Payments are completed in the UPI app you choose."

## What it does

- **Buckets mode** — split monthly income into buckets (Needs, Wants, Savings…),
  every payment is assigned to a bucket, with rollover and held PENDING amounts.
- **Daily allowance mode** — one safe amount per day; unspent money is swept
  into savings (default) or rolled evenly over the remaining days.
- **Pay flow** — scan a UPI QR (CameraX + ML Kit, bundled model, offline) or type
  a UPI ID, pick account + bucket, then hand off to a UPI app via the system
  chooser. Hisaab never assumes success: unclear returns ask you, and PENDING
  payments show as "Needs check" in Activity until resolved.
- **Activity** — search, filters, manual add, edit, delete, resolve PENDING.
- **Insights** — spend by category/bucket, day-by-day bars, weekday totals,
  top payees, pace projection, streak (Daily mode).
- **Settings** — mode switch, income/payday/savings/leftover rule, strict mode,
  biometric lock, haptics, reduce motion, hide in recents, CSV/JSON export,
  JSON import, delete-all with typed DELETE confirmation.

## Build

Requirements: JDK 17, Android SDK (platform android-35, build-tools 35.0.0),
`ANDROID_HOME` pointing at the SDK.

```bash
export JAVA_HOME=<path-to-jdk17>
export ANDROID_HOME=~/android-sdk
cd hisaab
./gradlew testDebugUnitTest --no-daemon
./gradlew assembleDebug --no-daemon
```

The debug APK is at `app/build/outputs/apk/debug/app-debug.apk`.

## Install on a phone

1. On the phone: Settings → About → tap Build number 7 times, then
   Developer options → enable USB debugging (only needed for `adb install`).
   For sideloading, no developer options are needed.
2. Copy the APK to the phone and tap it, allowing "Install unknown apps" for
   the app you open it from (e.g. Files/Chrome), or:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
3. Open Hisaab, complete the 5-step onboarding, and follow the real-device
   checklist in TESTING.md. Never enter a UPI PIN inside Hisaab — the PIN is
   entered only in your UPI app.

## Project layout

Single-module Kotlin app, package `app.hisaab`, minSdk 26, portrait only,
Jetpack Compose single Activity + Navigation, Hilt, Room, DataStore,
CameraX + ML Kit barcode, androidx.biometric, custom Canvas charts,
Gradle Kotlin DSL + version catalog.

```
app/src/main/java/app/hisaab
  core/     design system, money, time
  data/     room, datastore, repositories
  domain/   budget math, daily allowance, cycle, UPI parser, categories
  feature/  onboarding, home, pay, buckets, activity, insights, settings
```

## Privacy

See PRIVACY.md. All data stays on the device. No backend, login, analytics,
ads, or SMS permissions. Camera frames are never stored or sent.
