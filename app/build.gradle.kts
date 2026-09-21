plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val releaseKeystoreFile = System.getenv("UNIMANAGER_KEYSTORE_FILE")
val releaseKeystorePassword = System.getenv("UNIMANAGER_KEYSTORE_PASSWORD")
val releaseKeyAlias = System.getenv("UNIMANAGER_KEY_ALIAS")
val releaseKeyPassword = System.getenv("UNIMANAGER_KEY_PASSWORD")
val semanticVersion = System.getenv("UNIMANAGER_VERSION")?.takeIf { it.isNotBlank() } ?: "0.1.0"
val semanticVersionParts = semanticVersion.substringBefore('-').split('.')
val semanticVersionCode = semanticVersionParts
    .map { it.toIntOrNull() ?: 0 }
    .let { parts ->
        parts.getOrElse(0) { 0 } * 1_000_000 +
            parts.getOrElse(1) { 0 } * 1_000 +
            parts.getOrElse(2) { 0 }
    }

android {
    namespace = "com.zanuaimi.unimanager"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.zanuaimi.unimanager"
        minSdk = 23
        targetSdk = 36
        versionCode = semanticVersionCode
        versionName = semanticVersion
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    val releaseSigningConfig = if (
        !releaseKeystoreFile.isNullOrBlank() &&
        !releaseKeystorePassword.isNullOrBlank() &&
        !releaseKeyAlias.isNullOrBlank() &&
        !releaseKeyPassword.isNullOrBlank()
    ) {
        signingConfigs.create("release") {
            storeFile = file(releaseKeystoreFile)
            storePassword = releaseKeystorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
        }
    } else {
        null
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            releaseSigningConfig?.let { signingConfig = it }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.06.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.1")
    implementation("androidx.navigation:navigation-compose:2.9.0")
}
