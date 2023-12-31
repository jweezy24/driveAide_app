plugins {
    id("com.android.application")
//    id("com.chaquo.python") version "15.0.0"
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.driveaide"
    compileSdk = 34

    flavorDimensions += "pyVersion"
//    productFlavors {
        //create("py310") { dimension = "pyVersion" }
//        create("py311") { dimension = "pyVersion" }
//    }

    defaultConfig {
        applicationId = "com.example.driveaide"
        minSdk = 32
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += listOf( "arm64-v8a", "x86_64")
        }

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.navigation:navigation-fragment:2.5.3")
    implementation("androidx.navigation:navigation-ui:2.5.3")
    implementation("androidx.fragment:fragment:1.6.2")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("androidx.activity:activity:+")
    implementation("org.tensorflow:tensorflow-lite:+") // Replace with the latest version
//    implementation("org.pytorch:pytorch_android_lite:1.9.0") // Replace with the latest version
//    implementation("org.pytorch:pytorch_android_torchvision:1.9.0") // Replace with the latest version
    implementation("com.google.android.gms:play-services-maps:18.1.0")  // allows for google maps to be used
    implementation("com.google.android.gms:play-services-location:18.0.0") // This is for location services if needed
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation(platform("com.google.firebase:firebase-bom:32.6.0"))
    implementation("com.google.firebase:firebase-database")


    implementation("org.pytorch:pytorch_android_lite:1.13.1") // Replace with the latest version
//    implementation("org.pytorch:pytorch_android:1.9.0") // Replace with the latest version
    implementation("org.pytorch:pytorch_android_torchvision_lite:1.13.1") // Replace with the latest version
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

}