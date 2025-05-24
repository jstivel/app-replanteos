plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.replanteosapp"
    compileSdk = 35 // Mantener en 35, es la última versión de API en este momento.

    defaultConfig {
        applicationId = "com.example.replanteosapp"
        minSdk = 24 // Puedes mantenerlo si necesitas compatibilidad, pero 26-29 es más común hoy día.
        targetSdk = 35
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true // Mantener si usas Compose en otras partes de la app o planeas usarlo.
    }
}

dependencies {

    // AndroidX Core y Lifecycle (versiones estables y compatibles con la última AppCompat)
    implementation("androidx.core:core-ktx:1.13.1") // Última versión estable
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0") // Última versión estable (no alpha)
    implementation("androidx.appcompat:appcompat:1.6.1") // Agregada, es buena práctica para FragmentActivity


    // Si usas Compose (y parece que sí, por los plugins y activity.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom)) // Asegúrate de que esta línea esté presente
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // CameraX (¡TODAS A LA MISMA VERSIÓN ESTABLE MÁS RECIENTE!)
    // Consulta https://developer.android.com/jetpack/androidx/releases/camera para la última estable
    // La 1.3.3 es la última estable.
    implementation("androidx.camera:camera-core:1.3.3")
    implementation("androidx.camera:camera-camera2:1.3.3")
    implementation("androidx.camera:camera-lifecycle:1.3.3")
    implementation("androidx.camera:camera-view:1.3.3") // Para PreviewView

    // Location Services (versión ya correcta)
    implementation("com.google.android.gms:play-services-location:21.2.0")

    // ConstraintLayout (versión ya correcta)
    implementation(libs.androidx.constraintlayout)

    // Pruebas (no modificadas)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}