import SwiftUI

/// Claude's warm dark aesthetic, shared by the app UI and the widget so both read as one product.
/// Values mirror the Android `Color.kt` palette exactly.
enum ClaudeTheme {
    // Brand
    static let orange       = Color(hex: 0xD97757)
    static let orangeDark   = Color(hex: 0xB85E3E)
    static let orangeLight  = Color(hex: 0xF0956F)

    // Dark surface
    static let bg           = Color(hex: 0x1A1917)
    static let surface      = Color(hex: 0x252422)
    static let surfaceHi    = Color(hex: 0x2E2C2A)
    static let textPrimary  = Color(hex: 0xF0EDE8)
    static let textSecondary = Color(hex: 0x9E9B96)
    static let textMuted    = Color(hex: 0x6B6863)
    static let border       = Color(hex: 0x3A3835)

    // Status
    static let success      = Color(hex: 0x4CAF82)
    static let warning      = Color(hex: 0xE8A045)
    static let error        = Color(hex: 0xE05252)

    /// Progress colour ramp used by cards and the widget: orange → amber → red as usage climbs.
    static func progressColor(percent: Int) -> Color {
        switch percent {
        case 90...: return error
        case 70...: return warning
        default:    return orange
        }
    }
}

extension Color {
    init(hex: UInt32, alpha: Double = 1.0) {
        let r = Double((hex >> 16) & 0xFF) / 255.0
        let g = Double((hex >> 8) & 0xFF) / 255.0
        let b = Double(hex & 0xFF) / 255.0
        self.init(.sRGB, red: r, green: g, blue: b, opacity: alpha)
    }
}
