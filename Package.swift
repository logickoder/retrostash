// swift-tools-version:5.9

import PackageDescription

// Update url + checksum on each release. Compute checksum via:
//   swift package compute-checksum Retrostash.xcframework.zip
// XCFramework is built by `./gradlew :retrostash-ktor:assembleRetrostashReleaseXCFramework`
// and zipped + uploaded to the corresponding GitHub Release.

let version = "0.0.5"

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
            checksum: "REPLACE_WITH_RELEASE_CHECKSUM"
        ),
    ]
)
