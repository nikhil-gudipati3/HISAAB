# TESTING.md — Hisaab

## Automated tests (this build)

Unit tests (`app/src/test/`): `DomainTest.kt` — table-style cases for:
- Indian formatter/parser (grouping 12,34,567.89; whole-rupee vs paise display;
  >2-decimal rejection)
- Cycle math: payday 31 in a 30-day month, payday 31 in February,
  payday 31 after February ends, payday 1 normal
- Daily allowance: ₹1,00,000 / 30 days sums to exactly 10,000,000 paise
  (first 10 days 3,33,334 paise, rest 3,33,333); SWEEP and ROLL over a 10-day
  sample with overspend; bucket math incl. rollover + held PENDING;
  pace projection; streak
- UPI parser: valid, missing amount, 3 decimals rejected, non-INR rejected,
  encoded characters, garbage, non-UPI QR, build-URI round-trip, result
  interpretation (SUCCESS / FAILURE / SUBMITTED→ask / empty→ask)
- Category mapper (MCC 5411/5812/5814/5541/4111, unknown→Other) and payee
  memory (case-insensitive VPA, last value wins)

Room DAO tests (`app/src/androidTest/DaoInstrumentedTest.kt`): in-memory DB —
bucket + transaction CRUD, date-range query, PENDING list, payee memory lookup.
Requires a device/emulator; not run in this VM (no emulator here).

Compose UI tests (`app/src/androidTest/PaySheetTest.kt`): Pay confirm-sheet
surface — over-budget text formatting and strict-blocked vs "Pay anyway"
decision logic. Full sheet interaction tests need a device/emulator.

Run:
```bash
./gradlew testDebugUnitTest --no-daemon
./gradlew connectedDebugAndroidTest --no-daemon   # device/emulator only
```

## Results (build VM, 2026-10-28)

- **Unit tests: 21/21 PASS.** Run outside Gradle with kotlinc 2.0.21 +
  JUnit 4.13.2 directly on the exact project sources
  (`core/money`, `core/time`, `domain/*`, `DomainTest.kt`): MoneyTest (3),
  CycleTest (4), DailyTest (4), UpiTest (8), CategoryTest (2). One test
  expectation was fixed during the run (streak counts backwards from the last
  day, so an overspend on the last day is a 0 streak — the code was right,
  the test was wrong; no production code was lowered to pass).
- **Full Android source compile: 0 errors.** All main Kotlin sources
  (Compose UI, CameraX/ML Kit scanner, Room, DataStore, Hilt, Navigation,
  Biometric) compiled with kotlinc 2.0.21 + the Compose compiler plugin
  against the real published library jars for the pinned versions, with only
  the generated `R` class stubbed (font resource IDs). This compile check
  caught and fixed 4 real bugs that a Gradle build would also have failed
  on: `DrawScope.drawOutline` does not exist in Compose 1.7.x (glow now draws
  Rounded/Rectangle/Generic outlines explicitly), button `onClick` parameters
  were not last so trailing lambdas bound to the wrong parameter, a
  ToggleRow lambda shadowed its Boolean parameter, and a SecondaryButton
  call in the lock screen passed its lambda as `modifier`.
- **Gradle build / APK: NOT produced in this VM — blocked by the sandbox,
  not by the code.** Evidence: JDK 17 (Temurin 17.0.21) and a full Android
  SDK (platform android-35, build-tools 35.0.0, platform-tools/adb) were
  installed under `~/jdk17` and `~/android-sdk`. Gradle 8.9 starts its daemon
  fine (daemon log shows it listening on 127.0.0.1), but the client cannot
  read from it: `DaemonConnectionException: Unexpected type tag 109`. A
  direct Java-to-Java loopback socket test in the same VM returned the
  sandbox message "Other TCP connections is turned off for this assistant"
  (type tag 109 = 'm' = "muse: …"), while a Python loopback test worked —
  i.e. the sandbox intercepts Java process TCP, which Gradle's daemon
  protocol requires. No Gradle task (not even `tasks`) can run here, so
  `./gradlew testDebugUnitTest` and `./gradlew assembleDebug` must be run on
  a normal machine or on Nikhil's phone-connected computer, where the
  sources above are ready to build. **Do not treat the APK as delivered
  until that build runs.**
- **Instrumented tests not run:** DAO + Compose tests exist under
  `app/src/androidTest/` but need a device/emulator; none was attached here.

## Manual checklist — FOR USER on a real Android phone (do not skip)

Nikhil will do these himself. Never enter a UPI PIN inside Hisaab — the PIN
goes only into the UPI app.

1. **Install / sideload**
   - Copy `app-debug.apk` to the phone, tap it, allow "Install unknown apps"
     for Files/Chrome, and open Hisaab. Cold start: no white flash (dark
     splash), onboarding appears on first run.
2. **Onboarding (5 steps)**
   - Welcome → income + payday → style (Buckets / Daily, live example) →
     accounts (or "Add later") → buckets 50/30/20 (or savings target in Daily).
   - Kill the app and relaunch: everything persists; Home shows, not onboarding.
3. **QR scan (or manual UPI ID)**
   - Tap Pay → allow Camera → point at a real merchant UPI QR. The confirm
     sheet must show the payee name + UPI ID + amount.
   - If camera is denied, the "Enter UPI ID" path must still work
     (type e.g. a known UPI ID, tap Continue).
   - A non-UPI QR must show "That QR isn't a UPI payment code." (no crash).
4. **UPI app chooser**
   - In the confirm sheet, pick an account and a bucket (each chip shows what
     is left; projection "₹3,100 → ₹2,980" updates). Tap "Pay ₹…".
   - A chooser must appear ("Pay with") — Hisaab must not force one app.
     Test on **two different UPI apps** (e.g. GPay and PhonePe).
   - The UPI app must open pre-filled with the same payee and amount.
5. **Return-to-Hisaab confirmation (Yes / No / Not sure yet)**
   - Complete the payment in the UPI app (PIN entered there only), return to
     Hisaab: if the result is unclear, the sheet "Did the payment go through?"
     must appear. Try all three answers across tests:
     Yes → transaction becomes SUCCESS and Home/Buckets/Activity update.
     No → transaction becomes FAILED.
     Not sure yet → transaction stays PENDING, marked "Needs check".
   - If the UPI app returns a clear SUCCESS/FAILURE, it is applied directly.
6. **PENDING / Needs-check recovery**
   - Leave a payment as "Not sure yet", kill Hisaab, relaunch: the PENDING
     item must still be in Activity with "Needs check". Open it and resolve
     it ("It went through" / "It failed") — numbers must update everywhere.
7. **Home / Buckets / Activity update after a confirmed payment**
   - After a Yes: Home hero/bucket remaining drops by the amount, the bucket
     card and tick meter update, Activity shows the payment, Insights totals
     match Activity for the same cycle.
   - Edit the transaction in Activity (change bucket/note) and delete one:
     Home, Buckets and Activity must all update immediately.
8. **Over-budget and strict mode**
   - Pay more than a bucket has left (strict off): the sheet shows
     "Over by ₹…" and the CTA becomes "Pay anyway".
   - Turn strict mode on in Settings and repeat: the CTA is disabled with the
     strict-mode explanation; a manual Activity entry is still allowed.
9. **Settings / export / mode switch**
   - Switch Buckets ↔ Daily in Settings: Home changes correctly.
   - Export CSV and JSON, import the JSON back, toggle biometric lock,
     haptics, reduce motion. Delete-all requires typing DELETE.
10. **Offline**
    - Airplane mode: everything except opening the UPI app works (scanning
      uses the bundled ML Kit model, no network needed).

## Known limitations (by design)

- Some UPI apps block or limit intent payments to personal VPAs, and result
  extras are inconsistent between apps — Hisaab therefore never assumes
  success and always lets you override.
- No emulator/device was attached in the build VM, so instrumented
  (DAO + Compose) tests and all steps above are real-device checks, not
  claims from this build.
