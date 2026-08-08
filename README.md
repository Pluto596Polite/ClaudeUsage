# Claude Usage

Monitor your Claude.ai usage limits, percent consumed, and reset times — with an at-a-glance widget.
Available for **Android** and **iOS / iPadOS**.

| Platform | Location | Glanceable surface | Needs |
|---|---|---|---|
| Android | this directory (`app/`) | Home screen widget | Android Studio |
| iOS & iPadOS (full) | [`ios/`](ios/) | Home Screen + Lock Screen widgets | a Mac (or PC + sideload) |
| iPad-only (no computer) | [`ios-playground/`](ios-playground/) | in-app usage view (no widget) | just an iPad |

**Have a Mac?** `cd ios && ./install.sh` — see [`ios/README.md`](ios/README.md) for the full,
widget-capable app and how to install **entirely free** with a free Apple ID (and the App Group
caveat for the widget).

**Only have an iPad?** Use [`ios-playground/`](ios-playground/) — a Swift Playground app you build
and run **entirely on the iPad** with Apple's free Swift Playground app, no computer needed. It
gives you the usage view in-app (Swift Playground can't build the separate widget).

iOS/iPadOS have no menu bar, so the equivalent glance is a Home Screen or Lock Screen widget
(full build), or opening the app (iPad-only build).

---

## Android

Android app to monitor your Claude.ai usage limits, messages remaining, and reset times — with a home screen widget for at-a-glance stats.

## Features

- **Sign in** via Claude's web login (no credentials stored by the app)
- **Usage dashboard** — messages used / limit, % consumed, time until reset
- **Home screen widget** — pin to launcher for instant glance
- **Auto-refresh** — background sync every 30 minutes via WorkManager
- **Offline cache** — last-known data shown when offline
- **Claude design** — matches the Claude desktop app's dark warm aesthetic

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
