import Foundation
#if canImport(WidgetKit)
import WidgetKit
#endif

/// On-device session + usage cache shared between the app and the widget.
///
/// Sharing goes through an **App Group** container when one is available (the standard iOS
/// mechanism, and what lets the home-screen/lock-screen widget see the data the app captured).
/// When the app is signed with a bare *free* Apple ID — which cannot register App Groups — this
/// transparently falls back to the app's own `UserDefaults`. In that case the app remains fully
/// functional on its own; only the separate widget process can't read the cache. Nothing crashes,
/// and nothing costs money.
///
/// All data stays on the device: no server, no backend, no fees — same as the Android app.
final class SharedStore {

    static let shared = SharedStore()

    /// Must match the App Group declared in the app + widget entitlements and in `project.yml`.
    /// Override at build time with `-DAPP_GROUP_ID=...` isn't needed; edit here if you rename it.
    static let appGroupId = "group.com.adriaan.claudeusage"

    private let defaults: UserDefaults

    /// True when the App Group container was reachable — i.e. the widget can share this data.
    let usingAppGroup: Bool

    private init() {
        if let suite = UserDefaults(suiteName: Self.appGroupId) {
            defaults = suite
            usingAppGroup = true
        } else {
            defaults = .standard
            usingAppGroup = false
        }
    }

    private enum Key {
        static let cookies = "cookies"
        static let loggedIn = "logged_in"
        static let planName = "plan_name"
        static let orgName = "org_name"
        static let lastFetched = "last_fetched"
        static let rawJson = "raw_json"
    }

    // MARK: Session

    var isLoggedIn: Bool { defaults.bool(forKey: Key.loggedIn) }

    var cookies: String? { defaults.string(forKey: Key.cookies) }

    func saveSession(cookies: String) {
        defaults.set(cookies, forKey: Key.cookies)
        defaults.set(true, forKey: Key.loggedIn)
    }

    func clearSession() {
        for key in [Key.cookies, Key.loggedIn, Key.planName, Key.orgName, Key.lastFetched, Key.rawJson] {
            defaults.removeObject(forKey: key)
        }
        reloadWidgets()
    }

    // MARK: Usage cache

    /// The raw `/usage` JSON is the source of truth; limit windows are re-parsed from it on read.
    var usageData: UsageData {
        let raw = defaults.string(forKey: Key.rawJson)
        return UsageData(
            limits: UsageData.parseLimits(raw),
            planName: defaults.string(forKey: Key.planName),
            orgName: defaults.string(forKey: Key.orgName),
            lastFetchedEpoch: defaults.double(forKey: Key.lastFetched),
            rawJson: raw
        )
    }

    /// - Parameter reloadWidgets: pass `false` when saving from *inside* the widget's own timeline
    ///   provider, otherwise the reload would immediately re-request the timeline and loop.
    func saveUsageData(_ data: UsageData, reloadWidgets: Bool = true) {
        if let plan = data.planName { defaults.set(plan, forKey: Key.planName) }
        if let org = data.orgName { defaults.set(org, forKey: Key.orgName) }
        defaults.set(Date().timeIntervalSince1970, forKey: Key.lastFetched)
        if let raw = data.rawJson { defaults.set(raw, forKey: Key.rawJson) }
        if reloadWidgets { self.reloadWidgets() }
    }

    // MARK: Networking convenience (used by app, widget, and background refresh)

    /// Fetches fresh usage using the stored cookie and persists it. Never overwrites the cache
    /// with an empty result — on failure it throws and the last-known data is preserved.
    @discardableResult
    func refreshUsage(reloadWidgets: Bool = true) async throws -> UsageData {
        guard let cookies, isLoggedIn else { throw ClaudeAPIService.FetchError.notLoggedIn }
        let data = try await ClaudeAPIService(cookieString: cookies).fetchUsageData()
        saveUsageData(data, reloadWidgets: reloadWidgets)
        return data
    }

    // MARK: Widget refresh

    func reloadWidgets() {
        #if canImport(WidgetKit)
        WidgetCenter.shared.reloadAllTimelines()
        #endif
    }
}
