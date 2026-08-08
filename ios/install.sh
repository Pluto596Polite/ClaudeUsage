#!/usr/bin/env bash
#
# Claude Usage — iOS/iPadOS installer
#
# Generates the Xcode project and gets the app onto your iPhone/iPad. Works with a FREE Apple ID
# (no paid developer account needed to run it on your own device).
#
# Usage:
#   ./install.sh                       Generate project and open it in Xcode (recommended)
#   ./install.sh --build               Build and install to a connected device (headless)
#   ./install.sh --prefix com.yourname Use your own bundle-id prefix (avoids App ID collisions)
#   ./install.sh --team ABCDE12345     Set your Apple Developer Team ID for signing
#   ./install.sh --device <UDID>       Target a specific device for --build
#   ./install.sh --appgroup            Enable the shared-data widget (needs a PAID account or
#                                      AltStore/SideStore — a free Personal Team cannot use it)
#   ./install.sh --help
#
# The app itself is 100% free: no purchase, no subscription, no server. All data stays on device.
# By default the build uses NO App Group so it runs on a free Apple ID out of the box; the widget
# then shows "Open app to load". Add --appgroup for the live, self-updating widget.
set -euo pipefail

cd "$(dirname "$0")"

MODE="open"        # open | build
TEAM=""
DEVICE=""
PREFIX=""
APPGROUP=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --build)   MODE="build"; shift ;;
    --open)    MODE="open"; shift ;;
    --team)    TEAM="${2:?}"; shift 2 ;;
    --device)  DEVICE="${2:?}"; shift 2 ;;
    --prefix)  PREFIX="${2:?}"; shift 2 ;;
    --appgroup) APPGROUP=1; shift ;;
    -h|--help)
      grep '^#' "$0" | sed 's/^# \{0,1\}//' | sed '1d'
      exit 0 ;;
    *) echo "Unknown option: $1" >&2; exit 1 ;;
  esac
done

info()  { printf '\033[0;34m›\033[0m %s\n' "$*"; }
ok()    { printf '\033[0;32m✓\033[0m %s\n' "$*"; }
warn()  { printf '\033[0;33m!\033[0m %s\n' "$*"; }
die()   { printf '\033[0;31m✗ %s\033[0m\n' "$*" >&2; exit 1; }

# --- Preconditions ----------------------------------------------------------
[[ "$(uname)" == "Darwin" ]] || die "Building iOS apps requires macOS with Xcode. See README.md for AltStore/SideStore alternatives."
command -v xcodebuild >/dev/null 2>&1 || die "Xcode not found. Install Xcode from the App Store, then run: xcode-select --install"

# --- Optional: rewrite the bundle-id prefix ---------------------------------
# App IDs are globally unique across all Apple developers, so if you plan to keep the app past a
# free ID's 7-day window (or use a paid account), pick your own prefix.
if [[ -n "$PREFIX" ]]; then
  info "Rewriting bundle prefix com.adriaan → $PREFIX"
  ESCAPED_OLD='com\.adriaan'
  ESCAPED_NEW="$(printf '%s' "$PREFIX" | sed 's/[&/\]/\\&/g')"
  # macOS/BSD sed in-place
  while IFS= read -r -d '' f; do
    sed -i '' "s/${ESCAPED_OLD}/${ESCAPED_NEW}/g" "$f"
  done < <(find . -type f \( -name '*.swift' -o -name '*.plist' -o -name '*.entitlements' -o -name '*.yml' \) -print0)
  ok "Prefix updated. (These edits touch your working copy — 'git checkout ios' to revert.)"
fi

# Effective app group id (tracks the prefix chosen above; default com.adriaan).
BASE_PREFIX="${PREFIX:-com.adriaan}"
GROUP="group.${BASE_PREFIX}.claudeusage"

# --- Optional: enable App Group (shared-data widget) ------------------------
write_entitlements() {
  local file="$1" body="$2"
  cat > "$file" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
${body}</dict>
</plist>
EOF
}

if [[ "$APPGROUP" -eq 1 ]]; then
  info "Enabling App Group $GROUP (widget ↔ app data sharing)…"
  warn "This requires a PAID Apple Developer account, or installing via AltStore/SideStore."
  BODY="    <key>com.apple.security.application-groups</key>
    <array>
        <string>${GROUP}</string>
    </array>
"
  write_entitlements ClaudeUsage/App/ClaudeUsage.entitlements "$BODY"
  write_entitlements ClaudeUsageWidget/ClaudeUsageWidget.entitlements "$BODY"
  ok "App Group written to both targets' entitlements."
fi

# --- Ensure XcodeGen --------------------------------------------------------
if ! command -v xcodegen >/dev/null 2>&1; then
  warn "XcodeGen not found."
  if command -v brew >/dev/null 2>&1; then
    info "Installing XcodeGen via Homebrew…"
    brew install xcodegen
  else
    die "XcodeGen is required to generate the project. Install Homebrew (https://brew.sh) then 'brew install xcodegen', or run: mint install yonaskolb/XcodeGen"
  fi
fi

# --- Generate the Xcode project ---------------------------------------------
info "Generating ClaudeUsage.xcodeproj…"
xcodegen generate
ok "Project generated."

# --- Open or build ----------------------------------------------------------
if [[ "$MODE" == "open" ]]; then
  info "Opening in Xcode. Select your device, set the Team under Signing & Capabilities, then press ⌘R."
  open ClaudeUsage.xcodeproj
  cat <<'EOF'

Next steps in Xcode:
  1. Plug in your iPhone/iPad and select it as the run destination.
  2. Target 'ClaudeUsage' → Signing & Capabilities → choose your Team
     (a free "Personal Team" is fine to run on your own device).
  3. Do the same for the 'ClaudeUsageWidget' target (use the same Team).
  4. Press ⌘R to build, install, and launch.
  5. On the device: Settings → General → VPN & Device Management → trust your developer profile.
  6. Add the widget: long-press the Home Screen → + → search "Claude Usage".

Note: a free Apple ID re-signs every 7 days (re-run to refresh) and cannot use App Groups,
so the separate widget only shares data with a paid account or via AltStore/SideStore.
The app itself works fully either way. See README.md.
EOF
  exit 0
fi

# --- Headless build & install ----------------------------------------------
[[ -n "$TEAM" ]] || die "--build needs a signing team. Pass --team <TEAM_ID> (find it at https://developer.apple.com/account under Membership, or in Xcode → Settings → Accounts)."

DEST="generic/platform=iOS"
if [[ -n "$DEVICE" ]]; then
  DEST="id=$DEVICE"
else
  # Try to auto-pick the first connected device.
  FOUND="$(xcrun xctrace list devices 2>/dev/null | awk '/\(([0-9]+\.)+[0-9]+\) \(/{print; exit}')" || true
  [[ -n "$FOUND" ]] && info "Detected device: $FOUND"
fi

info "Building & installing (team $TEAM)…"
xcodebuild \
  -project ClaudeUsage.xcodeproj \
  -scheme ClaudeUsage \
  -configuration Debug \
  -destination "$DEST" \
  -allowProvisioningUpdates \
  DEVELOPMENT_TEAM="$TEAM" \
  CODE_SIGN_STYLE=Automatic \
  clean build

ok "Build succeeded."
info "If it didn't auto-install, open the project in Xcode and press ⌘R, or use 'xcrun devicectl device install app'."
