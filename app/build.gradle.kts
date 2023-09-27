/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    kotlin("plugin.serialization")
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
    id("org.jlleitschuh.gradle.ktlint")
}

android {
    namespace = "me.stanis.apps.fiRun"
    compileSdk = 33

    defaultConfig {
        applicationId = "me.stanis.apps.fiRun"
        minSdk = 30
        targetSdk = 33
        versionCode = 100
        versionName = "1.0"
        vectorDrawables {
            useSupportLibrary = true
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.majorVersion
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.6"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("com.google.android.gms:play-services-wearable:18.0.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation(platform("androidx.compose:compose-bom:2023.03.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.activity:activity-compose:1.7.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.7.0")
    implementation("com.github.polarofficial:polar-ble-sdk:5.1.0")
    implementation("io.reactivex.rxjava3:rxjava:3.1.6")
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")
    implementation("com.google.dagger:hilt-android:2.46.1")

    implementation("androidx.wear.compose:compose-material:1.2.0-beta02")
    implementation("androidx.wear.compose:compose-foundation:1.2.0-beta02")
    implementation("androidx.wear.compose:compose-navigation:1.2.0-beta02")
    implementation("androidx.wear:wear:1.3.0-rc01")

    implementation("androidx.hilt:hilt-navigation-compose:1.0.0")

    implementation("androidx.navigation:navigation-compose:2.6.0")

    implementation("androidx.compose.material:material-icons-core:1.5.0-beta02")
    implementation("androidx.compose.material:material-icons-extended:1.5.0-beta02")

    implementation("androidx.health:health-services-client:1.0.0-beta03")
    implementation("androidx.lifecycle:lifecycle-service:2.6.1")
    implementation("androidx.room:room-runtime:2.5.2")
    implementation("androidx.test:rules:1.5.0")
    annotationProcessor("androidx.room:room-compiler:2.5.2")
    implementation("androidx.room:room-ktx:2.5.2")
    implementation("com.google.android.horologist:horologist-compose-layout:0.4.7")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.5.1")
    kapt("androidx.room:room-compiler:2.5.2")
    kapt("com.google.dagger:hilt-compiler:2.46.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.9")
    testImplementation("androidx.room:room-testing:2.5.2")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.8.22")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.0.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.0")

    testImplementation("com.google.dagger:hilt-android-testing:2.46.1")
    testAnnotationProcessor("com.google.dagger:hilt-compiler:2.46.1")
    kaptTest("com.google.dagger:hilt-compiler:2.46.1")

    implementation("androidx.wear:wear-remote-interactions:1.0.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    implementation(kotlin("reflect"))
}

// Allow references to generated code
kapt {
    correctErrorTypes = true
}

ktlint {
    android.set(true)
    outputColorName.set("RED")
}
