import Foundation

/// On-device session + usage cache for the Swift Playground build.
///
/// This is the single-target version of the full app's `SharedStore`: there is no widget extension
/// here (Swift Playground can't build one), so it stores everything in the app's own
/// `UserDefaults` — no App Group needed. All data stays on the device: no server, no fees.
final class SharedStore {

    static let shared = SharedStore()

    private let defaults = UserDefaults.standard
    private init() {}

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

    func saveUsageData(_ data: UsageData) {
        if let plan = data.planName { defaults.set(plan, forKey: Key.planName) }
        if let org = data.orgName { defaults.set(org, forKey: Key.orgName) }
        defaults.set(Date().timeIntervalSince1970, forKey: Key.lastFetched)
        if let raw = data.rawJson { defaults.set(raw, forKey: Key.rawJson) }
    }

    // MARK: Networking convenience

    /// Fetches fresh usage using the stored cookie and persists it. Never overwrites the cache with
    /// an empty result — on failure it throws and the last-known data is preserved.
    @discardableResult
    func refreshUsage() async throws -> UsageData {
        guard let cookies, isLoggedIn else { throw ClaudeAPIService.FetchError.notLoggedIn }
        let data = try await ClaudeAPIService(cookieString: cookies).fetchUsageData()
        saveUsageData(data)
        return data
    }
}
