plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.amstudio.studymagic"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.amstudio.studymagic"
        minSdk = 27
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("com.squareup.picasso:picasso:2.8")
    
    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    
    // Markdown Rendering
    implementation("io.noties.markwon:core:4.6.2")

    // Confetti Animation
    implementation("nl.dionsegijn:konfetti-xml:2.0.4")
    //circle image view
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // PDF Viewer
    implementation("com.github.barteksc:AndroidPdfViewer:3.1.0-beta.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}