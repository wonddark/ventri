# App Icon Design — Ventri

**Date:** 2026-04-21
**Status:** Approved

## Goal

Replace the existing app icon (countdown arc + jar) with a new design built from scratch: a stopwatch-style circular progress ring with a 7-segment digital display showing "1d" in the center.

---

## Design Decisions

| Dimension | Decision |
|---|---|
| Concept | Stopwatch — a circular countdown timer showing time remaining |
| Background | Cream `#faf7f3` (matches app light theme background) |
| Ring fill | 75% clockwise from 12 o'clock |
| Ring color | Terracotta `#b5613a` (primary accent) |
| Ring track | Warm muted `#ede5d8` (remaining 25%) |
| Center label | "1d" in 7-segment display style |
| Label active color | Terracotta `#b5613a` |
| Label ghost color | `#e0d5c8` (inactive segments, faintly visible) |
| Crown | Small stopwatch button at top — two stacked rounded rects |
| Linecaps | Round on both ring arcs |
| Wordmark | To be derived from this icon shape in a follow-up |

---

## SVG Geometry (512 × 512 viewBox)

### Background
```
<rect width="512" height="512" rx="112" fill="#faf7f3"/>
```

### Crown (stopwatch button, top center)
```
<rect x="232" y="22" width="48" height="20" rx="8" fill="#e0d8ce"/>
<rect x="244" y="14" width="24" height="18" rx="6" fill="#e8e0d6"/>
```

### Ring
- Center: (256, 256)
- Radius: 200
- Stroke width: 44
- Linecap: round

```
<!-- Track: 9 o'clock → 12 o'clock (25%, 90°) -->
<path d="M 56 256 A 200 200 0 0 1 256 56"
      fill="none" stroke="#ede5d8" stroke-width="44" stroke-linecap="round"/>

<!-- Fill: 12 o'clock → 9 o'clock clockwise (75%, 270°) -->
<path d="M 256 56 A 200 200 0 1 1 56 256"
      fill="none" stroke="#b5613a" stroke-width="44" stroke-linecap="round"/>
```

### 7-Segment Display — "1d"

Character cell: W=68, H=116, T=14 (segment thickness), G=2 (gap between segments)

Display centered at (256, 256):
- Total display width: 68 + 16 (gap) + 68 = 152px
- Character `"1"` origin: (180, 198)
- Character `"d"` origin: (264, 198)  ← 180 + 68 + 16

**Segment geometry** (hexagonal ends, relative to character origin):

| Segment | sx | sy | sw | sh | Type |
|---|---|---|---|---|---|
| a (top) | 16 | 0 | 36 | 14 | horizontal |
| f (top-left) | 0 | 16 | 14 | 33 | vertical |
| b (top-right) | 54 | 16 | 14 | 33 | vertical |
| g (middle) | 16 | 51 | 36 | 14 | horizontal |
| e (bot-left) | 0 | 67 | 14 | 33 | vertical |
| c (bot-right) | 54 | 67 | 14 | 33 | vertical |
| d (bottom) | 16 | 102 | 36 | 14 | horizontal |

**Hexagon polygon formula** (half = 7 for both types, T=14):

Horizontal segment at (sx, sy, sw, sh):
```
(sx+7, sy)  (sx+sw-7, sy)  (sx+sw, sy+7)
(sx+sw-7, sy+sh)  (sx+7, sy+sh)  (sx, sy+7)
```

Vertical segment at (sx, sy, sw, sh):
```
(sx+7, sy)  (sx+sw, sy+7)  (sx+sw, sy+sh-7)
(sx+7, sy+sh)  (sx, sy+sh-7)  (sx, sy+7)
```

Add the character origin offset to get absolute SVG coordinates.

**Ghost segments**: at ≤48px render only the active segments (drop ghost polygons) to keep the icon readable.

**Active segments:**
- `"1"` → b, c
- `"d"` → b, c, d, e, g

**Colors:**
- Active: `#b5613a`
- Inactive (ghost): `#e0d5c8`

---

## Android Adaptive Icon

The design maps to two XML files:

- `ic_launcher_background.xml` — solid cream `#faf7f3`
- `ic_launcher_foreground.xml` — all vector paths (crown, ring arcs, 7-segment polygons) on transparent background, 108dp × 108dp, viewportWidth/Height = 512

The existing `ic_launcher.xml` and `ic_launcher_round.xml` reference these two and require no changes.

---

## Size Behaviour

| Size | What reads |
|---|---|
| 512px | Full detail — crown, ring, ghost segments, "1d" label |
| 128px | Ring + label clearly visible, crown subtle |
| 72px | Ring + terracotta segments readable |
| 48px | Ring arc dominant, active segments visible |
| 24px | Ring arc only — terracotta quarter gap against cream |

---

## Out of Scope

- Wordmark / logotype (follow-up task)
- Notification icon (separate, monochrome treatment)
- Dark mode adaptive icon variant
