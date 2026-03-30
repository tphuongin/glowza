# Glowza

Glowza is an Android application built with Kotlin, Jetpack libraries, CameraX, Hilt, and ML Kit face detection. The app appears to focus on camera, photo editing, collage creation, gallery browsing, and onboarding flows.

## Demo

[▶ Watch Demo Video](https://drive.google.com/drive/folders/1rB_jynpGaiyz26SZ4R_8pHQSBa2DClAV)

## Project structure

- `app/` - Android application module
- `app/src/main/AndroidManifest.xml` - app manifest and activity declarations
- `app/build.gradle.kts` - module build configuration
- `settings.gradle.kts` - Gradle project settings
- `build.gradle.kts` - root build configuration
- `gradle/` - version catalog and wrapper configuration

## Key technologies

- Kotlin
- Android SDK 36
- CameraX (`camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-video`, `camera-view`, `camera-extensions`)
- Hilt dependency injection
- ML Kit Face Detection
- Glide image loading
- uCrop image cropping
- GPUImage image filters
- DataStore preferences
- View Binding

## Permissions

The app requests the following permissions:

- `android.permission.CAMERA`
- `android.permission.RECORD_AUDIO`
- `android.permission.READ_EXTERNAL_STORAGE`
- `android.permission.READ_MEDIA_IMAGES`
- `android.permission.WRITE_EXTERNAL_STORAGE` (up to SDK 28)

## Activities

The application includes these main activities:

- `MainActivity` - launcher activity
- `CameraActivity`
- `EditorActivity`
- `FinishActivity`
- `CollageActivity`
- `GalleryActivity`
- `OnboardingActivity`

## Build and run

### Prerequisites

- Android Studio
- JDK 11
- Android SDK 36

### Using Gradle wrapper

From the project root:

```bash
./gradlew assembleDebug
./gradlew installDebug
```

On Windows:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
```

### Open in Android Studio

Open the root folder `Glowza` in Android Studio and let it sync Gradle. Then build and run the `app` module on an emulator or device.

## Notes

- The app uses the `com.sgroupmobile.glowza` application ID.
- The main launcher theme is `Theme.Glowza.Splash`.
- `GlowzaApp` is the custom `Application` class declared in the manifest.

