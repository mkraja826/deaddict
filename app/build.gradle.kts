import java.io.File
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
val appVersionCode = 100
val appVersionName = "1.0.0"

fun resourceNames(resourceFile: File): Set<String> =
    Regex("""<(?:string|plurals)\s+name="([^"]+)"""")
        .findAll(resourceFile.readText())
        .map { match -> match.groupValues[1] }
        .toSet()

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
    description = "Checks structural release, localization, policy, and staged-rollout safeguards."

    doLast {
        check(appVersionCode >= 100) { "Production versionCode must be at least 100." }
        check(Regex("""\d+\.\d+\.\d+""").matches(appVersionName)) {
            "Production versionName must use stable semantic versioning without a prerelease suffix."
        }
        check(appVersionName.substringBefore('.').toInt() >= 1) {
            "Production versionName must be 1.0.0 or later."
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

        val localeConfig = file("src/main/res/xml/locales_config.xml")
        check(localeConfig.isFile) { "Per-app locale configuration is missing." }
        listOf("en", "hi", "te").forEach { locale ->
            check(localeConfig.readText().contains("android:name=\"$locale\"")) {
                "locales_config.xml must include $locale."
            }
        }

        val baseStrings = file("src/main/res/values/strings.xml")
        check(baseStrings.isFile) { "Base string resources are missing." }
        val baseResourceNames = resourceNames(baseStrings)
        check(baseResourceNames.isNotEmpty()) { "Base string resources are empty." }
        listOf(
            "hi" to file("src/main/res/values-hi/strings.xml"),
            "te" to file("src/main/res/values-te/strings.xml"),
        ).forEach { (locale, localizedFile) ->
            check(localizedFile.isFile) { "Localized resources are missing for $locale." }
            val missing = baseResourceNames - resourceNames(localizedFile)
            check(missing.isEmpty()) {
                "Localized resources for $locale are missing: ${missing.sorted().joinToString()}"
            }
        }

        val requiredPublishFiles = listOf(
            rootProject.file("docs/legal/PRIVACY_POLICY.md"),
            rootProject.file("docs/legal/TERMS_OF_SERVICE.md"),
            rootProject.file("docs/legal/ACCOUNT_DELETION.md"),
            rootProject.file("docs/play/STORE_LISTING_EN.md"),
            rootProject.file("docs/play/DATA_SAFETY.md"),
            rootProject.file("docs/play/CONTENT_RATING.md"),
            rootProject.file("docs/play/ASSET_REQUIREMENTS.md"),
            rootProject.file("docs/play/PUBLISH_CHECKLIST.md"),
            rootProject.file("docs/play/RELEASE_NOTES_1.0.0.md"),
        )
        requiredPublishFiles.forEach { requiredFile ->
            check(requiredFile.isFile && requiredFile.length() >= 300L) {
                "Required publish document is missing or incomplete: ${requiredFile.relativeTo(rootProject.projectDir)}"
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

        println("Release readiness verified for DeAddict $appVersionName ($appVersionCode) at $rolloutPercent% rollout.")
    }
}

tasks.register("verifyPublishReadiness") {
    group = "verification"
    description = "Requires production credentials and public policy/support endpoints before Play publication."
    dependsOn("verifyReleaseReadiness")

    doLast {
        fun requiredValue(name: String): String =
            providers.gradleProperty(name).orNull
                ?: System.getenv(name)?.takeIf(String::isNotBlank)
                ?: error("$name is required for publication.")

        fun requireHttps(name: String): String {
            val value = requiredValue(name)
            check(value.startsWith("https://") && value.length > "https://".length + 3) {
                "$name must be a public HTTPS URL."
            }
            return value
        }

        requireHttps("DEADDICT_PRIVACY_POLICY_URL")
        requireHttps("DEADDICT_ACCOUNT_DELETION_URL")
        requireHttps("DEADDICT_SUPPORT_URL")

        val supportEmail = requiredValue("DEADDICT_SUPPORT_EMAIL")
        check(Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""").matches(supportEmail)) {
            "DEADDICT_SUPPORT_EMAIL must be a valid public support email."
        }

        listOf(
            "DEADDICT_DEVELOPER_NAME",
            "DEADDICT_SUPABASE_URL",
            "DEADDICT_SUPABASE_PUBLISHABLE_KEY",
            "DEADDICT_GOOGLE_SERVER_CLIENT_ID",
        ).forEach(::requiredValue)
        check(hasReleaseSigning) {
            "Release keystore path, passwords, and alias are required for publication."
        }

        println("Publish readiness verified. Public policy, deletion, support, backend, auth, and signing metadata are present.")
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
