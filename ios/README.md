# Claude Usage — iOS & iPadOS

A native SwiftUI port of the Claude Usage Android app. Monitor your Claude.ai usage limits,
percent consumed, and reset times — with **Home Screen and Lock Screen widgets** for at-a-glance
stats on iPhone and iPad.

> **Why widgets and not a menu bar?** iOS and iPadOS have no menu bar. Their glanceable surfaces
> are **Home Screen widgets**, **Lock Screen widgets** (iOS 16+), and **StandBy** (which reuses the
> same widgets). This app ships all of them, so you get the same "quick glance without opening the
> app" experience the Android widget gives you. On a Mac, these same widgets also appear in
> Notification Center.

## Features

- **Sign in** via Claude's real web login (the app never sees your password — it only reads the
  `sessionKey` cookie the site sets, exactly what the usage API needs)
- **Usage dashboard** — a card per limit window (current session + weekly), percent consumed, ring,
  bar, and time until reset
- **Home Screen widget** — small & medium sizes
- **Lock Screen widgets** — circular, rectangular, and inline (the closest thing to an
  always-visible menu-bar readout)
- **Auto-refresh** — the widget refreshes on its own timeline and the app schedules background
  refresh (Background App Refresh)
- **Offline cache** — last-known data shown when offline; a failed refresh never blanks good data
- **Claude design** — the warm dark aesthetic, matching the Android app pixel-for-pixel
- **Entirely free** — no purchase, no subscription, no server, no backend. All data stays on device.

## No Mac or PC? (iPad-only)

If all you have is an iPad, use the **[`../ios-playground`](../ios-playground)** build instead — a
Swift Playground app you build and run **entirely on the iPad** with Apple's free Swift Playground
app, no computer or developer account needed. It gives you the full in-app usage view; only the
separate Home/Lock Screen widget is unavailable there (widgets need a Mac or a PC sideload). This
`ios/` project below is the full, widget-capable version.

## Requirements

- A **Mac** with **Xcode 15+** (to build/sign the app for your device)
- An **Apple ID** — a **free** one works to run the app on your own iPhone/iPad
- [XcodeGen](https://github.com/yonaskolb/XcodeGen) (the installer installs it for you via Homebrew)

## Install (the easy way)

```bash
cd ios
./install.sh
```

This generates `ClaudeUsage.xcodeproj` and opens it in Xcode. The default build uses **no App
Group**, so it compiles and runs on a free Apple ID with zero fuss (the widget then shows "Open app
to load"). For the live, self-updating widget, add `--appgroup` (see below). Then:

1. Plug in your iPhone/iPad and pick it as the run destination (top bar in Xcode).
2. Select the **ClaudeUsage** target → **Signing & Capabilities** → choose your **Team**
   (a free *Personal Team* is fine).
3. Do the same for the **ClaudeUsageWidget** target (same Team).
4. Press **⌘R** to build, install, and launch.
5. On the device: **Settings → General → VPN & Device Management → trust your developer profile.**
6. Add a widget: long-press the Home Screen → **+** → search **"Claude Usage"**. For a Lock Screen
   widget, edit the Lock Screen and add the accessory.

### Headless build

```bash
./install.sh --build --team ABCDE12345         # your Team ID
./install.sh --build --team ABCDE12345 --device <UDID>
```

### Use your own bundle ID

App IDs are globally unique across all Apple developers. If you'll keep the app past the free 7-day
window, or use a paid account, pick your own prefix:

```bash
./install.sh --prefix com.yourname
```

## The "entirely free" details (important)

Apple's rules, not this app's:

| | Free Apple ID (Personal Team) | Paid Developer Program ($99/yr) |
|---|---|---|
| Run the **app** on your device | ✅ Yes | ✅ Yes |
| App re-sign interval | Every **7 days** (just re-run `install.sh`) | **1 year** |
| **App Groups** (widget ↔ app data sharing) | ❌ Not allowed by Apple | ✅ Yes |
| Home/Lock Screen **widget shows live data** | Only via AltStore/SideStore¹ | ✅ Yes |

¹ **The app is always 100% free and fully functional** — sign in, dashboard, refresh, cache, and
background updates all work on a bare free Apple ID. The only thing a free *Personal Team* can't do
is the **App Group** that lets the *separate widget process* read the app's cache.

The default build ships **without** the App Group so it compiles cleanly on a free Personal Team;
the widget shows "Open app to load" until sharing is enabled. To turn the live widget on:

```bash
./install.sh --appgroup            # writes the App Group into both targets' entitlements
```

…which needs a **paid** account, or a free install via **[SideStore](https://sidestore.io)** /
**[AltStore](https://altstore.io)** (they can register the App Group on your behalf). Combine flags,
e.g. `./install.sh --appgroup --prefix com.yourname`.

## How it works

1. You log in through Claude's real login page in a `WKWebView`.
2. The app captures the `sessionKey` session cookie after login (never your credentials).
3. It calls Claude's internal API (`/api/organizations`, `/api/organizations/{id}/usage`) with that
   cookie — the same endpoints the Android app uses.
4. Usage is cached on device and shown in the native UI, the widget, and the Lock Screen accessories.

If Claude changes their API you may see an error with a diagnostic of every endpoint tried; the raw
response is viewable in-app via **"View raw API data."**

## Project layout

```
ios/
├── install.sh                  # one-command installer (generates project, opens Xcode)
├── project.yml                 # XcodeGen project definition (app + widget targets)
├── Makefile
├── Shared/                     # compiled into BOTH the app and the widget
│   ├── UsageModels.swift       # Organization / UsageLimit / UsageData (+ parser)
│   ├── ClaudeAPIService.swift  # cookie-authed calls to claude.ai's internal API
│   ├── SharedStore.swift       # App Group cache w/ graceful free-signing fallback
│   ├── UsageFormatting.swift   # reset labels / countdowns / plan names
│   └── Theme.swift             # Claude warm-dark palette
├── ClaudeUsage/                # the app
│   ├── App/                    # @main entry, root routing, Info.plist, entitlements
│   ├── ViewModels/             # UsageViewModel (state + refresh)
│   ├── Views/                  # LoginView (WKWebView), DashboardView, UsageProgressCard
│   └── Resources/Assets.xcassets
└── ClaudeUsageWidget/          # WidgetKit extension (home + lock screen)
```

## Tech stack

| Layer | Android | iOS |
|---|---|---|
| UI | Jetpack Compose | SwiftUI |
| Widget | Glance AppWidget | WidgetKit |
| Storage | DataStore | App Group `UserDefaults` (+ fallback) |
| Networking | OkHttp | `URLSession` |
| Background sync | WorkManager | `BGAppRefreshTask` |
| Login | WebView | `WKWebView` |
| Architecture | MVVM | MVVM (`ObservableObject`) |

## License

MIT
