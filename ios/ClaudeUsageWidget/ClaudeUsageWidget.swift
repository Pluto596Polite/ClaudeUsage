import WidgetKit
import SwiftUI

// MARK: - Timeline

struct UsageEntry: TimelineEntry {
    let date: Date
    let data: UsageData
    let loggedIn: Bool
}

/// Refreshes the widget by fetching fresh usage (when the session cookie is shared via the App
/// Group), falling back to the last cached snapshot. Mirrors the Android widget's 30-minute
/// self-refresh via the background worker.
struct UsageProvider: TimelineProvider {
    func placeholder(in context: Context) -> UsageEntry {
        UsageEntry(date: Date(), data: .empty, loggedIn: true)
    }

    func getSnapshot(in context: Context, completion: @escaping (UsageEntry) -> Void) {
        let store = SharedStore.shared
        completion(UsageEntry(date: Date(), data: store.usageData, loggedIn: store.isLoggedIn))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<UsageEntry>) -> Void) {
        Task {
            let store = SharedStore.shared
            // Try a live fetch; on any failure keep showing the cached snapshot. Save without
            // reloading timelines — we're already inside a timeline request, so a reload would loop.
            let data = (try? await store.refreshUsage(reloadWidgets: false)) ?? store.usageData
            let entry = UsageEntry(date: Date(), data: data, loggedIn: store.isLoggedIn)
            let next = Calendar.current.date(byAdding: .minute, value: 30, to: Date()) ?? Date().addingTimeInterval(1800)
            completion(Timeline(entries: [entry], policy: .after(next)))
        }
    }
}

// MARK: - Views

struct ClaudeUsageWidgetEntryView: View {
    @Environment(\.widgetFamily) private var family
    let entry: UsageEntry

    var body: some View {
        switch family {
        case .systemSmall:            SmallWidgetView(data: entry.data, loggedIn: entry.loggedIn)
        case .systemMedium:           MediumWidgetView(data: entry.data, loggedIn: entry.loggedIn)
        case .accessoryCircular:      AccessoryCircularView(data: entry.data)
        case .accessoryRectangular:   AccessoryRectangularView(data: entry.data)
        case .accessoryInline:        AccessoryInlineView(data: entry.data)
        default:                      SmallWidgetView(data: entry.data, loggedIn: entry.loggedIn)
        }
    }
}

// MARK: Home-screen (small)

private struct SmallWidgetView: View {
    let data: UsageData
    let loggedIn: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            header
            if !loggedIn {
                Text("Open app to sign in")
                    .font(.system(size: 11))
                    .foregroundColor(ClaudeTheme.textMuted)
            } else if data.limits.isEmpty {
                Text("Open app to load")
                    .font(.system(size: 11))
                    .foregroundColor(ClaudeTheme.textMuted)
            } else {
                if let session = data.sessionLimit {
                    LimitBlock(label: "Session", limit: session)
                }
                if let weekly = data.weeklyLimits.first {
                    LimitBlock(label: "Weekly", limit: weekly)
                }
                let countdown = UsageFormatting.sessionCountdown(data.sessionLimit?.resetsAt)
                if !countdown.isEmpty {
                    Text(countdown)
                        .font(.system(size: 10))
                        .foregroundColor(ClaudeTheme.textMuted)
                        .lineLimit(1)
                }
            }
            Spacer(minLength: 0)
        }
        .widgetPadding()
        .widgetBackground()
    }

    private var header: some View {
        HStack(spacing: 4) {
            Text("Claude")
                .font(.system(size: 13, weight: .bold))
                .foregroundColor(ClaudeTheme.orange)
            if let plan = data.planName, !plan.isEmpty {
                Text("\u{00B7} \(UsageFormatting.planName(plan))")
                    .font(.system(size: 11))
                    .foregroundColor(ClaudeTheme.textSecondary)
                    .lineLimit(1)
            }
        }
    }
}

// MARK: Home-screen (medium)

private struct MediumWidgetView: View {
    let data: UsageData
    let loggedIn: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 4) {
                Text("Claude")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(ClaudeTheme.orange)
                if let plan = data.planName, !plan.isEmpty {
                    Text("\u{00B7} \(UsageFormatting.planName(plan))")
                        .font(.system(size: 12))
                        .foregroundColor(ClaudeTheme.textSecondary)
                }
                Spacer()
                if let updated = data.lastFetchedDate {
                    Text(updated, style: .time)
                        .font(.system(size: 10))
                        .foregroundColor(ClaudeTheme.textMuted)
                }
            }

            if !loggedIn {
                Text("Open the app to sign in")
                    .font(.system(size: 12))
                    .foregroundColor(ClaudeTheme.textMuted)
            } else if data.limits.isEmpty {
                Text("Open the app to load your usage")
                    .font(.system(size: 12))
                    .foregroundColor(ClaudeTheme.textMuted)
            } else {
                HStack(spacing: 16) {
                    ForEach(Array(([data.sessionLimit].compactMap { $0 } + data.weeklyLimits).prefix(3))) { limit in
                        VStack(spacing: 6) {
                            WidgetRing(percent: limit.percent)
                                .frame(width: 46, height: 46)
                            Text(UsageFormatting.limitTitle(limit.kind))
                                .font(.system(size: 10))
                                .foregroundColor(ClaudeTheme.textSecondary)
                                .lineLimit(1)
                                .minimumScaleFactor(0.7)
                        }
                        .frame(maxWidth: .infinity)
                    }
                }
            }
            Spacer(minLength: 0)
        }
        .widgetPadding()
        .widgetBackground()
    }
}

// MARK: Lock Screen (accessory) — the closest iOS has to an always-visible menu-bar glance

private struct AccessoryCircularView: View {
    let data: UsageData

    var body: some View {
        let percent = data.primaryLimit?.percent ?? 0
        Gauge(value: Double(percent), in: 0...100) {
            Text("Cl")
        } currentValueLabel: {
            Text("\(percent)")
        }
        .gaugeStyle(.accessoryCircular)
    }
}

private struct AccessoryRectangularView: View {
    let data: UsageData

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text("Claude Usage").font(.headline)
            if let session = data.sessionLimit {
                Text("Session \(session.percent)%")
            }
            if let weekly = data.weeklyLimits.first {
                Text("Weekly \(weekly.percent)%")
            }
            if data.limits.isEmpty {
                Text("Open app to load").font(.caption)
            }
        }
    }
}

private struct AccessoryInlineView: View {
    let data: UsageData

    var body: some View {
        if let limit = data.primaryLimit {
            Text("Claude \(UsageFormatting.limitTitle(limit.kind)) \(limit.percent)%")
        } else {
            Text("Claude \u{2014} open app to load")
        }
    }
}

// MARK: Shared widget pieces

private struct LimitBlock: View {
    let label: String
    let limit: UsageLimit

    var body: some View {
        let color = ClaudeTheme.progressColor(percent: limit.percent)
        VStack(spacing: 4) {
            HStack {
                Text(label)
                    .font(.system(size: 12))
                    .foregroundColor(ClaudeTheme.textSecondary)
                Spacer()
                Text("\(limit.percent)%")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(color)
            }
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(ClaudeTheme.surfaceHi).frame(height: 8)
                    Capsule().fill(color).frame(width: geo.size.width * limit.fraction, height: 8)
                }
            }
            .frame(height: 8)
        }
    }
}

private struct WidgetRing: View {
    let percent: Int

    var body: some View {
        let color = ClaudeTheme.progressColor(percent: percent)
        ZStack {
            Circle().stroke(ClaudeTheme.surfaceHi, lineWidth: 5)
            Circle()
                .trim(from: 0, to: min(max(Double(percent) / 100, 0), 1))
                .stroke(color, style: StrokeStyle(lineWidth: 5, lineCap: .round))
                .rotationEffect(.degrees(-90))
            Text("\(percent)%")
                .font(.system(size: 11, weight: .semibold))
                .foregroundColor(color)
        }
    }
}

private extension View {
    func widgetPadding() -> some View { self.padding(14) }

    /// iOS 17+ requires `containerBackground` for widgets; keep the warm dark ground behind them.
    @ViewBuilder
    func widgetBackground() -> some View {
        if #available(iOS 17.0, *) {
            self.containerBackground(ClaudeTheme.bg, for: .widget)
        } else {
            ZStack { ClaudeTheme.bg; self }
        }
    }
}

// MARK: - Widget declaration

struct ClaudeUsageWidget: Widget {
    let kind = "ClaudeUsageWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: UsageProvider()) { entry in
            ClaudeUsageWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("Claude Usage")
        .description("Your Claude session and weekly usage at a glance.")
        .supportedFamilies([
            .systemSmall,
            .systemMedium,
            .accessoryCircular,
            .accessoryRectangular,
            .accessoryInline
        ])
    }
}
