import SwiftUI

/// Routes between the login WebView and the dashboard based on the stored session — the SwiftUI
/// equivalent of the Android `AppNavigation` NavHost.
struct RootView: View {
    @EnvironmentObject private var viewModel: UsageViewModel

    var body: some View {
        ZStack {
            ClaudeTheme.bg.ignoresSafeArea()

            if viewModel.isLoggedIn {
                DashboardView()
            } else {
                LoginView { cookies in
                    viewModel.onLoginSuccess(cookies: cookies)
                }
            }
        }
    }
}
