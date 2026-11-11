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
- AI Studio conversation and Skill registry: Automatic subtitles, Voice clean-up, and Scene detection. Skills are typed operations, not prompt-only labels; selecting them dispatches to their mapped backend operation.
- Conversation state is held in the activity-scoped ViewModel and survives navigation. Existing AIClipStudio job IDs are reused for the same selected source URI.
- Tool execution, render progress, retryable failures, scene markers, and completed render media are surfaced in the conversation.
- Existing backend URL and encrypted access-token settings are used; backend provider keys remain server-only.
- Dark/light appearance preference.

## Backend Contracts

- The app uses `POST /api/upload`, `POST /api/chat`, `POST /api/captions`, `POST /api/tool`, and `GET /api/render` from AIClipStudio.
- AIClipStudio needs the documented optional Skill and caption-instruction fields in [docs/AIClipStudio-skills-api.md](docs/AIClipStudio-skills-api.md) deployed for structured planner context and custom subtitle instructions. Without that backend update, those optional fields are ignored or rejected depending on the deployed server version.
- Voice clean-up relies on the configured AI provider and the original `/api/chat` render workflow. The app reports success only after render polling returns `done` and rendered media is downloaded.
- Automatic subtitle language instructions require a transcription-capable server provider; the backend currently transcribes through its Gemini STT path.
- Scene detection reports visual cuts through the server's `detect_scenes` tool. Semantic highlight ranking is not available in that tool.
- Full chat model tool-call planning requires an AI provider configured on the backend. Skill selection still maps to explicit known API operations; it does not fabricate execution for unsupported Skills.
- Timeline editing, SRT import/export, and native MP4 export remain available in the Android editor as separate native workflows.

No API keys or secrets are required by the current Android implementation.