
plugins {
    id("com.android.application")
    
}

android {
    namespace = "org.buildsmali.provider"
    compileSdk = 35
    defaultConfig {
        applicationId = "org.buildsmali.provider"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        
        vectorDrawables { 
            useSupportLibrary = true
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    //buildFeatures {
    //    viewBinding = true  
    //}
    
}

dependencies {


    //implementation("com.google.android.material:material:1.9.0")
    //implementation("androidx.appcompat:appcompat:1.6.1")
    //implementation("androidx.constraintlayout:constraintlayout:2.1.4")
}
