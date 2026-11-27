plugins { id("com.android.application") }
android {
    namespace = "kr.personal.historyjobs"
    compileSdk = 35
    defaultConfig {
        applicationId = "kr.personal.historyjobs"
        minSdk = 26
        testInstrumentationRunner = "kr.personal.historyjobs.UiSmokeRunner"
        targetSdk = 35
        versionCode = 14
        versionName = "0.1.13"
    }
    compileOptions { isCoreLibraryDesugaringEnabled = true; sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    packaging { resources { excludes += setOf("META-INF/DEPENDENCIES", "META-INF/LICENSE", "META-INF/LICENSE.txt", "META-INF/NOTICE", "META-INF/NOTICE.txt") } }
    lint { abortOnError = true; checkReleaseBuilds = true }
    buildTypes { getByName("release") { isMinifyEnabled = false } }
}
dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation("androidx.work:work-runtime:2.10.1")
}
