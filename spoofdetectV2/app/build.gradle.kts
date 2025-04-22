plugins {
    id("com.android.application")
}


android {
    namespace = "com.example.spoofdetect"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.spoofdetect"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(libs.appcompat.v161)
    implementation(libs.material)
}
