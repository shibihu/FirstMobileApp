---
name: AIClipStudio Native Android
description: A phone-native video editing studio where AI skills execute as typed operations.
colors:
  signal-blue: "#6AA7FF"
  booth-ink: "#0B1220"
  blue-mist: "#EAF2FF"
  bench-ink: "#0F172A"
  mixer-teal: "#5EEAD4"
  bench-white: "#F5F7FB"
  set-white: "#FFFFFF"
  slate-grey: "#475569"
  moonlit-blue: "#8CC5FF"
  night-ink: "#07111A"
  deep-booth-navy: "#1D2F4D"
  booth-navy: "#090E17"
  panel-navy: "#111827"
  dark-paper: "#E2E8F0"
  dark-slate: "#CBD5E1"
  teal-deep: "#0F766E"
  teal-container: "#CCFBF1"
  teal-container-ink: "#134E4A"
  bench-surface-variant: "#E7ECF3"
  bench-outline: "#64748B"
  bench-outline-variant: "#CBD5E1"
  surface-container: "#F1F5F9"
  booth-surface-variant: "#1E293B"
  booth-container: "#131C2A"
  error-red: "#B3261E"
  error-container: "#FEE4E2"
  error-container-ink: "#7A271A"
  error-red-dark: "#F2B8B5"
  error-container-dark: "#8C1D18"
typography:
  headline-medium:
    fontFamily: "Roboto, Helvetica Neue, Arial, sans-serif"
    fontSize: "28px"
    fontWeight: 400
    lineHeight: 1.29
    letterSpacing: "0px"
  headline-small:
    fontFamily: "Roboto, Helvetica Neue, Arial, sans-serif"
    fontSize: "24px"
    fontWeight: 400
    lineHeight: 1.33
    letterSpacing: "0px"
  title-large:
    fontFamily: "Roboto, Helvetica Neue, Arial, sans-serif"
    fontSize: "22px"
    fontWeight: 400
    lineHeight: 1.27
    letterSpacing: "0px"
  title-medium:
    fontFamily: "Roboto, Helvetica Neue, Arial, sans-serif"
    fontSize: "16px"
    fontWeight: 500
    lineHeight: 1.5
    letterSpacing: "0.15px"
  body-large:
    fontFamily: "Roboto, Helvetica Neue, Arial, sans-serif"
    fontSize: "16px"
    fontWeight: 400
    lineHeight: 1.5
    letterSpacing: "0.5px"
  body-medium:
    fontFamily: "Roboto, Helvetica Neue, Arial, sans-serif"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: 1.43
    letterSpacing: "0.25px"
  body-small:
    fontFamily: "Roboto, Helvetica Neue, Arial, sans-serif"
    fontSize: "12px"
    fontWeight: 400
    lineHeight: 1.33
    letterSpacing: "0.4px"
  label-large:
    fontFamily: "Roboto, Helvetica Neue, Arial, sans-serif"
    fontSize: "14px"
    fontWeight: 500
    lineHeight: 1.43
    letterSpacing: "0.1px"
  label-medium:
    fontFamily: "Roboto, Helvetica Neue, Arial, sans-serif"
    fontSize: "12px"
    fontWeight: 500
    lineHeight: 1.33
    letterSpacing: "0.5px"
  label-small:
    fontFamily: "Roboto, Helvetica Neue, Arial, sans-serif"
    fontSize: "11px"
    fontWeight: 500
    lineHeight: 1.45
    letterSpacing: "0.5px"
rounded:
  caption: "6px"
  media: "8px"
  chip: "8px"
  field: "4px"
  card: "12px"
  bubble: "18px"
  composer: "22px"
  hero: "24px"
  full: "999px"
spacing:
  xxs: "4px"
  xs: "6px"
  sm: "8px"
  md: "10px"
  lg: "12px"
  card-pad: "14px"
  xl: "16px"
  xxl: "20px"
components:
  button-primary:
    backgroundColor: "{colors.signal-blue}"
    textColor: "{colors.booth-ink}"
    typography: "{typography.label-large}"
    rounded: "{rounded.full}"
    padding: "0 24px"
    height: "40px"
  button-primary-hero:
    backgroundColor: "{colors.set-white}"
    textColor: "{colors.booth-ink}"
    typography: "{typography.label-large}"
    rounded: "{rounded.full}"
    padding: "0 24px"
    height: "40px"
  button-outlined:
    backgroundColor: "{colors.set-white}"
    textColor: "{colors.signal-blue}"
    typography: "{typography.label-large}"
    rounded: "{rounded.full}"
    padding: "0 24px"
    height: "40px"
  button-tall:
    backgroundColor: "{colors.signal-blue}"
    textColor: "{colors.booth-ink}"
    typography: "{typography.label-large}"
    rounded: "{rounded.full}"
    padding: "0 20px"
    height: "52px"
  card-section:
    backgroundColor: "{colors.set-white}"
    textColor: "{colors.bench-ink}"
    typography: "{typography.body-medium}"
    rounded: "{rounded.card}"
    padding: "16px"
  card-hero:
    backgroundColor: "{colors.signal-blue}"
    textColor: "{colors.booth-ink}"
    rounded: "{rounded.hero}"
    padding: "20px"
  stat-card:
    backgroundColor: "{colors.set-white}"
    textColor: "{colors.bench-ink}"
    typography: "{typography.headline-small}"
    rounded: "{rounded.card}"
    padding: "16px"
  skill-row:
    backgroundColor: "{colors.set-white}"
    textColor: "{colors.bench-ink}"
    typography: "{typography.body-medium}"
    rounded: "{rounded.card}"
    padding: "9px 12px"
    height: "52px"
  chat-bubble-user:
    backgroundColor: "{colors.blue-mist}"
    textColor: "{colors.bench-ink}"
    typography: "{typography.body-medium}"
    rounded: "{rounded.bubble}"
    padding: "11px 14px"
  field-outlined:
    backgroundColor: "{colors.set-white}"
    textColor: "{colors.bench-ink}"
    typography: "{typography.body-large}"
    rounded: "{rounded.field}"
    padding: "16px"
  chip-assist:
    backgroundColor: "{colors.set-white}"
    textColor: "{colors.bench-ink}"
    typography: "{typography.label-large}"
    rounded: "{rounded.chip}"
    padding: "0 12px"
    height: "32px"
  nav-bar:
    backgroundColor: "{colors.set-white}"
    textColor: "{colors.bench-ink}"
    typography: "{typography.label-medium}"
    height: "80px"
  composer-bar:
    backgroundColor: "{colors.set-white}"
    textColor: "{colors.bench-ink}"
    typography: "{typography.body-medium}"
    rounded: "{rounded.composer}"
    padding: "8px 12px"
---

# Design System: AIClipStudio Native Android

## Overview

**Creative North Star: "Field Studio"**

This is a compact instrument panel for a creator working between shoots. Everything on screen is a control or a readout: a metric, a progress value, the name of the clip in hand. Chrome stays quiet so state stays loud, because the one thing the creator must never lose track of is whether a Skill is still running, finished, or failed.

The system is Jetpack Compose with Material 3, used close to its baseline. Type is Material's own scale, shapes are Material's own pills and rounded containers, and depth is flat at rest. Restraint is deliberate: the app is an operating surface, not a landing page, and a creator holding a phone does not want to learn a new visual language mid-project.

Accent arrives in exactly two places. Signal Blue marks the current job (hero panel, primary action, selected Skill, send affordance). Mixer Teal never appears as marketing color; it carries tool and execution context, including tool-result bubbles. Neutral surfaces do the rest of the work, with Slate Grey for supporting text and Bench Ink for anything that must be read first.

**Key Characteristics:**
- Material 3 baseline type and shapes; no custom display face, no custom geometry.
- Flat surfaces at rest, with elevation reserved for state and the fixed composer.
- One job accent (Signal Blue) and one execution accent (Mixer Teal).
- Metrics lead with the number, label follows.
- Every screen is a single scrolling column on a phone; no tablet or landscape variants exist.

## Colors

Cool, instrument-grade blues on near-white or near-black neutrals. No warm hue appears anywhere in the shipped scheme.

### Primary
- **Signal Blue** (#6AA7FF): Light-theme primary. The hero gradient's leading stop, primary button fill, send affordance, selected-chip container, and the tint of the empty-state sparkle icon.
- **Moonlit Blue** (#8CC5FF): Dark-theme primary. Same jobs as Signal Blue, lifted in lightness so it holds against Booth Navy.
- **Booth Ink** (#0B1220): The only text color permitted on Signal Blue fills. Reaches 7.65:1 against #6AA7FF.
- **Blue Mist** (#EAF2FF): Primary container. User chat bubbles, selected Skill icon wells.

### Secondary
- **Mixer Teal Container** (#CCFBF1 light / #134E4A dark): Tool-result bubbles and selected Skill wells. Teal says "a Skill ran", not "look here".

### Tertiary
- **Mixer Teal Deep** (#0F766E): Light-theme tertiary for text and small marks — scene-marker timestamps, tool-row headings. The light teal is too faint to read on white, so light theme uses its deep step. Dark theme reuses **Mixer Teal** (#5EEAD4), which reads at 13.06:1 on Booth Navy.

### Neutral
- **Bench White** (#F5F7FB): Light-theme app background. Slightly cool, one step off pure white so that white cards lift without a shadow.
- **Set White** (#FFFFFF): Light-theme surface. Every Card and navigation bar sits on it.
- **Bench Ink** (#0F172A): Light-theme primary text and hero body text. 16.65:1 on Bench White.
- **Slate Grey** (#475569): Light-theme secondary text: durations, filenames, helper lines, empty-state copy. 7.07:1 on Bench White.
- **Bench Surface Variant** (#E7ECF3): Assistant bubbles, unselected Skill wells. A cool grey, never Material's lavender.
- **Bench Outline** (#64748B): Field borders, ruler ticks. 4.9:1 on white so the boundary is visible, not implied.
- **Bench Outline Variant** (#CBD5E1): Dividers and chip borders only.
- **Surface Container Family** (#F1F5F9 / #E9EFF6 / #E2E8F0): The navigation bar and filled cards pick their tone from here, so tonal depth stays in the same cool family.
- **Booth Navy** (#090E17): Dark-theme background. Near-black with a blue cast rather than pure black.
- **Panel Navy** (#111827): Dark-theme surface for cards and the navigation bar.
- **Dark Paper** (#E2E8F0): Dark-theme primary text.
- **Dark Slate** (#CBD5E1): Dark-theme secondary text.

### Error
- **Error Red** (#B3261E) on Set White for light, **Error Red Dark** (#F2B8B5) on Booth Navy for dark, with matching containers (#FEE4E2 / #8C1D18). Failure bubbles and inline error lines use the container; icons and short error text use the role itself. Error never borrows Mixer Teal.

### Named Rules
**The Two-Signal Rule.** Signal Blue means "this is the current job". Mixer Teal means "this is execution state". Neither color is ever used to fill space or add cheer.

**The Role-Only Rule.** Every color comes from `MaterialTheme.colorScheme`. The only hard-coded literals allowed are currentColor icon tints inside the hero and the black scrim behind subtitle overlays.

**The Complete-Role Rule.** Every Material 3 role the app reads is defined in `Theme.kt`, in both schemes. An undefined role silently falls back to baseline Material lavender, and one lavender bubble breaks the whole palette argument.

## Typography

**Display Font:** None. The system has no display face.
**Body Font:** Roboto (Material 3 baseline via `Typography()`), with Helvetica Neue and Arial as fallbacks outside Android.

**Character:** System-native and unremarkable on purpose. The type is the phone's own voice; hierarchy comes from size and color, never from a font swap. A custom typeface would read as a template, not a tool.

### Hierarchy
- **Headline** (400, 28sp/36sp, `headlineMedium`): The hero claim on Dashboard — the imported filename, or "Start an edit". One per screen at most.
- **Headline Small** (400, 24sp/32sp): Stat card values. The number is the largest thing in the card.
- **Title Large** (400, 22sp/28sp): Empty-conversation question in AI Studio.
- **Title Medium** (500, 16sp/24sp, 0.15sp): Card and panel headings, top-bar titles, the hero's eyebrow line.
- **Body Large** (400, 16sp/24sp, 0.5sp): Primary list content, settings row labels, single-line input text.
- **Body Medium** (400, 14sp/20sp, 0.25sp): Hero supporting copy, chat message text, card body, media metadata.
- **Body Small** (400, 12sp/16sp, 0.4sp): Metadata and helper lines: durations, codecs, token status, backend constraints.
- **Label Large** (500, 14sp/20sp, 0.1sp): Button and chip text, tool-row headings.
- **Label Medium** (500, 12sp/16sp, 0.5sp): Bottom navigation labels, trim readouts, export percentage.
- **Label Small** (500, 11sp/16sp, 0.5sp): Top-bar subtitle in AI Studio, time-ruler ticks, Skill limitations.

### Named Rules
**The Baseline Rule.** Use Material 3's scale as shipped. Sizes may be selected by role; the family and weights stay untouched. No screen may introduce a second font family.

**The Value-First Rule.** In any metric or stat block, the value takes the larger style and the label takes Body Small in Slate Grey. The label never outranks its own number.

## Layout

Phone portrait only. There are no `layout-sw600dp` resources, no landscape composition, and no breakpoints; the app ships as a single-column surface.

Content screens sit on a **20dp side margin with 12dp top and bottom padding** (Dashboard, Media, Settings). The Editor tightens to **16dp sides and 8dp vertical** because the video preview needs width. Vertical rhythm comes from `Arrangement.spacedBy` on the column: 16dp between sections on Dashboard and Media, 12dp on Settings and Editor, 10dp between chat messages, 8dp inside mixed control rows, 6dp inside chip rows.

Cards choose their own inner padding by content: 20dp for the hero and for Dashboard's quick-action and recent-media panels, 16dp for stat cards and settings sections, 14dp for editor panels and chat bubbles' horizontal inset. Card corner → inner element gaps stay on the same 8/12 scale.

Top app bars are composed inside the scrolling column rather than pinned in a `Scaffold`, so they scroll away with content. The five-destination bottom navigation bar is the only fixed element at app level; AI Studio additionally fixes its composer to the bottom and pads for the keyboard with `imePadding()`. Chat bubbles cap at 88% width for user messages and 96% for everything else, so alignment — not width — separates the speakers.

## Elevation & Depth

Flat by default. Section cards are filled `Card`s at zero elevation; separation comes from Set White panels on Bench White background, not from shadow. Elevation signals interaction state or fixed position, never hierarchy: `ElevatedCard` appears only for the Skill selection rows inside the composer, and the composer surface uses 2dp tonal elevation because it stays pinned over scrolling content. The top app bars are flat. There is no glow, no gradient shadow, and no stacked-shadow vocabulary.

One atmospheric gradient exists: the Dashboard hero paints a linear gradient from Signal Blue to the same hue at 72% alpha, inside a 24dp card. It is the single expressive moment in the app.

### Named Rules
**The Flat-At-Rest Rule.** Surfaces are flat until they are interactive, floating, or in motion. A shadow on a static card is a defect, not a flourish.

## Shapes

Pills for actions, rounded rectangles for containers, circles for icons and identity. Action buttons, outlined buttons, and text buttons are fully pill-shaped (Material 3 `Shapes.Full`). Cards keep Material's 12dp. Radius grows with the conversational, "soft" role of the surface rather than with its size: media 8dp, cards 12dp, chat bubbles 18dp, the composer field 22dp, the hero 24dp. Text fields stay nearly square at Material's 4dp so input reads as form, not as chat. The subtitle overlay is the smallest radius at 6dp — it must hug the video, not float above it.

Icon wells are 34–36dp circles with tinted fills (Signal Blue container, `surfaceVariant`, or Booth Ink at 12% alpha over the hero). Nothing is clipped at an angle; there is no diagonal, no notch, and no custom silhouette.

### Named Rules
**The Role-Not-Scale Rule.** Pick a radius by what the surface does — media, container, conversation, hero — not by how large it is. Two same-size elements may legitimately differ.

## Components

### Buttons
- **Shape:** Pill at every size (999px effective, Material `Shapes.Full`).
- **Primary:** Signal Blue fill, Booth Ink label, Label Large text, Material's 40dp height and 24dp side padding. Icon plus 8dp spacer plus label when an icon is present.
- **Tall primary:** Dashboard quick actions and the send control rise to 52dp so the thumb target survives one-handed use.
- **Hero primary:** Inside the hero the primary button inverts to a surface fill with on-surface label, so it stays the strongest element on a blue field.
- **Secondary:** OutlinedButton with a neutral 1dp outline and Signal Blue label; used for paired or reversible actions (Cancel export, Import SRT, Split at playhead, Import audio).
- **Tertiary:** TextButton in Signal Blue for retry, dismiss, and reversible actions ("Remove clip", "Delete cue", "Clear token"). Destructive text buttons (Delete clip) take the error role instead and never sit beside a constructive one.

### Chips
- **Style:** 8dp radius, surface fill, `outlineVariant` border, Label Large text, and an 18dp leading Skill icon. Chosen Skills appear as a horizontally scrolling row, each with a trailing close glyph.
- **State:** Selection is expressed by the chip existing at all — the chip row only renders chosen Skills. Each chip is at least 48dp tall, and its click action is announced as "Remove <skill>". Selection inside the expanded panel instead uses a Signal Blue container well and a trailing check.

### Cards / Containers
- **Corner Style:** 12dp by default (24dp for the hero).
- **Background:** Set White on Bench White in light; Panel Navy on Booth Navy in dark.
- **Shadow Strategy:** None at rest. See Elevation.
- **Border:** None. Cards are fill-only, which is why the background must stay a step off the surface.
- **Internal Padding:** 20dp (hero, Dashboard panels), 16dp (stat and settings cards), 14dp (editor panels, skill rows).

### Inputs / Fields
- **Style:** `OutlinedTextField` with a 1dp neutral outline, surface fill, 4dp radius, Label Large floating label, Body Large input text. The composer is the exception: 22dp radius, 4 max lines, placeholder instead of floating label, 22dp sending progress in place of the send glyph.
- **Focus:** Material's default outline → Signal Blue outline and label swap. No custom glow.
- **Error / Disabled:** Error text uses `colorScheme.error` at Body Small. Disabled send falls back to Slate Grey tint rather than dimming the whole row.

### Navigation
Bottom `NavigationBar`, five destinations: Dashboard, Media, Editor, AI, Settings. Default item is Slate Grey icon and label at Label Medium; the selected item takes Material's pill indicator in Blue Mist with Signal Blue icon and Bench Ink label. AI Studio adds a two-line top bar: title plus a Label Small subtitle carrying backend and clip status, truncated to one line.

### Hero Panel (signature)
The only expressive surface in the app: a 24dp card containing a Signal Blue gradient, a 36dp Booth-Ink-at-12% circle holding the sparkle icon, a Title Medium eyebrow ("Creative workspace"), a Headline Medium primary line that is either the imported filename or "Start an edit", a Body Medium status line, and a full-width surface pill button. Every word and glyph on it is Booth Ink, which measures 7.65:1 against Signal Blue in light and 10.46:1 against Moonlit Blue in dark. It states the current job in one glance and offers exactly one next action.

### Stat Row (signature)
Three equal-width cards, each `weight(1f)`, 12dp apart. Value in Headline Small, label in Body Small Slate Grey, centered. Missing data renders an em dash, never "0" or "unknown", so absence reads as absence.

### Chat Bubbles (signature)
Full-width column with an 88% (user) or 96% (other) surface. User: Blue Mist fill. Assistant: `surfaceVariant` fill. Tool: `tertiaryContainer` fill — teal in both themes — with a 16dp Skill icon, skill name in Label Large, and a 14dp progress ring or check beside it. Failed: `errorContainer` with a TextButton "Retry". Radius 18dp, padding 11dp vertical and 14dp horizontal, Body Medium text.

### Destructive Actions
Deleting a clip, a cue, or an audio track is undoable: the action runs immediately and a snackbar offers Undo for ten seconds, restoring the pre-delete timeline snapshot. Clearing the server access token is irreversible from the phone, so it asks for confirmation in a dialog instead. Status and error messages surface as snackbars, never as a `TextButton` whose only action is dismissal.

### Skill Row (signature)
`ElevatedCard` at full width: 34dp circle well, skill name in Body Medium, description in Body Small, limitation in Label Small Slate Grey, trailing check when selected. This is the only elevated, tappable row in the app and the only place where a shadow carries affordance.

## Do's and Don'ts

### Do:
- **Do** read every color from `MaterialTheme.colorScheme` so the appearance preference in Settings stays authoritative.
- **Do** keep the hero to one primary action and one status line.
- **Do** lead metrics with the value at Headline Small and put the label in Body Small Slate Grey beneath it.
- **Do** use an em dash for absent data in stat and metadata positions.
- **Do** keep the 20dp content margin outside the Editor, and 16dp inside it.
- **Do** show execution state in Mixer Teal and interactive state in Signal Blue.
- **Do** use full pills for actions and 12dp corners for containers.
- **Do** preserve the fixed bottom navigation bar as the only persistent chrome.
- **Do** give every touch target at least 48dp, including selected-Skill chips.
- **Do** make a destructive edit undoable, and reserve a dialog for what undo cannot restore.

### Don't:
- **Don't** add a second accent hue; the two-signal pair is the whole palette argument.
- **Don't** put white Body text on Signal Blue (2.45:1). Use Booth Ink (7.65:1).
- **Don't** introduce a custom font family or a display face.
- **Don't** add shadows to static cards, or a second elevated row style beyond the Skill row.
- **Don't** rely on Material fallback roles: every role a screen reads must be defined in `Theme.kt` in both schemes, or it resolves to baseline lavender.
- **Don't** register design tokens in `Color.kt` without wiring them into `Theme.kt`; an unused token is a claim the UI does not keep.
- **Don't** pin a second app bar under the bottom navigation; top bars scroll with content in this system.
- **Don't** fabricate testimonials, benchmarks, or marketing claims in UI copy.
