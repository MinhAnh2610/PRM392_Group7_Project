plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)

  kotlin("plugin.serialization") version "2.1.20"
}

android {
  namespace = "com.tutorial.project"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.tutorial.project"
    minSdk = 26
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  kotlinOptions {
    jvmTarget = "11"
  }
  buildFeatures {
    compose = true
  }
}

dependencies {

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.ui.tooling.preview)
  implementation(libs.androidx.material3)
  implementation("com.github.skydoves:cloudy:0.2.7")
  // Compose BOM - ensures all Compose libraries use compatible versions
  implementation(platform("androidx.compose:compose-bom:2024.02.00"))

  // Material Icons Extended (no version needed with BOM)
  implementation("androidx.compose.material:material-icons-extended")

  implementation(libs.retrofit)
  implementation(libs.converter.gson)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.androidx.lifecycle.viewmodel.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx.v261)
  implementation(libs.androidx.lifecycle.livedata.ktx)
  implementation(libs.androidx.runtime.livedata)
  implementation(libs.androidx.navigation.compose)

  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
  implementation("io.coil-kt.coil3:coil-compose:3.2.0")
  implementation("io.coil-kt.coil3:coil-network-okhttp:3.2.0")

  implementation(platform("io.github.jan-tennert.supabase:bom:3.0.0-rc-1"))
  implementation("io.github.jan-tennert.supabase:postgrest-kt")
  implementation("io.github.jan-tennert.supabase:auth-kt")
  implementation("io.github.jan-tennert.supabase:realtime-kt")
  implementation("io.github.jan-tennert.supabase:storage-kt")
  implementation("io.github.jan-tennert.supabase:functions-kt")

  implementation("io.ktor:ktor-client-android:3.1.3")

  implementation("com.mapbox.maps:android:11.12.3")
  implementation("com.mapbox.extension:maps-compose:11.12.3")

  implementation("com.stripe:stripe-android:21.15.1")
  implementation("com.stripe:paymentsheet:21.15.1")

  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.ui.test.junit4)
  debugImplementation(libs.androidx.ui.tooling)
  debugImplementation(libs.androidx.ui.test.manifest)
}