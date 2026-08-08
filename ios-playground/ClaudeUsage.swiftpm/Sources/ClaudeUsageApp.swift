import SwiftUI

/// Swift Playground app entry point. Simpler than the full Xcode app: no background-task
/// registration (Swift Playground doesn't expose that Info.plist configuration) — the app refreshes
/// whenever it's opened and via pull-to-refresh. The usage view is identical to the full app.
@main
struct ClaudeUsageApp: App {
    @StateObject private var viewModel = UsageViewModel()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(viewModel)
                .preferredColorScheme(.dark)
        }
    }
}
