plugins {
    id("com.android.application")
}

android {
    namespace = "com.dangernoodle.snake"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.dangernoodle.snake"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    // Same key as scripts/build-apk.sh so either build installs over the other.
    signingConfigs {
        create("sideload") {
            storeFile = rootProject.file("keystore/sideload.jks")
            storePassword = "dangernoodle"
            keyAlias = "sideload"
            keyPassword = "dangernoodle"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("sideload")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("sideload")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
