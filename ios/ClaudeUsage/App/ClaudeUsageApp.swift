import SwiftUI
import BackgroundTasks

@main
struct ClaudeUsageApp: App {

    /// Background App Refresh task id — must match the value in Info.plist's
    /// `BGTaskSchedulerPermittedIdentifiers`.
    static let refreshTaskId = "com.adriaan.claudeusage.refresh"

    @StateObject private var viewModel = UsageViewModel()
    @Environment(\.scenePhase) private var scenePhase

    init() {
        Self.registerBackgroundTask()
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(viewModel)
                .preferredColorScheme(.dark)
        }
        .onChange(of: scenePhase) { newPhase in
            if newPhase == .background {
                Self.scheduleRefresh()
            }
        }
    }

    // MARK: Background refresh (mirrors the Android WorkManager periodic sync)

    private static func registerBackgroundTask() {
        BGTaskScheduler.shared.register(forTaskWithIdentifier: refreshTaskId, using: nil) { task in
            guard let task = task as? BGAppRefreshTask else { return }
            scheduleRefresh() // chain the next one

            let work = Task {
                _ = try? await SharedStore.shared.refreshUsage()
                task.setTaskCompleted(success: true)
            }
            task.expirationHandler = { work.cancel() }
        }
    }

    /// Ask the system to run a refresh in ~30 minutes (the OS decides the actual timing).
    static func scheduleRefresh() {
        guard SharedStore.shared.isLoggedIn else { return }
        let request = BGAppRefreshTaskRequest(identifier: refreshTaskId)
        request.earliestBeginDate = Date(timeIntervalSinceNow: 30 * 60)
        try? BGTaskScheduler.shared.submit(request)
    }
}
