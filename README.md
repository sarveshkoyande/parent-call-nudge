# Parent Call Nudge (Android)

A commitment-device app: after office hours (default 7pm), the next time you unlock
your phone it just dials Parent 1, then Grandparent — no prompt, no countdown, no
skip button. Your only way out is the phone's native "end call" button.

## What it does
- Runs a small foreground service so it can notice when you unlock your phone.
- On each unlock, if it's after your set hour and you haven't finished today's
  calls, it immediately places the call (`ACTION_CALL`, needs CALL_PHONE).
- Order: Parent first. Once dialed, the next unlock dials Grandparent. Then quiet
  until tomorrow.
- Cancel/decline the outgoing call → it simply dials again on your *next* unlock.

## What it CANNOT do (by design / OS limits)
- It cannot be made truly impossible to stop. You can always force-stop it,
  use airplane mode, or revoke permissions. That's an Android safety guarantee,
  not a bug — and a good thing (driving, emergencies, meetings).
- iOS cannot do the auto-dial part at all; this is Android-only.

## How to build
1. Install **Android Studio** (free).
2. `File → Open` this `parent-call-nudge` folder. Let Gradle sync.
3. Plug in your phone with USB debugging on (or use an emulator).
4. Press **Run** (green ▶).
5. In the app: enter Parent's & Grandparent's numbers, pick the "after" hour,
   tap **Arm it**. On first arm it will ask you to grant:
   - **Phone** (place calls)
   - **Display over other apps** (lets it dial from the background after unlock) —
     this is required; without it the auto-dial may silently not fire on Android 10+.
   - Then set the app's **Battery** to *Unrestricted* so the OS doesn't kill it.

## Files worth reading
- `ForegroundService.kt` — stays alive, listens for unlocks.
- `UnlockReceiver.kt` — the "phone was unlocked" trigger.
- `NudgeActivity.kt` — the full-screen countdown + auto-dial + friction skip.
- `Prefs.kt` — all saved state (numbers, who's next, done-today).
