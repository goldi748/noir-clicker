# Noir Clicker — Android project

A native Android app: a gold/black "Noir" UI (same look as the web preview)
sitting on top of two native pieces that do the real work —

- `ClickerAccessibilityService.kt` — the only thing Android lets tap or type
  into *other* apps (games, Instagram, WhatsApp...). Requires the user to
  turn it on once in Settings > Accessibility.
- `OverlayService.kt` — draws a small draggable gold dot (where to click) and
  a start/stop pill bubble on top of whatever app is open.

## Building it

This is a standard Gradle/Android Studio project — no Pydroid/Python involved,
since AccessibilityService only exists in Kotlin/Java.

**Fastest path:** zip is ready to upload as-is to any online Android build
service that accepts a Gradle project (e.g. Android Studio's own "open
project" on a borrowed PC, Gitpod/Codespaces with the Android SDK installed,
or similar). Entry point: open the folder, let it sync Gradle, press Run —
or `Build > Generate Signed/Unsigned APK` to get an installable `.apk`.

If the tool complains about a missing Gradle wrapper jar, run `gradle wrapper`
once inside the project folder, or let Android Studio generate it on first
sync (it will prompt you).

## First run, on the phone that installs the APK

1. Open the app → tap **Enable** next to *Accessibility service* → find
   "Noir Clicker" in the list → turn it on. (Android will warn this lets the
   app see screen content and perform actions — that's the accessibility
   permission working as intended, not a bug.)
2. Tap **Enable** next to *Draw over other apps* → allow it.
3. Pick **Single Clicker** (one point, you drag the dot to it) or
   **Multi Clicker** (fills a sentence into whatever text field is focused).
4. Tap **Start Floating Control**, switch to the game/app you want, drag the
   gold dot onto the right spot, tap the pill to begin, tap it again to stop.

## Honest limits, so there are no surprises

- **Speed ceiling:** the slider goes to 100 CPS, but Android's own gesture
  and accessibility-event pipeline realistically tops out around 30–60
  taps/second on most phones — the OS itself throttles it, not this code.
- **Typing:** `ACTION_SET_TEXT` fills whatever field is currently focused.
  It works on standard text fields (most chat/comment boxes). Some apps use
  custom input views that quietly ignore it — if typing does nothing in a
  specific app, that app's field is the reason.
- **It fills text, it doesn't press Send.** Sending is left to you on
  purpose — wiring up an auto-send loop turns this into a spam tool, and
  automated messaging is against Instagram's and WhatsApp's Terms of
  Service and can get an account restricted.
- The accessibility permission is powerful (screen content + actions) —
  that's exactly why Android gates it behind an explicit, scary-looking
  toggle. Nothing here works around that; it's the intended, documented API.

## Project layout
```
app/src/main/java/com/noir/autoclicker/
  MainActivity.kt               — hosts the WebView UI, permission bridge
  ClickerAccessibilityService.kt — dispatches taps, fills focused text fields
  OverlayService.kt             — floating target dot + start/stop bubble
app/src/main/assets/index.html  — the Noir UI (tabs, sliders, permission status)
app/src/main/res/               — manifest-referenced XML (icon, theme, strings)
```
