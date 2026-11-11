# AIClipStudio Skill API Extension

The Android assistant uses the existing AIClipStudio routes. This small, backward-compatible server extension passes user-selected Skills as structured context and allows caption instructions. The matching reference implementation was updated and tested in the temporary reference source checkout at `/tmp/AIClipStudio_ref`; deploy the same server changes with the AIClipStudio service before using these optional request fields.

## `POST /api/chat`

Existing body remains supported:

```json
{"id":"<job-id>","message":"<user message>"}
```

The Android client sends selected Skill IDs and up to 12 recent conversation/tool-result turns as optional fields:

```json
{"id":"<job-id>","message":"<user message>","skills":["automatic_subtitles","scene_detection"],"context":[{"role":"user","content":"..."},{"role":"assistant","content":"..."}]}
```

Only those three IDs are accepted; invalid, non-array, or overlong lists receive HTTP 400. Context accepts only `user` and `assistant` roles, with at most 12 messages of 2000 characters each. The backend adds Skill descriptions and current timeline context to the AI planner's system context and keeps the existing `{reply, renderId?, ...}` response shape. Render jobs continue to use `GET /api/render?id=<job-id>&rid=<render-id>`.

## `POST /api/captions`

Existing requests are unchanged. Android sends the stable `automatic_subtitles` Skill ID and an optional `instructions` field limited to 1000 characters:

```json
{"id":"<job-id>","auto":true,"skill":"automatic_subtitles","instructions":"Translate to Thai and keep cues concise."}
```

The server passes the bounded preference to its configured Gemini transcription provider while retaining timestamped cue JSON output. Without instructions, captions preserve the source language as before. Successful rendered captions still return a `renderId`; no render ID means no success claim is made by the app.

## Scene detection

No server route change is needed for scene detection. Android uses the existing structured tool contract:

```json
{"id":"<job-id>","tool":"detect_scenes","args":{"threshold":0.45}}
```

The `threshold` is omitted unless the user gives a valid explicit sensitivity. `/api/tool` invokes the Python `cv/scenes` handler and returns a tool result containing `scenes`; these visual cut markers are added to the Android timeline. This tool does not rank semantic highlights.

## Security

The Skill list is validated on the server. User instructions are bounded. AI provider keys remain exclusively in backend environment configuration (for example `GEMINI_API_KEY`); the Android client sends no provider credentials.

## `GET /api/health`

The Skill-capable server returns `capabilities.androidSkills: true` and `capabilities.captionInstructions: true`. The app checks these flags before executing selected Skills, so an older deployment fails with an update message instead of silently ignoring Skill context.

## Verification

The reference source changes and prompt/skill contract tests are in `/tmp/AIClipStudio_ref`. Run `node --test test/engine.test.js` from that source directory. Android code uses the unchanged existing access-token header and configured backend base URL.
