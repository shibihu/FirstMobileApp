# AIClipStudio Native Android

Native Kotlin and Jetpack Compose implementation inspired by the AIClipStudio editor. The Android app does not embed the reference web application.

## Build

Requirements: the JDK, Android SDK, and NDK versions supported by the existing Gradle configuration. Build the debug APK with:

```sh
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. The existing Gradle wrapper, Android Gradle Plugin, Kotlin, SDK, and NDK configuration are retained.

## Implemented

- Compose dashboard and navigation for Dashboard, Media, Editor, AI, and Settings.
- Video import through Android's Storage Access Framework. Access is limited to the video explicitly selected by the user; no broad storage permission is requested.
- Imported video name, duration, and dimensions are read natively. The selected URI permission and selection are retained across app process restarts when the provider supports persistable grants.
- Native Android video preview with playback controls.
- Dark/light appearance preference.

## Limitations

- The reference AIClipStudio relies on a Node.js server, Python media-analysis service, FFmpeg, and Remotion. Those services are not bundled or run on Android.
- AI transformations and automatic subtitles are unavailable until a compatible backend API client and configured service are implemented.
- The current timeline displays the imported clip but does not support clip trimming, multiple tracks, audio editing, subtitle editing, or effects.
- MP4 rendering/export is not implemented. No export button is presented as functional.
- Playback uses Android's native `VideoView`; codec support depends on the device.

No API keys or secrets are required by the current Android implementation.