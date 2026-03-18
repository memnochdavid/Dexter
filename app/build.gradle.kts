import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { input ->
        localProperties.load(input)
    }
}



plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
}

android {
    namespace = "com.david.pokedex_api"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.david.pokedex_api"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }

}
val camerax_version = "1.3.0" // Revisa la última versión estable
dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Optional - Integration with activities
    implementation("androidx.activity:activity-compose")
    // Optional - Integration with ViewModels
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")
    // Optional - Integration with LiveData
    implementation("androidx.compose.runtime:runtime-livedata")
    // Optional - Integration with RxJava
    implementation("androidx.compose.runtime:runtime-rxjava2")
    implementation("androidx.compose.animation:animation")
    //retrofit
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    //room
    implementation("androidx.room:room-runtime:${libs.versions.room.get()}")
    implementation("androidx.room:room-ktx:${libs.versions.room.get()}")
    ksp("androidx.room:room-compiler:${libs.versions.room.get()}")

    //appwrite
//    implementation("io.appwrite:sdk-for-kotlin:5.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
//    implementation("androidx.compose.foundation:foundation:1.8.3") // O la versión que estés utilizando


    //gifs
    implementation("com.google.accompanist:accompanist-drawablepainter:0.35.0-alpha")
    implementation("com.google.accompanist:accompanist-pager:0.28.0")
    implementation("androidx.navigation:navigation-compose:2.7.5")

    //coil
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-gif:2.6.0")
    //video webm
    implementation("io.coil-kt:coil-video:2.6.0")
//    val mediaThreeVersion = "1.7.1"
//    implementation("androidx.media3:media3-exoplayer:$mediaThreeVersion")
//    implementation("androidx.media3:media3-exoplayer-dash:$mediaThreeVersion") // Si la usas
//    implementation("androidx.media3:media3-ui:$mediaThreeVersion")
//    implementation("androidx.media3:media3-decoder:$mediaThreeVersion") // Esta es la clave para software decoders

    // https://mvnrepository.com/artifact/org.videolan/vlc-lib-android
//    implementation("org.videolan:vlc-lib-android:3.1.7")
    implementation("org.videolan.android:libvlc-all:4.0.0-eap20")

    //-----------------------------
    // ExoPlayer core
    implementation ("androidx.media3:media3-exoplayer:1.7.1")

    // UI para PlayerView (usado en AndroidView)
    implementation ("androidx.media3:media3-ui:1.3.1")

    // Opcional: para formatos como WebM, MKV, etc.
//    implementation ("androidx.media3:media3-exoplayer-dash:1.3.1")
    implementation ("androidx.media3:media3-exoplayer-hls:1.7.1")
    implementation ("androidx.media3:media3-exoplayer-rtsp:1.7.1")
    implementation ("com.google.android.exoplayer:exoplayer:2.19.1")
    implementation ("com.google.android.exoplayer:exoplayer-ui:2.19.1")
    //-----------------------------





    //pager
    implementation("androidx.compose.foundation:foundation:1.7.0-alpha0")

    //palette
    implementation ("androidx.palette:palette-ktx:1.0.0")

    //constraintlayout
    implementation("androidx.constraintlayout:constraintlayout-compose:1.0.1")

    //lotties
    implementation("com.airbnb.android:lottie-compose:6.4.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.0")

    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")


    //camera X
    implementation ("androidx.camera:camera-core:${camerax_version}")
    implementation ("androidx.camera:camera-camera2:${camerax_version}")
    implementation ("androidx.camera:camera-lifecycle:${camerax_version}")
    implementation ("androidx.camera:camera-view:${camerax_version}")
    implementation ("androidx.camera:camera-extensions:${camerax_version}") // Opcional para efectos
    // Para permisos más fácil con Compose (opcional, pero recomendado)
    implementation ("com.google.accompanist:accompanist-permissions:0.33.2-alpha") // Revisa la última

    //ML kit
    implementation ("com.google.mlkit:image-labeling:17.0.8")

    //KTOR
    val ktor_version = "2.3.8" // Siempre es bueno revisar la última versión estable
    implementation ("io.ktor:ktor-client-core:$ktor_version")
    implementation ("io.ktor:ktor-client-android:$ktor_version") // Motor para Android (puedes usar CIO o OkHttp también)
    implementation ("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation ("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation ("io.ktor:ktor-client-logging:$ktor_version") // Para logging, útil en desarrollo

    // Kotlinx Serialization (Ktor lo usa para JSON)
    implementation ("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3") // Revisa la última






}