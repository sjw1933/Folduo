plugins { id("com.android.application") }
android {
 namespace = "jp.bunkaich.sukashimotion"
 compileSdk = 37
 buildToolsVersion = "36.0.0"
 defaultConfig {
  applicationId = "jp.bunkaich.sukashimotion"
  minSdk = 33
  targetSdk = 36
  versionCode = 42
  versionName = "0.1.21-fold8.1"
  testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
 }
 buildFeatures { buildConfig = true; aidl = true }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 buildTypes { release { isMinifyEnabled = false; signingConfig = signingConfigs.getByName("debug") } }
}
dependencies {
 implementation("dev.rikka.shizuku:api:13.1.5")
 implementation("dev.rikka.shizuku:provider:13.1.5")
 testImplementation("junit:junit:4.13.2")
 androidTestImplementation("androidx.test.ext:junit:1.3.0")
 androidTestImplementation("androidx.test:runner:1.7.0")
 androidTestImplementation("androidx.test:rules:1.7.0")
}

// Preserve the same notices in both the source distribution and the APK.
val licenseAssets = tasks.register<Sync>("prepareLicenseAssets") {
 from(rootProject.file("LICENSE"))
 from(rootProject.file("THIRD_PARTY_NOTICES.md"))
 from(rootProject.file("licenses"))
 into(layout.buildDirectory.dir("generated/licenseAssets/licenses"))
}
android.sourceSets.getByName("main").assets.directories.add(layout.buildDirectory.dir("generated/licenseAssets").get().asFile.path)
tasks.named("preBuild").configure { dependsOn(licenseAssets) }
