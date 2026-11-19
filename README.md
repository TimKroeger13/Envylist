# Envylist

A playful, fully offline task and chores app for Android (made with GrapheneOS in mind),
built around a home-screen widget that can do everything the app can.

- **Tap to advance:** open → in progress → done (struck through). ↺ or long-press resets.
- **Close instead of delete:** tick the checkbox of a finished task. It glows for a moment,
  then moves to the archive. Tap again within ~2.5 s to undo.
- **Jealous tasks:** every finished task gives all other unfinished tasks of the list one envy point.
  At 5 / 10 / 15 points they complain louder. At 20 they object before you finish another one first.
  All thresholds can be changed in the settings.
- Multiple colourful lists, manual ordering (drag & drop in the app, ⬆ in the widget's task menu).
- No `INTERNET` permission, no Google services, data stays in a local Room database.

## Build

Requirements: JDK 17+ and the Android SDK (`local.properties` → `sdk.dir`).

```sh
./gradlew :app:testDebugUnitTest   # envy engine unit tests
./gradlew :app:assembleDebug       # app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Structure

| Package | Content |
|---|---|
| `domain` | Pure rules: `EnvyEngine` (envy, levels, objections, ordering), palette, voice lines |
| `data` | Room entities/DAO, `TaskRepository` (every write refreshes the widgets), settings |
| `widget` | Glance widget, action callbacks, small dialog activities (quick add, task actions, objection, list picker) |
| `ui` | Compose app: board with list tabs, archive, settings |
| `work` | `ArchiveWorker` closes ticked tasks after the grace period |

Envy is derived, not stored: a task's envy is the number of completions of *other* tasks in its list
since its baseline. Undoing an accidental completion (within 10 minutes) removes that completion again.
