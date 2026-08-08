import SwiftUI

/// A single usage window ("Current session", "Weekly \u{00B7} all models", ...): big percent, ring,
/// bar and reset label. Mirrors the Android `UsageProgressCard`.
struct UsageProgressCard: View {
    let title: String
    let percent: Int
    let resetLabel: String?

    private var fraction: Double { min(max(Double(percent) / 100.0, 0), 1) }
    private var color: Color { ClaudeTheme.progressColor(percent: percent) }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .center) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.caption)
                        .foregroundColor(ClaudeTheme.textSecondary)
                    HStack(alignment: .lastTextBaseline, spacing: 1) {
                        Text("\(percent)")
                            .font(.system(size: 34, weight: .bold))
                            .foregroundColor(ClaudeTheme.textPrimary)
                        Text("%")
                            .font(.title3)
                            .foregroundColor(ClaudeTheme.textSecondary)
                    }
                    Text("used")
                        .font(.caption2)
                        .foregroundColor(ClaudeTheme.textSecondary)
                }
                Spacer()
                ProgressRing(fraction: fraction, color: color)
                    .frame(width: 72, height: 72)
            }

            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule().fill(ClaudeTheme.surfaceHi).frame(height: 6)
                    Capsule().fill(color)
                        .frame(width: geo.size.width * fraction, height: 6)
                }
            }
            .frame(height: 6)

            if let resetLabel, !resetLabel.isEmpty {
                Text(resetLabel)
                    .font(.subheadline)
                    .foregroundColor(ClaudeTheme.textSecondary)
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(ClaudeTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

/// Ring with a centred percent label, animating as the fraction changes.
struct ProgressRing: View {
    let fraction: Double
    let color: Color

    @State private var animated: Double = 0

    var body: some View {
        ZStack {
            Circle()
                .stroke(ClaudeTheme.surfaceHi, lineWidth: 7)
            Circle()
                .trim(from: 0, to: animated)
                .stroke(color, style: StrokeStyle(lineWidth: 7, lineCap: .round))
                .rotationEffect(.degrees(-90))
            Text("\(Int((animated * 100).rounded()))%")
                .font(.system(size: 14, weight: .semibold))
                .foregroundColor(color)
        }
        .onAppear {
            withAnimation(.easeOut(duration: 0.8)) { animated = fraction }
        }
        .onChange(of: fraction) { newValue in
            withAnimation(.easeOut(duration: 0.8)) { animated = newValue }
        }
    }
}

/// Label/value row used in the details card.
struct InfoRow: View {
    let label: String
    let value: String

    var body: some View {
        HStack {
            Text(label)
                .font(.subheadline)
                .foregroundColor(ClaudeTheme.textSecondary)
            Spacer()
            Text(value)
                .font(.subheadline.weight(.medium))
                .foregroundColor(ClaudeTheme.textPrimary)
        }
    }
}
