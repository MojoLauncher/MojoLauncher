plugins {
    id("com.android.library")
}

android {
    namespace = "com.bzlzhh.ng_gl4es"
    compileSdk = 36

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        ndkVersion = "28.2.13676358"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("proguard") {
            isMinifyEnabled = true
            initWith(getByName("debug"))
        }
        create("fordebug") {
        }
    }
    externalNativeBuild {
        cmake {
            path = file("NG-GL4ES/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
