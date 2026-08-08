# Claude Usage — iPad-only build (no Mac, no PC)

This folder is a **Swift Playground app** you can build and run **entirely on your iPad** using
Apple's free **Swift Playground** app — no Mac, no Windows PC, no paid developer account, no
sideloading tools. It is completely free.

## What you get (and the one thing you don't)

- ✅ The full app: sign in to Claude, and see your **usage quota** — current session + weekly
  windows, percent used, rings/bars, and reset countdowns.
- ✅ Offline cache and pull-to-refresh.
- ❌ **No Home Screen / Lock Screen widget.** Swift Playground can only build a single app target,
  and a widget is a separate "extension" that Apple only lets you build on a Mac (or install by
  sideloading a prebuilt file from a PC). To get the widget, use the full project in [`../ios`](../ios)
  with a Mac, or a PC + AltStore/SideStore. This build gives you the usage view inside the app
  instead — open the app to glance at your quota.

## Install it on your iPad (step by step)

1. **Get Swift Playground** — install the free **[Swift Playground](https://apps.apple.com/app/swift-playgrounds/id908519492)**
   app from the App Store (works on iPadOS 16+). Newer iPadOS may call it "Playgrounds".
2. **Download this project to your iPad:**
   - Open this repository on GitHub in **Safari** on your iPad.
   - Tap the green **Code** button → **Download ZIP** (or download the release ZIP).
   - Open the **Files** app, find the ZIP in *Downloads*, and **tap it to unzip**.
   - Open the unzipped folder and go into `ios-playground/`. You'll see **`ClaudeUsage.swiftpm`**
     with a Swift Playground icon.
3. **Open it:** tap **`ClaudeUsage.swiftpm`**. It opens in Swift Playground.
4. **Run it:** tap the **▶︎ Run** button (top-right). Swift Playground builds the app on-device and
   launches it. The first run may ask you to sign in with your Apple ID — a **free** Apple ID is
   fine; that's what signs the app to run on your iPad.
5. **Sign in to Claude** inside the app, and your usage appears.
6. **(Optional) Keep it on your Home Screen:** Swift Playground can install the app to your Home
   Screen — use the app's ••• / settings menu → **"Add to Home Screen"** (wording varies by iPadOS
   version). A free Apple ID re-signs about every 7 days; just re-run from Swift Playground to
   refresh it.

> If Downloading the ZIP is awkward, you can also open the `ClaudeUsage.swiftpm` folder from
> iCloud Drive or AirDrop it from another device — Swift Playground opens `.swiftpm` packages from
> anywhere in Files.

## How it works

Same as the other builds: it loads Claude's real login page in a web view, captures the `sessionKey`
session cookie (never your password), and calls claude.ai's internal usage API with it. Everything
stays on your device.

## Want the widget too?

You'll need a computer at least once:

- **A Mac:** open [`../ios`](../ios) and run `./install.sh`.
- **A Windows PC:** build the [`../ios`](../ios) project into an `.ipa` (e.g. via a cloud Mac / CI)
  and sideload it with AltStore / SideStore / Sideloadly, then add the widget from the Home Screen.

See [`../ios/README.md`](../ios/README.md) for the full widget-capable version.
