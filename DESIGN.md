---
version: alpha
name: tl;dh
description: Dark, calm and local-first. A long voice note compresses into a short, readable signal.
colors:
  primary: "#A50B5E"
  on-primary: "#FDF7FB"
  primary-deep: "#6F063F"
  glow: "#D72B84"
  background: "#08030A"
  on-background: "#FDF7FB"
  surface: "#160815"
  surface-raised: "#1F0C1B"
  on-surface: "#FDF7FB"
  on-surface-muted: "#D7B8CA"
  divider: "#2A1424"
  outline: "#4A2140"
  success: "#34D399"
  danger: "#FB7185"
  unsure: "#FBBF24"
  glass-bar: "rgba(8, 3, 10, 0.62)"
  glass-pane: "rgba(31, 12, 27, 0.72)"
typography:
  display-lg:
    fontFamily: Bricolage Grotesque
    fontSize: 44px
    fontWeight: 700
    lineHeight: 46px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Bricolage Grotesque
    fontSize: 30px
    fontWeight: 700
    lineHeight: 34px
    letterSpacing: -0.015em
  title-md:
    fontFamily: Bricolage Grotesque
    fontSize: 24px
    fontWeight: 700
    lineHeight: 30px
    letterSpacing: -0.01em
  tldr:
    fontFamily: Bricolage Grotesque
    fontSize: 26px
    fontWeight: 500
    lineHeight: 33px
    letterSpacing: -0.01em
  body-lg:
    fontFamily: Roboto
    fontSize: 16px
    fontWeight: 400
    lineHeight: 24px
  transcript:
    fontFamily: Roboto
    fontSize: 16px
    fontWeight: 400
    lineHeight: 27px
  body-md:
    fontFamily: Roboto
    fontSize: 15px
    fontWeight: 400
    lineHeight: 22px
  label-lg:
    fontFamily: Roboto
    fontSize: 14px
    fontWeight: 500
    lineHeight: 20px
  caption:
    fontFamily: Roboto
    fontSize: 13px
    fontWeight: 400
    lineHeight: 18px
    fontFeature: tnum
  micro:
    fontFamily: Roboto
    fontSize: 12px
    fontWeight: 500
    lineHeight: 16px
    fontFeature: tnum
rounded:
  control: 12px
  inner: 14px
  button: 16px
  group: 20px
  full: 9999px
spacing:
  xs: 4px
  sm: 8px
  md: 12px
  lg: 16px
  margin: 20px
  section: 28px
  section-lg: 32px
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    typography: "{typography.body-lg}"
    rounded: "{rounded.button}"
    height: 52px
    padding: 20px
  button-secondary:
    backgroundColor: "{colors.background}"
    textColor: "{colors.on-surface}"
    typography: "{typography.body-md}"
    rounded: "{rounded.button}"
    height: 52px
  button-danger:
    backgroundColor: "{colors.background}"
    textColor: "{colors.danger}"
    typography: "{typography.body-md}"
    rounded: "{rounded.button}"
    height: 52px
  icon-button:
    backgroundColor: "{colors.background}"
    textColor: "{colors.on-background}"
    rounded: "{rounded.full}"
    size: 48px
  group:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.on-surface}"
    rounded: "{rounded.group}"
    padding: 20px
  group-row-secondary:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.on-surface-muted}"
    typography: "{typography.caption}"
  divider:
    backgroundColor: "{colors.divider}"
    height: 1px
  segmented-track:
    backgroundColor: "{colors.background}"
    textColor: "{colors.on-surface-muted}"
    rounded: "{rounded.button}"
    padding: 4px
  segmented-selected:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    typography: "{typography.label-lg}"
    rounded: "{rounded.control}"
    height: 44px
  reply-option:
    backgroundColor: "{colors.surface-raised}"
    textColor: "{colors.on-surface}"
    typography: "{typography.body-md}"
    rounded: "{rounded.inner}"
    height: 56px
  outline-hairline:
    backgroundColor: "{colors.outline}"
    height: 1px
  pill-ready:
    backgroundColor: "rgba(52, 211, 153, 0.14)"
    textColor: "{colors.success}"
    typography: "{typography.micro}"
    rounded: "{rounded.full}"
  pill-unsure:
    backgroundColor: "rgba(251, 191, 36, 0.14)"
    textColor: "{colors.unsure}"
    typography: "{typography.micro}"
    rounded: "{rounded.full}"
  pill-very-unsure:
    backgroundColor: "rgba(251, 113, 133, 0.14)"
    textColor: "{colors.danger}"
    typography: "{typography.micro}"
    rounded: "{rounded.full}"
  pill-recommended:
    backgroundColor: "rgba(215, 43, 132, 0.18)"
    textColor: "{colors.on-surface}"
    typography: "{typography.micro}"
    rounded: "{rounded.full}"
  word-unsure:
    backgroundColor: "rgba(251, 191, 36, 0.15)"
    textColor: "{colors.on-surface}"
    typography: "{typography.transcript}"
  word-very-unsure:
    backgroundColor: "rgba(251, 113, 133, 0.18)"
    textColor: "{colors.on-surface}"
    typography: "{typography.transcript}"
  glass-bar:
    backgroundColor: "{colors.glass-bar}"
    textColor: "{colors.on-background}"
  glass-pane:
    backgroundColor: "{colors.glass-pane}"
    textColor: "{colors.on-surface}"
    rounded: "{rounded.full}"
  waveform-idle:
    backgroundColor: "{colors.outline}"
    width: 3px
  waveform-done:
    backgroundColor: "{colors.glow}"
    width: 3px
  waveform-tail:
    backgroundColor: "{colors.primary-deep}"
    width: 4px
  headline:
    backgroundColor: "{colors.background}"
    textColor: "{colors.on-background}"
    typography: "{typography.display-lg}"
  screen-title:
    backgroundColor: "{colors.background}"
    textColor: "{colors.on-background}"
    typography: "{typography.headline-md}"
  settings-title:
    backgroundColor: "{colors.background}"
    textColor: "{colors.on-background}"
    typography: "{typography.title-md}"
  summary:
    backgroundColor: "{colors.background}"
    textColor: "{colors.on-background}"
    typography: "{typography.tldr}"
---

# tl;dh Design System

## Overview

tl;dh turns a long WhatsApp voice note into something you can read in seconds. The interface carries that idea: a long, muted waveform compresses into three short bars and a dot. Everything else stays quiet so the content (the summary, the words to check, the replies) is the loudest thing on screen.

The mood is dark, calm and private. The app works offline; nothing in the visual language suggests clouds, sync or AI magic. It should feel like a precise instrument with one moment of delight per screen, not a dashboard.

**The One Focal Moment Rule.** Each screen has a single moving or emphasized element: the breathing waveform on start, the filling waveform while processing, the summary in large type on the result. Nothing competes with it.

**The Verbatim Rule.** Summaries and replies are built from what was said. The UI says so ("Wörtlich aus dem Transkript, nichts dazuerfunden") and never decorates generated content with sparkles or AI symbols.

Motion is short and physical. Entering elements and state changes use a fast-out, soft-landing curve (cubic-bezier 0.23, 1, 0.32, 1); elements moving on screen use 0.77, 0, 0.175, 1. Pressed controls scale to 0.97 over 160 ms. UI transitions stay under 300 ms; only ambient loops (breathing waveform 2.4 s, frontier blink) are longer. When the system turns animations off, every loop stops and transitions snap.

## Colors

The palette is fixed brand color: a near-black aubergine background, one step of plum surface, and a magenta accent.

- **primary** (#A50B5E) marks the one main action per screen, the selected segment and the active radio. It is never used for body text: as text it fails contrast (2.7:1 on background).
- **glow** (#D72B84) is for large or graphic elements only: the waveform progress, the mark, the semicolon in the wordmark, spinners. Below 18 px it does not reach 4.5:1, so small labels use on-surface on a glow-tinted background instead.
- **surface** and **surface-raised** create depth by stepping up in lightness, not by shadow.
- **on-surface-muted** carries secondary text; **divider** and **outline** separate rows and outline secondary buttons.
- **success**, **danger** and **unsure** are state colors. Unsure (amber) and danger (rose) always come with a second cue: a dotted or wavy underline for words, a text label in pills.

**The Accent Budget Rule.** One filled primary element per screen. If a second thing wants the accent, it gets an outline instead.

## Typography

Two families with separate jobs.

- **Bricolage Grotesque** (bundled, SIL OFL 1.1) is the display voice: the start headline "Long story, short.", screen headlines, the settings title and the summary on the result screen. Bold for headlines, Medium for the summary so longer text stays readable.
- **Roboto** (system sans) does everything else: body, rows, labels, captions.

Numbers, timestamps, percentages and durations use tabular figures (tnum) so counters do not jitter. Text is sentence case everywhere; no all-caps eyebrows. German compound words and long file names must wrap or ellipsize cleanly; numbers and dates are never truncated.

## Layout

A single column on a 4/8 dp rhythm with 20 dp screen margins. Sections are separated by 28–32 dp; rows inside a group by hairline dividers inset to the text start (56 dp after an icon).

Screens are built from a scrolling column with an optional top bar and bottom dock. The primary action lives in the bottom dock, within thumb reach, and stays visible while content scrolls underneath. Touch targets are at least 48 dp; full-width buttons are 52 dp high; reply options 56 dp.

Content is laid out start-aligned. Centering is reserved for single short captions under a button.

## Elevation & Depth

Depth comes from surface steps: background → surface → surface-raised. There are no drop shadows and no glows behind cards.

**The Glass Rule.** Frosted glass appears only on chrome that floats above content, and only while content is actually underneath it:

- **Top bars** (result, settings, processing) are transparent at rest and turn into glass as soon as the content scrolls below them: a 28 dp backdrop blur, a glass-bar tint and a white hairline at 8 % along the bottom edge.
- **Bottom docks** (start, processing, setup, error) do the same in reverse: glass while more content is hidden below.
- **Floating panes** (the "Session gelöscht" toast, the snippet player) are always glass: glass-pane tint, blur, and a lit outline that fades from 18 % white at the top to 4 % at the bottom.

Backdrop blur needs Android 12 (RenderEffect). On older versions the same elements get a near-opaque tint, so text contrast never depends on the blur.

Surfaces and the primary button carry a subtle **lit rim**: a 1 dp border whose white fades from about 9 % (22 % on the button) at the top edge to nothing, like light catching the edge of a pane. It is a nuance, not a border you notice.

## Shapes

Corners scale with size: 12 dp for segmented controls, 14 dp for reply options, 16 dp for buttons, 20 dp for groups, fully round for icon buttons, pills, play buttons and floating panes. The app mark uses a squircle (continuous curvature), which is also the preferred launcher mask.

The mark: three vertical bars of decreasing height (3 : 2 : 1) followed by a dot, on a 108 dp grid with the glyph inside the 66 dp safe zone. The same glyph is the monochrome layer for Android 13 themed icons.

## Components

**Primary button.** Filled primary, on-primary label in body-lg medium, optional 20 dp leading icon, lit rim. Pressed: scale 0.97. Disabled: divider fill, muted label.

**Secondary button.** Transparent with a 1 dp outline border. The danger variant ("Session löschen") uses a danger label and icon, never a filled red block.

**Group.** Surface background, 20 dp corners, lit rim. Holds rows separated by inset dividers. Groups never nest.

**Segmented control.** Recessed track in background color with 4 dp padding; the selected segment fills with primary. Used for the reply tone.

**Radio rows** for model choice: 22 dp ring in outline color, glow ring and dot when selected, title, description and a status line ("Aktiv", "Geladen", "Nicht geladen · 190 MB Download") or a progress bar while downloading.

**Pills.** Micro text on a 14–18 % tint of the state color, fully rounded. Always contain words ("Zahl/Betrag · 31 %"), never only a color dot.

**Uncertain words.** Tinted background plus underline: dotted amber for unsure, wavy rose for very unsure. Tapping plays the moment around the word. The same marks appear in the summary and in the check rows, and a legend explains them under the transcript.

**Play button.** Round, outlined, at least 72 × 48 dp, play icon plus timestamp. While playing it fills with primary and shows three equalizer bars; a glass snippet pane with a stop button floats at the bottom.

**Waveform.** 3 dp bars on a 6 dp pitch. Idle bars use outline color; progress fills them with glow up to a blinking white frontier. The processing waveform is computed from the real loudness of the voice note.

**Toast.** Glass pane with a success check and one sentence, shown for about three seconds after a session is deleted or a run is cancelled.

## Do's and Don'ts

- Do keep one filled accent element per screen.
- Do give every state color a second cue (underline style, label text, icon).
- Do use glass only for floating chrome, and only when something is underneath.
- Do return to the start screen with a confirmation after closing a result; never leave old content on screen.
- Do label every icon-only button for TalkBack.
- Don't use gradients as backgrounds, glows behind cards, or neon outlines.
- Don't use the accent or glow color for small text.
- Don't use emoji or AI sparkle icons; icons are 24 dp line icons with round caps.
- Don't center whole screens or nest cards in cards.
- Don't animate layout for longer than 300 ms or ignore the system's animation setting.
