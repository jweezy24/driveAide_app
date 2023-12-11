plugins {
    id("com.android.application")
//    id("com.chaquo.python") version "15.0.0"
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
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("androidx.activity:activity:+")
    implementation("org.tensorflow:tensorflow-lite:+") // Replace with the latest version
    implementation("org.pytorch:pytorch_android_lite:1.13.1") // Replace with the latest version
//    implementation("org.pytorch:pytorch_android:1.9.0") // Replace with the latest version
    implementation("org.pytorch:pytorch_android_torchvision_lite:1.13.1") // Replace with the latest version


}
//chaquopy {
////    productFlavors {
////        //getByName("py310") { version = "3.10" }
//////        getByName("py311") { version = "3.11" }
////    }
//
//    defaultConfig {
//        version = "3.8"
//        pip {
//            install("numpy")
//            // "-r"` followed by a requirements filename, relative to the
//            // project directory:
////            install("-r", "src/main/python/requirements.txt")
//        }
//    }
//
//}
