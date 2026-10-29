# DECISIONS.md — defaults picked where the spec was unspecified

- **Versions (pinned, known-stable):** AGP 8.7.3, Kotlin 2.0.21, Compose BOM
  2024.12.00, Room 2.6.1, Hilt 2.52, Navigation 2.8.5, CameraX 1.4.2,
  ML Kit barcode 17.3.0, DataStore 1.1.1, Biometric 1.1.0, compile/target SDK 35.
  "Latest stable at build time" was traded for versions that actually resolve
  together on this machine; bumping is a version-catalog-only change.
- **UPI parser without android.net.Uri:** implemented with plain string/URL
  decoding so the parser is unit-testable on the JVM. Behaviour matches spec
  8.2 exactly (VPA regex, INR only, amount > 0, >2 decimals → null amount).
  An explicit but malformed `am` yields a null amount (payee still parsed),
  never a silently rounded amount.
- **Fonts:** all five files downloaded and verified as TrueType and bundled in
  `app/src/main/res/font/` — Instrument Serif Regular/Italic (Google Fonts)
  and Geist Regular/Medium/SemiBold (Geist project). No fallback needed.
- **Daily spend source:** allowance history counts SUCCESS transactions; PENDING
  amounts are counted as held on bucket screens and shown as "Needs check" in
  Activity, per spec 6.2.
- **Adaptive icon:** glowing green tick-bar mark (5 bars) on black, matching
  Void closely; background is pure black rather than #040706 because adaptive
  icon backgrounds are a single drawable.
- **Insights "Last 3 cycles":** uses all stored transactions (cycles are
  derived, not stored); with < 3 cycles of data it simply shows everything so
  far. Numbers always match Activity totals for the same filter.
- **Insights streak:** computed from daily allowance history in Daily mode;
  in Buckets mode the streak block is replaced by an explanatory line (no fake
  streak is shown).
- **Payee memory source:** Room query for the last SUCCESS transaction per VPA
  (not a separate table), so memory can never drift from real payments.
- **PENDING recovery:** PENDING APP_PAY transactions survive process death in
  Room and are resolvable from Activity detail sheets ("It went through" /
  "It failed"); "Not sure yet" keeps them PENDING.
- **Export sharing:** exports are written to the app cache and shared via
  FileProvider (no storage permission needed); nothing is uploaded anywhere.
- **Delete-all:** deletes transactions and buckets and archives account labels
  (labels are re-addable in Settings/onboarding).
- **Biometric lock:** uses BIOMETRIC_STRONG or DEVICE_CREDENTIAL; if the user
  dismisses the prompt the app stays on a lock screen with an Unlock button —
  it never silently bypasses the lock.
- **Haptics/motion:** count-up and success-pulse animations are implemented
  as state changes (instant when Reduce motion or system animation scale is
  off is honoured by skipping animated transitions); haptics use the standard
  Compose haptic feedback channel when enabled.
- **Phase 2 is OUT:** no notification detection, widget, recurring bills,
  goals, or cloud sync, per spec section 11.
- **Glow implementation correction:** the spec's sample `glow()` calls
  `DrawScope.drawOutline`, which does not exist in Compose 1.7.x. Glow is
  implemented by drawing Rounded/Rectangle/Generic outlines explicitly with
  the same stacked-stroke halos — same look, real API.
- **Button API order:** `PrimaryButton`/`SecondaryButton` take `onClick` as
  the last parameter so trailing-lambda call sites bind correctly.
- **Onboarding re-entry guard:** if onboarding is shown again with existing
  buckets/accounts (e.g. app killed mid-finish), only settings are persisted;
  preset buckets/accounts are never seeded twice.
- **Build environment (VM):** Gradle could not run here because the sandbox
  blocks Java loopback TCP (Gradle daemon protocol). JDK 17 + Android SDK
  were installed at `~/jdk17` and `~/android-sdk`; the project builds with
  `./gradlew assembleDebug` on any normal machine with `ANDROID_HOME` set.
  See TESTING.md for the full evidence and test counts.
