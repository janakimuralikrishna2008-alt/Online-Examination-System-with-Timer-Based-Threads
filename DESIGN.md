---
name: Online Examination System with Timer-Based Threads
description: Precision testing design system unifying Java 21 Swing FlatLaf and React 18 TypeScript web frontend
colors:
  primary: "#182b49"
  primary-interactive: "#2563eb"
  accent-indigo: "#4f46e5"
  status-emerald: "#10b981"
  status-amber: "#f59e0b"
  status-rose: "#ef4444"
  neutral-ink: "#020617"
  neutral-surface: "#ffffff"
  neutral-slate: "#f8fafc"
  neutral-border: "#e2e8f0"
  text-main: "#0f172a"
  text-muted: "#64748b"
typography:
  display:
    fontFamily: "'Segoe UI', 'Inter', -apple-system, sans-serif"
    fontSize: "22px"
    fontWeight: 700
    lineHeight: 1.2
  headline:
    fontFamily: "'Segoe UI', 'Inter', -apple-system, sans-serif"
    fontSize: "18px"
    fontWeight: 700
    lineHeight: 1.3
  title:
    fontFamily: "'Segoe UI', 'Inter', -apple-system, sans-serif"
    fontSize: "15px"
    fontWeight: 600
    lineHeight: 1.4
  body:
    fontFamily: "'Segoe UI', 'Inter', -apple-system, sans-serif"
    fontSize: "13px"
    fontWeight: 400
    lineHeight: 1.5
  label:
    fontFamily: "'Consolas', monospace"
    fontSize: "11px"
    fontWeight: 600
    letterSpacing: "0.04em"
rounded:
  sm: "4px"
  md: "6px"
  lg: "10px"
  pill: "9999px"
spacing:
  xs: "4px"
  sm: "8px"
  md: "12px"
  lg: "16px"
  xl: "24px"
components:
  button-primary:
    backgroundColor: "{colors.primary-interactive}"
    textColor: "{colors.neutral-surface}"
    rounded: "{rounded.md}"
    padding: "8px 16px"
  button-primary-hover:
    backgroundColor: "{colors.primary}"
  button-danger:
    backgroundColor: "{colors.status-rose}"
    textColor: "{colors.neutral-surface}"
    rounded: "{rounded.md}"
    padding: "8px 16px"
  card-surface:
    backgroundColor: "{colors.neutral-surface}"
    rounded: "{rounded.lg}"
    padding: "16px 20px"
  badge-pill:
    rounded: "{rounded.pill}"
    padding: "3px 10px"
---

# Design System: Online Examination System with Timer-Based Threads

## Overview

**Creative North Star: "The Precision Lab"**

The Online Examination System with Timer-Based Threads embodies the disciplined, high-reliability atmosphere of an advanced testing laboratory. It rejects ornamental visual noise and decorative excess in favor of crisp data density, authoritative temporal countdowns, and unmistakable state verification. Every screen exists to eliminate test-taking cognitive friction, reassure candidates of system stability, and provide instant, transparent feedback.

The dual-surface architecture bridges an enterprise Java 21 Swing desktop application (styled with modern FlatLaf components) and a modern React 18 / TypeScript web application. While each surface respects its runtime environment—desktop Swing honoring native OS workstation affordances and React leveraging responsive web flexbox/grid containers—they share identical semantic visual tokens: deep academic ink foundations, high-visibility digital timer readouts, and tactical state indicators.

**Key Characteristics:**
- **Analytical Data Density:** Compact padding, structured question palettes, and tabular review grids optimized for high-information scanning.
- **Authoritative Chronometry:** Monospace digital timer readouts backed by background thread countdowns, featuring unmistakable green-to-amber-to-red threshold shifts.
- **High-Contrast Legibility:** Deep academic navy and slate text set against stark white cards and clean slate backgrounds (`#f8fafc`).
- **Tactical Tri-State Palettes:** Distinct, accessible color coding for Answered (Cyber Emerald), Flagged (Instrument Amber), and Unanswered (Neutral Slate) items.

---

## Colors

The system uses a focused, high-contrast palette anchored in institutional academic ink with tactical status signals.

### Primary
- **Deep Academic Ink** (`#182b49` in Swing / `#0f172a` in Web): Used for primary headers, navigation backgrounds, frame borders, and visual anchors. Establishes institutional gravity and authority.
- **Electric Instrument Blue** (`#2563eb` in Web / `#4f46e5` in Swing): The active interactive accent. Powers primary submission buttons, selected radio cards, active navigation links, and focus rings.

### Secondary
- **Instrument Slate** (`#64748b`): Neutral secondary tone used for descriptive text, subtitle labels, secondary buttons, table column headers, and neutral badges.
- **Subtle Ice Slate** (`#f1f5f9` / `#f8fafc`): Page canvas backgrounds and card hover highlights that reduce visual glare during prolonged examination sessions.

### Tertiary
- **Cyber Emerald** (`#10b981`): The primary positive verification token. Indicates answered questions, passed examinations, active database connections, and progress bar health.
- **Amber Warning** (`#f59e0b`): Warning threshold token. Highlights flagged questions, timer countdown alerts (< 5 minutes / < 15 seconds), and review prompts.
- **Critical Rose** (`#ef4444`): High-consequence token reserved for final exam submission, time expiration, failed attempts, and validation errors.

### Neutral
- **Card White** (`#ffffff`): The primary container and card surface color across both desktop dialogs and web components.
- **Hairline Border** (`#e2e8f0`): 1px structural dividing lines separating question navigation, table rows, and card perimeters.
- **Main Text Ink** (`#0f172a`): High-contrast, WCAG AAA-compliant body text for question stems and answer choices.

### Named Rules
**The Academic Ink Rule.** High-saturation decorative colors are strictly prohibited on background canvases. The deep navy/slate ink tone must occupy all framing and navigation, ensuring that bright functional colors (Emerald, Amber, Rose) immediately command attention.

**The Cyber Emerald State Rule.** Green is reserved exclusively for verified completion: answered questions, passing scorecard scores, and live service health. It is never used for purely decorative flourishes.

**The Tri-State Question Rule.** Across both desktop and web question palettes, every question node must strictly reflect one of three states: Answered (`#10b981`), Flagged for Review (`#f59e0b`), or Unanswered (`#64748b` / `#cbd5e1`).

---

## Typography

Typography prioritizes tabular legibility, rapid reading comprehension, and drift-free digital countdown rendering.

**Display Font:** `Segoe UI` (Desktop) / `Inter` (Web)  
**Body Font:** `Segoe UI` (Desktop) / `Inter` (Web)  
**Digital Timer & Mono:** `Consolas` (Desktop) / Monospace Tabular Numbers (Web)

**Character:** Technical, neutral, and meticulously legible. Question stems and answer options are set with comfortable line heights to prevent eye fatigue during lengthy reading passages.

### Hierarchy
- **Display** (Bold 700, 22px / 1.75rem, line-height 1.2): Screen titles, welcome headers, and scorecard summary banners.
- **Headline** (Bold 700, 18px / 1.25rem, line-height 1.3): Exam titles, section headers, and dialog titles.
- **Title** (Bold 600, 15px / 1.05rem, line-height 1.4): Question stems, metric card titles, and modal subheadings.
- **Body** (Regular 400, 13px / 0.92rem, line-height 1.5): Multiple-choice option labels, explanatory text, and general prose. Max line length 75ch.
- **Label / Tag** (Bold 600, 11px / 0.75rem, letter-spacing 0.04em): Category badges, question marks pill tags, and table headers.
- **Digital Timer** (Bold 700, 26px / 1.85rem, Monospace `Consolas`): The central numerical countdown clock (`MM:SS`).

### Named Rules
**The Digital LED Rule.** The countdown timer must always be rendered in a fixed-width, tabular monospace typeface (`Consolas` or `tabular-nums`) enclosed in a high-contrast container. It must never jitter, shrink, or cause layout shifts as digits tick downward.

**The Question Stem Rule.** Question stems are always presented in high-contrast Main Text Ink (`#0f172a`) at a minimum size of 15px with clean 1.5 line height, ensuring effortless reading without squinting.

---

## Layout

Layout architecture balances dense multi-question navigation with a spacious central workspace.

- **Desktop Swing Layout Model:**
  - Standard resolution: 1100 × 720 (resizable, minimum 960 × 640).
  - Top Header Panel (`BorderLayout.NORTH`): Sticky 68px bar housing Candidate Info (West), Digital Timer & Progress Card (Center), and Submit Exam Button (East).
  - Left Question Palette (`BorderLayout.WEST`): 220px fixed-width column featuring a 4-column numerical button grid (`GridLayout(0, 4, 8, 8)`) and legend breakdown.
  - Center Question Workspace (`BorderLayout.CENTER`): Card panel containing Question Stem, Option Radio Group, and Action Bar (`Previous`, `Clear Selection`, `Next`).
- **Web Frontend Layout Model:**
  - Max container width: `1280px` / `1350px`, centered with fluid responsive padding (`1rem` to `2rem`).
  - Top Sticky Navigation: Deep navy bar (`#0f172a`) with branded emblem, role-based navigation tabs, and user profile badge.
  - Dashboard Grid: Responsive CSS Grid (`repeat(auto-fit, minmax(240px, 1fr))`) for analytical KPI cards.
  - Multi-column Assessment Workspace: Question stem container paired with sticky right-hand exam navigation drawer.

---

## Elevation & Depth

**Elevation Philosophy: Lifted Focus**

Surfaces maintain crisp, flat functional integrity at rest, while active focal points—specifically the countdown timer card, the current question workspace, and critical dialogs—utilize elevated soft shadows and distinct borders to guide the candidate's gaze.

### Shadow Vocabulary
- **Subtle Surface** (`box-shadow: 0 1px 2px 0 rgb(0 0 0 / 0.05)`): Applied to standard cards, KPI statistics boxes, and table containers at rest.
- **Lifted Focus** (`box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1)`): Applied to the active question card and digital timer card.
- **Floating Modal** (`box-shadow: 0 20px 25px -5px rgb(0 0 0 / 0.15)`): Applied to confirmation modals, time expiration alerts, and question drawer overlays.

### Named Rules
**The Lifted Focus Rule.** Shadows are strictly structural, never purely decorative. Only elements requiring immediate cognitive priority (the countdown timer, the active question, and modal confirmation prompts) receive elevated shadow depth.

---

## Shapes

- **Corner Radii Hierarchy:**
  - `sm` (`4px` / `6px`): Buttons, input fields, question palette tiles, and table containers.
  - `md` (`6px` / `10px`): Cards, timer containers, and analytical stat boxes.
  - `lg` (`16px`): Modals, scorecards, and hero banners.
  - `pill` (`9999px`): Status badges, question marks chips, and user role avatars.
- **Border Treatments:** Consistent hairline `1px solid #e2e8f0` on all cards and tables. Focus states employ `2px solid #3b82f6` or `3px rgba(59, 130, 246, 0.15)` focus rings.

---

## Components

### Buttons
- **Primary Action Button (`.btn-primary` / `UITheme.createButton`):**
  - Shape: Rounded rectangle (`6px` radius).
  - Background: Electric Blue (`#2563eb`), Text: Pure White (`#ffffff`), Font: Bold 13pt / 0.92rem.
  - Padding: `8px 16px` (Swing) / `0.65rem 1.25rem` (Web).
  - Hover: Darkens to `#1d4ed8` / brighter tint in Swing with smooth transition.
- **Submission / Danger Button (`.btn-danger` / `UITheme.DANGER`):**
  - Background: Rose Red (`#ef4444`), Text: White. Used exclusively for "Submit Exam" and destructive actions.
- **Palette Navigation Buttons:**
  - Shape: Square tile (`36px × 36px` in Swing, `40px × 40px` in Web), `4px` radius.
  - States: Unanswered (Slate border `#cbd5e1`), Answered (Emerald `#10b981`), Flagged (Amber `#f59e0b`), Active (Thick `#2563eb` ring).

### Digital Timer Display
- **Container:** High-contrast card (`#f8fafc` or deep LED panel), bordered with hairline slate.
- **Typography:** `Consolas` / tabular monospace, 26pt bold.
- **Threshold States:**
  - Normal (> 5m remaining): Slate text, emerald progress bar (`#059669`).
  - Warning (< 5m remaining): Amber text (`#d97706`), amber progress bar.
  - Critical (< 1m remaining): Pulsing red text (`#dc2626`), red progress bar with `@keyframes pulseRed`.

### Question Cards & Option Selectors
- **Container:** White background, hairline border (`#e2e8f0`), 16px/20px internal padding.
- **Option Item:** Full-width selectable card row with radio button, option identifier badge (A, B, C, D), and option text.
- **Selection State:** Active option highlights with subtle blue background (`#eff6ff`) and blue border (`#3b82f6`).

### Status Badges & Pills
- **Style:** Compact pill (`padding: 3px 10px`, `font-size: 11px`, bold, uppercase).
- **Variants:**
  - Primary: `#eff6ff` background, `#2563eb` text.
  - Success / Passed: `#ecfdf5` background, `#10b981` text.
  - Warning / Flagged: `#fffbeb` background, `#f59e0b` text.
  - Danger / Failed: `#fef2f2` background, `#ef4444` text.

### Tables & Data Grids
- **Header:** Light slate background (`#f1f5f9`), bold text (`#0f172a`), height 38px, subtle 1px border.
- **Rows:** 36px row height, horizontal hairline dividers, subtle hover highlight (`#fbfcfe`).

---

## Do's and Don'ts

### Do:
- **Do** use monospace typography (`Consolas` or `tabular-nums`) for all countdown timer readouts to eliminate jitter.
- **Do** maintain strict color semantics: Emerald for Answered/Passed, Amber for Flagged/Warning, Rose for Submission/Failed.
- **Do** wrap all Swing timer updates inside `SwingUtilities.invokeLater()` to guarantee Event Dispatch Thread (EDT) safety.
- **Do** provide visual warning alerts as examination countdown timers approach final thresholds.
- **Do** provide keyboard shortcuts (A, B, C, D, Next, Previous) for high-efficiency examination navigation.

### Don't:
- **Don't** allow client-side timer manipulation or drift; countdowns must remain synchronized with server nanosecond deadlines.
- **Don't** introduce distracting gradient animations or decorative clutter into examination taking screens.
- **Don't** use ambiguous grey shades for answered versus unanswered question palette indicators.
- **Don't** omit confirmation dialogs when candidates click "Submit Exam" with unanswered questions remaining.
- **Don't** allow double-submission: all submission buttons must immediately disable upon initial trigger.
