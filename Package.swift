// swift-tools-version:5.9

import PackageDescription

// Update url + checksum on each release. Compute checksum via:
//   swift package compute-checksum Retrostash.xcframework.zip
// XCFramework is built by `./gradlew :retrostash-ktor:assembleRetrostashReleaseXCFramework`
// and zipped + uploaded to the corresponding GitHub Release.

let version = "0.0.7"

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
            checksum: "72d86c88499bdc3dca9b0572dcdb08e4ce6fdb995e9136530ad724cbd0d83330"
        ),
    ]
)
