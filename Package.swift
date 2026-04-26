// swift-tools-version:5.9

import PackageDescription

// Update url + checksum on each release. Compute checksum via:
//   swift package compute-checksum Retrostash.xcframework.zip
// XCFramework is built by `./gradlew :retrostash-ktor:assembleRetrostashReleaseXCFramework`
// and zipped + uploaded to the corresponding GitHub Release.

let version = "0.0.6"

let package = Package(
    name: "Retrostash",
    platforms: [
        .iOS(.v13),
    ],
    products: [
        .library(name: "Retrostash", targets: ["Retrostash"]),
    ],
    targets: [
        .binaryTarget(
            name: "Retrostash",
            url: "https://github.com/logickoder/retrostash/releases/download/\(version)/Retrostash.xcframework.zip",
            checksum: "d26681f2a026927d800a0dffbaa5430344356afc750b25009fd89acd8115fb43"
        ),
    ]
)
