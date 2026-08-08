// swift-tools-version: 5.9

// Swift Playground "App" package — build & run this ENTIRELY ON YOUR iPad with Apple's free
// Swift Playground app (no Mac, no PC, no developer account). See README.md in this folder.
//
// Note: Swift Playground supports a single app target only, so this build has NO widget extension
// (widgets require a Mac or a PC-sideload). You still get the full app: login + usage dashboard.

import PackageDescription
import AppleProductTypes

let package = Package(
    name: "ClaudeUsage",
    platforms: [
        .iOS("16.0")
    ],
    products: [
        .iOSApplication(
            name: "ClaudeUsage",
            targets: ["AppModule"],
            bundleIdentifier: "com.adriaan.claudeusage",
            teamIdentifier: "",
            displayVersion: "1.0",
            bundleVersion: "1",
            appIcon: .asset("AppIcon"),
            accentColor: .presetColor(.orange),
            supportedDeviceFamilies: [
                .pad,
                .phone
            ],
            supportedInterfaceOrientations: [
                .portrait,
                .landscapeRight,
                .landscapeLeft,
                .portraitUpsideDown(.when(deviceFamilies: [.pad]))
            ]
        )
    ],
    targets: [
        .executableTarget(
            name: "AppModule",
            path: "Sources"
        )
    ]
)
