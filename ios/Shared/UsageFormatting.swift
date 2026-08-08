import Foundation

/// Formatting helpers mirroring the Android app's label logic, shared by the UI and the widget.
enum UsageFormatting {

    /// Human title for a limit window's `kind`.
    static func limitTitle(_ kind: String) -> String {
        switch kind {
        case "session":     return "Current session"
        case "weekly_all":  return "Weekly \u{00B7} all models"
        case "weekly_opus": return "Weekly \u{00B7} Opus"
        default:
            let spaced = kind.replacingOccurrences(of: "_", with: " ")
            return spaced.prefix(1).uppercased() + spaced.dropFirst()
        }
    }

    /// "Plan_tier_max" → "Plan Tier Max".
    static func planName(_ raw: String) -> String {
        raw.replacingOccurrences(of: "_", with: " ")
            .split(separator: " ")
            .map { $0.prefix(1).uppercased() + $0.dropFirst().lowercased() }
            .joined(separator: " ")
    }

    /// "Resets Jun 18, 11:00 PM \u{00B7} in 2h 5m", or nil if there is no reset timestamp.
    static func resetLabel(_ iso: String?) -> String? {
        guard let iso, !iso.isEmpty, let date = parseISO(iso) else { return nil }
        return "Resets \(resetTime(date)) \u{00B7} \(timeRemaining(date))"
    }

    /// Compact widget countdown: "Session resets in 2h 5m".
    static func sessionCountdown(_ iso: String?) -> String {
        guard let iso, !iso.isEmpty, let date = parseISO(iso) else { return "" }
        if date < Date() { return "Resetting soon" }
        return "Session resets in \(shortRemaining(date))"
    }

    static func lastUpdated(_ date: Date?) -> String? {
        guard let date else { return nil }
        let f = DateFormatter()
        f.dateFormat = "h:mm a"
        return "Updated \(f.string(from: date))"
    }

    // MARK: Internals

    private static func parseISO(_ iso: String) -> Date? {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let d = f.date(from: iso) { return d }
        f.formatOptions = [.withInternetDateTime]
        return f.date(from: iso)
    }

    private static func resetTime(_ date: Date) -> String {
        let f = DateFormatter()
        f.dateFormat = "MMM d, h:mm a"
        return f.string(from: date)
    }

    private static func timeRemaining(_ date: Date) -> String {
        let now = Date()
        if date < now { return "Resetting soon" }
        let totalMinutes = Int(date.timeIntervalSince(now) / 60)
        let hours = totalMinutes / 60
        let minutes = totalMinutes % 60
        if hours > 0 { return "in \(hours)h \(minutes)m" }
        if minutes > 0 { return "in \(minutes)m" }
        return "in < 1 minute"
    }

    private static func shortRemaining(_ date: Date) -> String {
        let totalMinutes = max(Int(date.timeIntervalSince(Date()) / 60), 0)
        let hours = totalMinutes / 60
        let minutes = totalMinutes % 60
        return hours > 0 ? "\(hours)h \(minutes)m" : "\(minutes)m"
    }
}
