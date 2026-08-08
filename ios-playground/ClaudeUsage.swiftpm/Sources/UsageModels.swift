import Foundation

/// A Claude organization as returned by `/api/organizations`.
struct Organization: Codable {
    var uuid: String
    var id: String
    var name: String
    var planTier: String?
    var billingType: String?

    /// Prefer the UUID, fall back to the numeric/string id.
    var effectiveId: String { uuid.isEmpty ? id : uuid }
}

/// One rate-limit window from claude.ai's `/api/organizations/{uuid}/usage` response.
///
/// claude.ai expresses Pro/Max usage as a percent-consumed per window (a rolling "session"
/// cap plus longer "weekly" caps) — there is no raw message count.
struct UsageLimit: Codable, Identifiable, Equatable {
    var kind: String        // "session", "weekly_all", "weekly_opus", ...
    var group: String       // "session" | "weekly"
    var percent: Int        // 0..100 of the cap consumed
    var severity: String    // "normal" | "warning" | ...
    var resetsAt: String?   // ISO-8601, when this window resets
    var isActive: Bool

    var id: String { kind }

    /// Consumed fraction, clamped to 0...1 for progress bars.
    var fraction: Double { min(max(Double(percent) / 100.0, 0), 1) }
}

/// The full usage snapshot shown in the UI and widget. The raw `/usage` JSON is the source of
/// truth; limit windows are re-parsed from it on read so the cache never drifts from the response.
struct UsageData: Codable, Equatable {
    var limits: [UsageLimit]
    var planName: String?
    var orgName: String?
    var lastFetchedEpoch: Double   // seconds since 1970
    var rawJson: String?

    init(limits: [UsageLimit] = [],
         planName: String? = nil,
         orgName: String? = nil,
         lastFetchedEpoch: Double = 0,
         rawJson: String? = nil) {
        self.limits = limits
        self.planName = planName
        self.orgName = orgName
        self.lastFetchedEpoch = lastFetchedEpoch
        self.rawJson = rawJson
    }

    static let empty = UsageData()

    var sessionLimit: UsageLimit? { limits.first { $0.group == "session" } }
    var weeklyLimits: [UsageLimit] { limits.filter { $0.group == "weekly" } }

    /// The window worth featuring: the active one, else the session window, else the first.
    var primaryLimit: UsageLimit? {
        limits.first { $0.isActive } ?? sessionLimit ?? limits.first
    }

    var hasData: Bool { !limits.isEmpty || lastFetchedEpoch > 0 }

    var lastFetchedDate: Date? {
        lastFetchedEpoch > 0 ? Date(timeIntervalSince1970: lastFetchedEpoch) : nil
    }

    /// Parse the `/usage` JSON body (`{"limits":[...]}`) into limit windows.
    /// Returns an empty array on any failure or unexpected shape — matching the Android parser.
    static func parseLimits(_ json: String?) -> [UsageLimit] {
        guard let json, !json.isEmpty,
              let data = json.data(using: .utf8),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let arr = root["limits"] as? [[String: Any]] else {
            return []
        }
        return arr.map { o in
            UsageLimit(
                kind: o["kind"] as? String ?? "",
                group: o["group"] as? String ?? "",
                percent: (o["percent"] as? NSNumber)?.intValue ?? 0,
                severity: (o["severity"] as? String).flatMap { $0.isEmpty ? nil : $0 } ?? "normal",
                resetsAt: (o["resets_at"] as? String).flatMap { $0.isEmpty ? nil : $0 },
                isActive: o["is_active"] as? Bool ?? false
            )
        }
    }
}
