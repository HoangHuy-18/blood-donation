plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.hienmau"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.hienmau"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("com.microsoft.signalr:signalr:6.0.0") // Nếu dùng SignalR
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0") // Để chuyển JSON sang Object

    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")

    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.0.1") // Để lấy vị trí hiện tại
    implementation("com.google.android.gms:play-services-base:18.2.0")
    implementation("com.google.maps.android:android-maps-utils:3.4.0")

    implementation("com.google.code.gson:gson:2.10.1")

    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("com.google.android.material:material:1.9.0")

    // tải thư viện Firebase Messaging về
    implementation("com.google.firebase:firebase-messaging:23.4.1")

    // Thêm BOM để quản lý phiên bản Firebase tốt hơn
    implementation(platform("com.google.firebase:firebase-bom:32.7.2"))

    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

}