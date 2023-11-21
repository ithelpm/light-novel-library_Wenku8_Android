plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("org.jetbrains.kotlin.android")
}
val SdkVersion = 33
android {
    compileSdkVersion(SdkVersion)
    buildToolsVersion = "33.0.1"
    defaultConfig {
        applicationId = "org.mewx.wenku8"
        minSdkVersion(16)
        targetSdkVersion(SdkVersion)
        versionCode = 57
        versionName = "1.18"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
    }
    buildTypes {
        create("customDebugType") {
            isDebuggable = true
            isMinifyEnabled = false
            // testCoverageEnabled true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
        release {
            isDebuggable = true
            isMinifyEnabled = true
            testProguardFile("proguard-rules-tests.pro") // FIXME - the rule does not work
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            ndk {
                // Default for arm only.
                abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            }
        }
    }
    flavorDimensions += listOf("default")
    productFlavors {
        // FIXME
        create("alpha") {
            // 内测渠道，群分发
            dimension = "default"
            manifestPlaceholders["PlayStore"] = "playstore"
        }

        create("playstore") {
            // Google Play Store
            dimension = "default"
            manifestPlaceholders["PlayStore"] = "playstore"
            ndk {
                abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
                debugSymbolLevel = "FULL"
            }
        }
    }
    packagingOptions {
        resources {
            excludes += listOf("META-INF/LICENSE.txt", "META-INF/NOTICE.txt", ".readme")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    namespace = "org.mewx.wenku8"
}

dependencies {
    // external libraries
    implementation("com.readystatesoftware.systembartint:systembartint:1.0.3")
    implementation("com.nostra13.universalimageloader:universal-image-loader:1.9.3")
    implementation("com.afollestad.material-dialogs:core:0.9.6.0")
    implementation("com.getbase:floatingactionbutton:1.9.0")
    implementation("com.makeramen:roundedimageview:2.0.1")
    implementation("com.davemorrissey.labs:subsampling-scale-image-view-androidx:3.10.0")
    implementation("com.github.castorflex.smoothprogressbar:library:1.1.0")
    implementation("com.jpardogo.googleprogressbar:library:1.2.0")
    implementation("org.adw.library:discrete-seekbar:1.0.1")
    implementation("com.nononsenseapps:filepicker:2.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.android.material:material:1.3.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("com.android.support:multidex:1.0.3")

    implementation(platform("com.google.firebase:firebase-bom:28.4.2"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("androidx.core:core-ktx:+")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("junit:junit:4.13.2")
    // Note Mockito 5.x has issue: 	java.lang.NoSuchMethodError: No static method stream([Ljava/lang/Object;)Ljava/util/stream/Stream; in class Ljava/util/Arrays; or its super classes (declaration of 'java.util.Arrays' appears in /system/framework/core-libart.jar)
    androidTestImplementation("org.mockito:mockito-core:2.19.0")
    androidTestImplementation("com.linkedin.dexmaker:dexmaker:2.19.1")
    androidTestImplementation("com.linkedin.dexmaker:dexmaker-mockito:2.19.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.4.0")
}

tasks.withType<Wrapper>() {
    gradleVersion = "8.4"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
