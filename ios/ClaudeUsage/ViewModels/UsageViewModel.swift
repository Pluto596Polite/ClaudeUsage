import SwiftUI

/// Drives the app UI. Mirrors the Android `MainViewModel` + `UsageRepository`: it loads the cached
/// snapshot immediately, refreshes from the network, and never blanks good data on a failed refresh.
@MainActor
final class UsageViewModel: ObservableObject {

    enum UIState: Equatable {
        case loading
        case success(UsageData)
        case error(message: String, cached: UsageData?)
    }

    @Published private(set) var state: UIState = .loading
    @Published private(set) var isRefreshing = false
    @Published var isLoggedIn: Bool

    private let store = SharedStore.shared

    init() {
        isLoggedIn = store.isLoggedIn
    }

    /// Show the last-known snapshot instantly (before the network call returns).
    func loadCachedData() {
        let cached = store.usageData
        if cached.hasData {
            state = .success(cached)
        }
    }

    func refresh() async {
        isRefreshing = true
        defer { isRefreshing = false }

        let cached: UsageData? = {
            if case let .success(data) = state { return data }
            let c = store.usageData
            return c.hasData ? c : nil
        }()

        do {
            let data = try await store.refreshUsage()
            state = .success(data)
        } catch {
            state = .error(message: error.localizedDescription, cached: cached)
        }
    }

    func onLoginSuccess(cookies: String) {
        store.saveSession(cookies: cookies)
        isLoggedIn = true
        state = .loading
        Task { await refresh() }
        ClaudeUsageApp.scheduleRefresh()
    }

    func logout() {
        store.clearSession()
        isLoggedIn = false
        state = .loading
    }
}
