plugins {
    id("com.android.application")
}

val releaseKeystoreFile = System.getenv("UNIMANAGER_KEYSTORE_FILE")
val releaseKeystorePassword = System.getenv("UNIMANAGER_KEYSTORE_PASSWORD")
val releaseKeyAlias = System.getenv("UNIMANAGER_KEY_ALIAS")
val releaseKeyPassword = System.getenv("UNIMANAGER_KEY_PASSWORD")

android {
    namespace = "com.zanuaimi.unimanager"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.zanuaimi.unimanager"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
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
