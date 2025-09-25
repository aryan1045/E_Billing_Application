plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    id("com.google.gms.google-services")
    id ("kotlin-parcelize")
}

android {
    namespace = "com.example.e_billing"
    compileSdk = 35


    packagingOptions {
        exclude("META-INF/DEPENDENCIES")
    }


        packagingOptions {
            exclude("mozilla/public-suffix-list.txt")
        }



    defaultConfig {
        applicationId = "com.example.e_billing"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true

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

    // ✅ Corrected View Binding Configuration
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.support.annotations)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation ("androidx.cardview:cardview:1.0.0")


    // ✅ Firebase BoM (should be declared before Firebase dependencies)
    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))

    // ✅ Firebase Dependencies
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-analytics")
    implementation ("com.google.firebase:firebase-database:20.3.0")
    implementation ("com.google.firebase:firebase-auth:22.0.0")






    dependencies {
        implementation("com.twilio.sdk:twilio:9.0.0")
        {
            exclude(group = "javax.xml.bind", module = "jaxb-api")
            exclude(group = "jakarta.xml.bind", module = "jakarta.xml.bind-api")
        }
    }


    implementation ("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("androidx.multidex:multidex:2.0.1")




    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
