# Code Grabber

Watches WhatsApp notifications on your device and, when a redeem code appears
(e.g. `6KED| N5VW| JKD5`), automatically copies just the code to your clipboard.

## How it works
- Uses Android's `NotificationListenerService` to read notifications from the
  WhatsApp app **already showing on your device** (this only works for
  notifications your own phone receives — it can't read anyone else's phone).
- Checks the notification text for:
  1. A line labeled `CODE:` — copies whatever follows it, or
  2. A pipe-separated pattern like `XXXX| XXXX| XXXX`
- Copies the match to the clipboard and shows a small "Code copied" confirmation.

## Setup
1. Open this folder in Android Studio (File → Open → select the `CodeGrabber` folder).
   Let Gradle sync (it will download the wrapper automatically).
2. Build → Make Project, then Run on your device/emulator.
3. On first launch, tap **"Open Notification Access Settings"**, find
   **Code Grabber** in the list, and turn it on. Android requires this to be
   granted manually — no app can request it silently.
4. Leave the app installed; the listener service keeps running in the
   background as long as notification access is granted.

## Auto-redirect countdown
After a code is copied, a full-screen black countdown appears (like a
low-battery shutdown screen) with a bar that depletes over ~6 seconds:
- **Open Now** — jumps straight to the redeem site and auto-fills the code
- **Close** — cancels the redirect
- If you do nothing, it auto-opens once the bar empties

To change the countdown length, edit `COUNTDOWN_MS` in
`RedirectCountdownActivity.kt`.

### Auto-fill on the redeem site
`RedeemWebViewActivity.kt` opens `https://reward.ff.garena.com/en` in a
WebView and tries to auto-fill the code into the input field using a few
common CSS selectors (`placeholder`/`name`/`id`/`class` containing "code",
falling back to any text input). Because the site is a JS-rendered SPA, if
the auto-fill doesn't land on the right field:
1. Open the site once in Chrome on your phone.
2. Long-press the code input → Inspect (or use desktop Chrome DevTools via
   USB debugging) to find its exact `id`/`name`/`class`.
3. Add that exact selector to the `selectors` array in
   `RedeemWebViewActivity.kt` so it's checked first.

Some devices also require the app to be granted **"Display over other
apps"** for the full-screen countdown to reliably appear from a background
service — you'll be prompted for this if needed, or you can enable it
manually in Settings → Apps → Code Grabber → Display over other apps.

## Adjusting the code pattern
Edit `CodeListenerService.kt`:
- `PIPE_CODE_REGEX` — matches groups like `6KED| N5VW| JKD5`
- `LABELED_CODE_REGEX` — matches an explicit `CODE:` label

Tweak the character-class lengths (`{3,6}`) or add new regexes if your codes
look different.

## Note on WhatsApp Business
If the codes come through WhatsApp Business instead of regular WhatsApp,
also add `"com.whatsapp.w4b"` as an accepted package in
`CodeListenerService.kt`.
