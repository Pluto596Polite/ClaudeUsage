import SwiftUI

/// The usage dashboard: one card per limit window, a details card, last-updated stamp, and the raw
/// API viewer. Mirrors the Android `DashboardScreen`, including pull-to-refresh and the
/// "keep cached data visible on a failed refresh" behaviour.
struct DashboardView: View {
    @EnvironmentObject private var viewModel: UsageViewModel
    @State private var showLogoutConfirm = false
    @State private var showRawJson = false

    var body: some View {
        VStack(spacing: 0) {
            topBar

            ScrollView {
                content
                    .padding(16)
            }
            .refreshable { await viewModel.refresh() }
        }
        .background(ClaudeTheme.bg.ignoresSafeArea())
        .task {
            viewModel.loadCachedData()
            await viewModel.refresh()
        }
        .confirmationDialog("Sign out", isPresented: $showLogoutConfirm, titleVisibility: .visible) {
            Button("Sign out", role: .destructive) { viewModel.logout() }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("You'll need to sign in again to view your usage.")
        }
        .sheet(isPresented: $showRawJson) {
            RawJsonSheet(json: currentData?.rawJson)
        }
    }

    private var currentData: UsageData? {
        switch viewModel.state {
        case .success(let d): return d
        case .error(_, let cached): return cached
        case .loading: return nil
        }
    }

    // MARK: Top bar

    private var topBar: some View {
        HStack {
            Button { showLogoutConfirm = true } label: {
                Image(systemName: "rectangle.portrait.and.arrow.right")
                    .foregroundColor(ClaudeTheme.textSecondary)
            }
            Spacer()
            Text("Usage")
                .font(.title2.weight(.semibold))
                .foregroundColor(ClaudeTheme.textPrimary)
            Spacer()
            Button { Task { await viewModel.refresh() } } label: {
                if viewModel.isRefreshing {
                    ProgressView().tint(ClaudeTheme.orange)
                } else {
                    Image(systemName: "arrow.clockwise").foregroundColor(ClaudeTheme.orange)
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(ClaudeTheme.surface)
    }

    // MARK: Content

    @ViewBuilder
    private var content: some View {
        switch viewModel.state {
        case .loading:
            ProgressView()
                .tint(ClaudeTheme.orange)
                .frame(maxWidth: .infinity, minHeight: 300)

        case .success(let data):
            usageContent(data)

        case .error(let message, let cached):
            if let cached {
                errorBanner(message)
                usageContent(cached)
            } else {
                VStack(spacing: 8) {
                    Text("Could not load usage")
                        .font(.headline)
                        .foregroundColor(ClaudeTheme.textPrimary)
                    Text(message)
                        .font(.subheadline)
                        .foregroundColor(ClaudeTheme.textSecondary)
                        .multilineTextAlignment(.center)
                }
                .frame(maxWidth: .infinity, minHeight: 300)
            }
        }
    }

    private func errorBanner(_ message: String) -> some View {
        Text("Could not refresh: \(message)")
            .font(.subheadline)
            .foregroundColor(ClaudeTheme.error)
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(ClaudeTheme.error.opacity(0.12))
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            .padding(.bottom, 12)
    }

    private func usageContent(_ data: UsageData) -> some View {
        VStack(spacing: 12) {
            let ordered = [data.sessionLimit].compactMap { $0 } + data.weeklyLimits
            ForEach(ordered) { limit in
                UsageProgressCard(
                    title: UsageFormatting.limitTitle(limit.kind),
                    percent: limit.percent,
                    resetLabel: UsageFormatting.resetLabel(limit.resetsAt)
                )
            }

            detailsCard(data)

            if let updated = UsageFormatting.lastUpdated(data.lastFetchedDate) {
                Text(updated)
                    .font(.subheadline)
                    .foregroundColor(ClaudeTheme.textSecondary)
            }

            if let raw = data.rawJson, !raw.isEmpty {
                Button { showRawJson = true } label: {
                    Label("View raw API data", systemImage: "info.circle")
                        .font(.subheadline)
                        .foregroundColor(ClaudeTheme.textSecondary)
                }
                .padding(.top, 4)
            }
        }
    }

    private func detailsCard(_ data: UsageData) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Details")
                .font(.caption)
                .foregroundColor(ClaudeTheme.textSecondary)
            if let plan = data.planName, !plan.isEmpty {
                InfoRow(label: "Plan", value: UsageFormatting.planName(plan))
            }
            if let org = data.orgName, !org.isEmpty {
                InfoRow(label: "Account", value: org)
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(ClaudeTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

}

private struct RawJsonSheet: View {
    let json: String?
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                Text(json ?? "No data")
                    .font(.system(size: 11, design: .monospaced))
                    .foregroundColor(ClaudeTheme.textPrimary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(16)
            }
            .background(ClaudeTheme.bg.ignoresSafeArea())
            .navigationTitle("API Response")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Close") { dismiss() }
                }
            }
        }
        .preferredColorScheme(.dark)
    }
}
