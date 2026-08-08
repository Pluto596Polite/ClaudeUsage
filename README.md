# Claude Usage

Android app to monitor your Claude.ai usage limits, messages remaining, and reset times — with a home screen widget for at-a-glance stats.

## Features

- **Sign in** via Claude's web login (no credentials stored by the app)
- **Usage dashboard** — messages used / limit, % consumed, time until reset
- **Quota alerts** — get a phone notification when usage crosses thresholds you set (50%, 75%, …), fully configurable
- **Auto-update** — the app checks GitHub Releases on launch and prompts you to install a newer build
- **Home screen widget** — pin to launcher for instant glance
- **Auto-refresh** — background sync every 15 minutes via WorkManager
- **Offline cache** — last-known data shown when offline
- **Claude design** — matches the Claude desktop app's dark warm aesthetic

## Quota alerts

Tap the bell icon on the dashboard to open **Alerts & Settings**:

- Toggle quota alerts on/off (grants the notification permission on Android 13+)
- Add or remove threshold percentages (defaults: 50%, 75%, 90%)

When any usage window (session or weekly) reaches a threshold, the app posts a
notification — **once per threshold per window**. The alert re-arms automatically
after the window resets and usage drops back below the threshold. Thresholds are
evaluated during the background sync (~every 15 minutes) and whenever you open the app.

## Updates

Releases are published on GitHub with an attached APK. On launch the app calls the
public `releases/latest` endpoint, compares the release tag to the installed
`versionName`, and shows an **Update available** dialog when a newer build exists.
Tapping **Update** downloads the APK so you can install it.

Releases are produced automatically by the `Release` GitHub Actions workflow when a
`v*` tag is pushed (e.g. `git tag v1.2.0 && git push origin v1.2.0`).

## Screenshots

> Open in Android Studio, build and run on a device/emulator to see the app.

## How it works

1. You log in through Claude's real login page (embedded WebView)
2. The app captures your session cookie after successful login
3. It calls Claude's internal API (`/api/organizations`, `/api/organizations/{id}/usage`) using that cookie
4. Usage data is stored locally and displayed in the native UI and widget

All data stays **on your device** — no server, no backend, no fees.

## Build

### Requirements

- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 35
- Java 11

### Steps

```bash
git clone https://github.com/Pluto596Polite/claude-usage-android.git
cd claude-usage-android
```

Open the project in **Android Studio** — it will download Gradle and all dependencies automatically.

Run on a device or emulator (API 26+).

### Adding the widget

1. Long-press your home screen
2. Tap **Widgets**
3. Find **Claude Usage** and drag it to your home screen

The widget updates every 30 minutes automatically.

## API notes

Claude's internal API endpoints are not publicly documented. The app tries these patterns in order:

- `GET /api/organizations/{id}/usage`
- `GET /api/organizations/{id}/subscription`
- `GET /api/organizations/{id}/limits`
- `GET /api/account/usage`

If Claude changes their API, you may see an empty dashboard but the app will show the raw API response (tap "View raw API data") to help diagnose.

## Tests

Pure logic is covered by JVM unit tests (no device/emulator needed):

- `VersionCompare` — release version parsing & "is newer" comparison (drives auto-update)
- `QuotaAlertEvaluator` — threshold crossing, fire-once, re-arm after reset, stale-flag pruning
- `Thresholds` — parsing/normalising the configurable alert thresholds
- `UsageData.parseLimits` — parsing the claude.ai `/usage` response

Run them locally:

```bash
./gradlew testDebugUnitTest
```

### CI gate

Every push and pull request runs the suite via the **Tests** GitHub Actions workflow
(`.github/workflows/tests.yml`); the **Release** workflow also runs it before building an APK.

To require it before merging, enable branch protection on `main`
(**Settings → Branches → Add rule**) and mark the **`unit-tests`** status check as required.
That blocks merging any PR whose tests fail.

## Tech stack

| Layer | Library |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose |
| Widget | Glance AppWidget |
| Storage | DataStore Preferences |
| Networking | OkHttp |
| Background sync | WorkManager |
| Architecture | MVVM + Repository |

## License

MIT
