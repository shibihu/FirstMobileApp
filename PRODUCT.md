# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Solo short-form video creators working on a phone, on the go between shoots. They need to import a clip, cut it, and run AI assists without sitting at a desktop or switching to the web editor.

## Product Purpose

A native Kotlin and Jetpack Compose Android video editor inspired by AIClipStudio. The creator imports a video, previews and edits it, runs AI assists, and exports, all on the phone. Success means finishing a clip from import to output on the device.

## Positioning

AI skills are typed operations, not prompt-only labels. Selecting Automatic subtitles, Voice clean-up, or Scene detection dispatches the mapped backend operation and surfaces its real execution state. A neighboring prompt-only assistant could not truthfully copy that mechanism.

## Operating Context

Phone-based editing between shoots, against an existing AIClipStudio backend. Workflows include Storage Access Framework import, native preview, timeline editing, SRT import and export, native MP4 export, and an AI Studio conversation with skill selection.

## Capabilities and Constraints

- Compose dashboard and navigation for Dashboard, Media, Editor, AI, and Settings.
- Video import through Android's Storage Access Framework. The app requests no broad storage permission and retains the selected URI grant across process restarts when the provider supports persistable grants.
- Imported video name, duration, and dimensions are read natively.
- Native video preview with playback controls.
- AI Studio conversation and Skill registry: Automatic subtitles, Voice clean-up, and Scene detection.
- Conversation state is held in the activity-scoped ViewModel and survives navigation. Existing AIClipStudio job IDs are reused for the same selected source URI.
- Tool execution, render progress, retryable failures, scene markers, and completed render media are surfaced in the conversation.
- Backend base URL and encrypted access-token settings are stored on device; AI provider keys remain server-side.
- Dark and light appearance preference, persisted from Settings.
- Timeline editing, SRT import and export, and native MP4 export exist as separate native workflows.
- Backend contracts: `POST /api/upload`, `POST /api/chat`, `POST /api/captions`, `POST /api/tool`, and `GET /api/render`.
- No API keys or secrets are required by the current Android implementation.

## Brand Commitments

Name: AIClipStudio Native Android. It is an Android companion to AIClipStudio, not an embedding of the web application. No logo, voice, or asset commitments were established.

## Evidence on Hand

- `README.md`: build steps and implemented feature list.
- `docs/AIClipStudio-skills-api.md`: optional Skill and caption-instruction backend contract.
- No testimonials, customer evidence, benchmarks, pricing, or marketing assets. Future work must not fabricate them.

## Product Principles

1. Phone-first completion: a creator finishes a clip from import to output on one device.
2. Skills must execute, not claim: report success only after the backend reports done and rendered media downloads.
3. Preserve workspace state across navigation so an interrupted edit resumes where it stopped.
4. Keep the user's appearance choice authoritative across redesigns.
5. Keep provider credentials server-side.

## Accessibility & Inclusion

Platform defaults only: Android and Material behavior, with no product-specific accessibility standard beyond that.
