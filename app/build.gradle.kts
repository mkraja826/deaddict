import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val deaddictLocalProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}

val releaseStoreFile = System.getenv("DEADDICT_KEYSTORE_PATH")
val releaseStorePassword = System.getenv("DEADDICT_KEYSTORE_PASSWORD")
val releaseKeyAlias = System.getenv("DEADDICT_KEY_ALIAS")
val releaseKeyPassword = System.getenv("DEADDICT_KEY_PASSWORD")
val hasReleaseSigning = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }
val appVersionCode = 1
val appVersionName = "0.1.0"

android {
    namespace = "com.deaddict.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.deaddict.app"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val supabaseUrl = providers.gradleProperty("DEADDICT_SUPABASE_URL")
            .orElse(deaddictLocalProperties.getProperty("DEADDICT_SUPABASE_URL", ""))
        val supabaseKey = providers.gradleProperty("DEADDICT_SUPABASE_PUBLISHABLE_KEY")
            .orElse(deaddictLocalProperties.getProperty("DEADDICT_SUPABASE_PUBLISHABLE_KEY", ""))
        val googleServerClientId = providers.gradleProperty("DEADDICT_GOOGLE_SERVER_CLIENT_ID")
            .orElse(deaddictLocalProperties.getProperty("DEADDICT_GOOGLE_SERVER_CLIENT_ID", ""))
        buildConfigField("String", "SUPABASE_URL", "\"${supabaseUrl.get()}\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"${supabaseKey.get()}\"")
        buildConfigField("String", "GOOGLE_SERVER_CLIENT_ID", "\"${googleServerClientId.get()}\"")
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(requireNotNull(releaseStoreFile))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

tasks.register("verifyReleaseReadiness") {
    group = "verification"
    description = "Checks structural release and staged-rollout safeguards."

    doLast {
        check(appVersionCode > 0) { "versionCode must be positive." }
        check(Regex("""\d+\.\d+\.\d+([-.][0-9A-Za-z.-]+)?""").matches(appVersionName)) {
            "versionName must use semantic versioning."
        }
        check(android.buildTypes.getByName("release").isMinifyEnabled) {
            "Release minification must remain enabled."
        }

        val manifest = file("src/main/AndroidManifest.xml").readText()
        listOf(
            "android:allowBackup=\"false\"",
            "android:usesCleartextTraffic=\"false\"",
            "android:supportsRtl=\"true\"",
        ).forEach { requiredSetting ->
            check(manifest.contains(requiredSetting)) {
                "AndroidManifest.xml is missing release safeguard: $requiredSetting"
            }
        }

        val rolloutRaw = providers.gradleProperty("DEADDICT_ROLLOUT_PERCENT").orNull
            ?: System.getenv("DEADDICT_ROLLOUT_PERCENT")
            ?: "5"
        val rolloutPercent = rolloutRaw.toIntOrNull()
            ?: error("DEADDICT_ROLLOUT_PERCENT must be a whole number.")
        check(rolloutPercent in 1..100) {
            "DEADDICT_ROLLOUT_PERCENT must be between 1 and 100."
        }

        val wideRolloutAllowed = providers.gradleProperty("DEADDICT_ALLOW_WIDE_ROLLOUT").orNull
            ?.toBooleanStrictOrNull()
            ?: System.getenv("DEADDICT_ALLOW_WIDE_ROLLOUT")?.toBooleanStrictOrNull()
            ?: false
        check(rolloutPercent <= 25 || wideRolloutAllowed) {
            "Rollouts above 25% require DEADDICT_ALLOW_WIDE_ROLLOUT=true after health review."
        }

        println("Release readiness verified for a staged rollout of $rolloutPercent%.")
    }
}

dependencies {
    implementation(project(":core:programs"))
    implementation(project(":core:database"))
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.functions)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services)
    implementation(libs.googleid)
    implementation(libs.datastore.preferences)
    implementation(libs.work.runtime)
    implementation(libs.androidx.core)
    implementation(libs.biometric)
    implementation(libs.billing)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.compose.ui.test.junit4)
}
