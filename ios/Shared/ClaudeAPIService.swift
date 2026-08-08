import Foundation

/// Talks to claude.ai's internal, undocumented API using the session cookie captured at login.
///
/// This mirrors the Android `ClaudeApiService`: it adds the `Cookie` header to every request,
/// lists organizations, then tries a handful of usage endpoints and parses the first that returns
/// usable limit windows.
struct ClaudeAPIService {

    let cookieString: String

    private static let base = "https://claude.ai"

    private var session: URLSession {
        let config = URLSessionConfiguration.ephemeral
        config.timeoutIntervalForRequest = 15
        config.httpCookieStorage = nil          // we set the Cookie header ourselves, like Android
        config.httpShouldSetCookies = false
        return URLSession(configuration: config)
    }

    /// HTTP status + body. `status == -1` means a transport/exception failure. `body` is nil
    /// unless the response was 2xx (so callers never parse an error page).
    private struct HTTPResult { let status: Int; let body: String? }

    private func get(_ path: String) async -> HTTPResult {
        guard let url = URL(string: Self.base + path) else { return HTTPResult(status: -1, body: nil) }
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue(cookieString, forHTTPHeaderField: "Cookie")
        request.setValue("Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15",
                         forHTTPHeaderField: "User-Agent")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue(Self.base + "/", forHTTPHeaderField: "Referer")
        request.setValue(Self.base, forHTTPHeaderField: "Origin")

        do {
            let (data, response) = try await session.data(for: request)
            let http = response as? HTTPURLResponse
            let code = http?.statusCode ?? -1
            let body = String(data: data, encoding: .utf8)
            let ok = (200...299).contains(code)
            return HTTPResult(status: code, body: ok ? body : nil)
        } catch {
            return HTTPResult(status: -1, body: nil)
        }
    }

    // MARK: Organizations

    func getOrganizations() async -> [Organization] {
        guard let body = await get("/api/organizations").body,
              let data = body.data(using: .utf8),
              let arr = try? JSONSerialization.jsonObject(with: data) as? [[String: Any]] else {
            return []
        }
        return arr.map { o in
            Organization(
                uuid: o["uuid"] as? String ?? "",
                id: o["id"] as? String ?? "",
                name: o["name"] as? String ?? "",
                planTier: (o["plan_tier"] as? String).flatMap { $0.isEmpty ? nil : $0 },
                billingType: (o["billing_type"] as? String).flatMap { $0.isEmpty ? nil : $0 }
            )
        }
    }

    // MARK: Usage

    /// On success `limits` is non-empty and `raw` holds the JSON that produced it. On failure
    /// `limits` is empty and `raw` holds a diagnostic listing every endpoint tried with its status,
    /// so a moved/auth-failed endpoint is visible instead of silently showing zeros.
    struct UsageResult { let limits: [UsageLimit]; let raw: String? }

    func getUsage(orgId: String) async -> UsageResult {
        // claude.ai serves usage from /usage as percent-per-window limit objects.
        // Keep fallbacks in case the path moves.
        let endpoints = [
            "/api/organizations/\(orgId)/usage",
            "/api/organizations/\(orgId)/limits",
            "/api/account/usage",
            "/api/usage"
        ]

        var diagnostics = "No usage endpoint returned usable data.\n\nTried:\n"
        for endpoint in endpoints {
            let result = await get(endpoint)
            let statusLabel = result.status == -1 ? "network error" : "HTTP \(result.status)"
            diagnostics += "\u{2022} \(endpoint) \u{2192} \(statusLabel)\n"

            guard let raw = result.body else { continue }
            let limits = UsageData.parseLimits(raw)
            if !limits.isEmpty { return UsageResult(limits: limits, raw: raw) }
        }
        return UsageResult(limits: [], raw: diagnostics)
    }

    // MARK: One-shot fetch

    enum FetchError: LocalizedError {
        case notLoggedIn
        case noOrganizations
        case noUsage(String)

        var errorDescription: String? {
            switch self {
            case .notLoggedIn: return "Not signed in."
            case .noOrganizations: return "No organizations found — session may have expired."
            case .noUsage(let diagnostic): return diagnostic
            }
        }
    }

    /// Full fetch pipeline shared by the app, the widget, and the background task.
    /// Returns fresh `UsageData` or throws a descriptive error (never blanks cached data).
    func fetchUsageData() async throws -> UsageData {
        let orgs = await getOrganizations()
        guard let org = orgs.first else { throw FetchError.noOrganizations }

        let result = await getUsage(orgId: org.effectiveId)
        guard !result.limits.isEmpty else {
            throw FetchError.noUsage(result.raw ?? "Claude returned no usage data.")
        }

        return UsageData(
            limits: result.limits,
            planName: org.planTier ?? org.billingType,
            orgName: org.name,
            lastFetchedEpoch: Date().timeIntervalSince1970,
            rawJson: result.raw
        )
    }
}
