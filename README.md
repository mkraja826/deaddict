# DeAddict

DeAddict is a private, local-first Android recovery companion for unwanted habits and addictions.

Core loop: **Notice → Pause → Choose → Learn → Improve**

## Development

Requirements: Android Studio JDK 21 and Android SDK 36.

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :core:programs:test :app:assembleDebug
```

The app uses Kotlin, Jetpack Compose, Material 3, Hilt, coroutines, and a layered architecture. See [ARCHITECTURE.md](ARCHITECTURE.md) and [PROJECT_PROGRESS_0_TO_100.md](PROJECT_PROGRESS_0_TO_100.md).

Hosted configuration is read from ignored `local.properties` during local development, with CI overrides accepted as Gradle project properties:

- `DEADDICT_SUPABASE_URL`
- `DEADDICT_SUPABASE_PUBLISHABLE_KEY`
- `DEADDICT_GOOGLE_SERVER_CLIENT_ID`

Never provide a Supabase secret or service-role key to the Android build.
