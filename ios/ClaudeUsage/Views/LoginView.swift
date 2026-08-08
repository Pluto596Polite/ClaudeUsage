import SwiftUI
import WebKit

/// Full-screen sign-in that loads Claude's real login page in a `WKWebView`, then captures the
/// session cookie once it appears — the iOS counterpart of the Android `LoginScreen`.
///
/// The app never sees your password: it only reads the `sessionKey` cookie the website itself sets,
/// which is exactly what the `/usage` API call needs.
struct LoginView: View {
    let onLoginSuccess: (_ cookies: String) -> Void

    @State private var progress: Double = 0
    @State private var isLoading = true

    var body: some View {
        VStack(spacing: 0) {
            ZStack {
                ClaudeTheme.surface
                Text("Sign in to Claude")
                    .font(.headline)
                    .foregroundColor(ClaudeTheme.textPrimary)
                    .padding(.vertical, 14)
            }
            .frame(maxWidth: .infinity)
            .fixedSize(horizontal: false, vertical: true)

            if isLoading {
                ProgressView(value: progress)
                    .tint(ClaudeTheme.orange)
            }

            ClaudeLoginWebView(
                progress: $progress,
                isLoading: $isLoading,
                onLoginSuccess: onLoginSuccess
            )
        }
        .background(ClaudeTheme.bg)
    }
}

private struct ClaudeLoginWebView: UIViewRepresentable {
    @Binding var progress: Double
    @Binding var isLoading: Bool
    let onLoginSuccess: (_ cookies: String) -> Void

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    func makeUIView(context: Context) -> WKWebView {
        let config = WKWebViewConfiguration()
        config.websiteDataStore = .default()
        config.defaultWebpagePreferences.allowsContentJavaScript = true

        let webView = WKWebView(frame: .zero, configuration: config)
        webView.navigationDelegate = context.coordinator
        webView.customUserAgent =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"
        context.coordinator.observe(webView)

        webView.load(URLRequest(url: URL(string: "https://claude.ai/login")!))
        context.coordinator.startPolling(webView)
        return webView
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {}

    static func dismantleUIView(_ uiView: WKWebView, coordinator: Coordinator) {
        coordinator.stop()
    }

    final class Coordinator: NSObject, WKNavigationDelegate {
        private let parent: ClaudeLoginWebView
        private var triggered = false
        private var timer: Timer?
        private var progressObservation: NSKeyValueObservation?

        init(_ parent: ClaudeLoginWebView) { self.parent = parent }

        func observe(_ webView: WKWebView) {
            progressObservation = webView.observe(\.estimatedProgress, options: .new) { [weak self] webView, _ in
                DispatchQueue.main.async {
                    self?.parent.progress = webView.estimatedProgress
                    self?.parent.isLoading = webView.estimatedProgress < 1.0
                }
            }
        }

        // claude.ai is a single-page app: after login it routes client-side without a full page
        // load, so URL-based detection is unreliable. Instead we poll for the auth cookie
        // (sessionKey) — exactly what the /usage API needs. We're done once it appears while on
        // claude.ai (and not on a third-party OAuth page).
        func startPolling(_ webView: WKWebView) {
            timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self, weak webView] _ in
                guard let self, let webView else { return }
                self.maybeFinish(webView)
            }
        }

        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            parent.isLoading = false
            maybeFinish(webView)
        }

        private func maybeFinish(_ webView: WKWebView) {
            guard !triggered else { return }
            let currentURL = (webView.url?.absoluteString ?? "").lowercased()
            if currentURL.contains("accounts.google") || currentURL.contains("appleid.apple") { return }

            webView.configuration.websiteDataStore.httpCookieStore.getAllCookies { [weak self] cookies in
                guard let self, !self.triggered else { return }
                let claudeCookies = cookies.filter { $0.domain.contains("claude.ai") }
                guard claudeCookies.contains(where: { $0.name == "sessionKey" }) else { return }

                let cookieString = claudeCookies
                    .map { "\($0.name)=\($0.value)" }
                    .joined(separator: "; ")

                self.triggered = true
                self.stop()
                DispatchQueue.main.async {
                    self.parent.onLoginSuccess(cookieString)
                }
            }
        }

        func stop() {
            timer?.invalidate()
            timer = nil
            progressObservation?.invalidate()
            progressObservation = nil
        }
    }
}
