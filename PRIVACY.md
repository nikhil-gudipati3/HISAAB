# Privacy — Hisaab

- **All data stays on this device.** Transactions, buckets, accounts (labels
  only) and settings are stored locally with Room and DataStore. There is no
  backend, no account, no login, and no sync.
- **No analytics, no ads, no crash reporters.** Nothing is sent anywhere.
- **Payments are not made by Hisaab.** When you tap Pay, Hisaab opens the UPI
  app you choose with the payee and amount pre-filled. Your UPI PIN is entered
  only in that UPI app. Hisaab never asks for, stores, or sees your UPI PIN,
  card numbers, CVV, OTPs, or net-banking credentials. Ever.
- **Camera:** used only to scan QR codes. Frames are processed on-device by
  ML Kit's bundled model and are never stored or sent.
- **Permissions:** CAMERA (QR scanning only), USE_BIOMETRIC (optional app
  lock). No SMS permissions, no QUERY_ALL_PACKAGES. `allowBackup` is false.
- **Exports:** CSV/JSON exports are files you choose to share yourself; the
  app uploads nothing.
- **Delete:** Settings → Delete all data (type DELETE) removes transactions
  and buckets from the device.

In-app, the same promise appears verbatim in onboarding and Settings → About:
"Hisaab doesn't hold or move your money. Payments are completed in the UPI app you choose."
