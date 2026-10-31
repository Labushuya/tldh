# tl;dh brand

**Long story, short.** A long, noisy voice note compresses into a short, usable signal. The full design system (colors, type, spacing, glass, components) is specified in [DESIGN.md](../../DESIGN.md); this folder holds the assets.

## Assets

| File | Use |
|---|---|
| `banner.png` | README banner, 2560 × 1280 (2× of 1280 × 640) |
| `banner-source.html` | Source of the banner; render at 1280 × 640 with device scale 2 |
| `icon.svg` | App icon on the 108 dp adaptive grid (72 dp visible area) |
| `icon-512.png` | App icon, 512 px, e.g. for store listings |

The launcher icon in the app is `app/src/main/res/drawable/ic_launcher_foreground.xml` on a `#A50B5E` background, with the same glyph as the Android 13 monochrome (themed) layer.

## The mark

Three bars getting shorter (3 : 2 : 1) and a dot: the voice note ends in a point. Bars are 7 dp wide with fully rounded ends; the dot has a 4 dp radius. On the 108 dp grid the glyph spans x 32–76 and y 38–70, centered at 54 and inside the 66 dp safe zone, so every launcher mask (circle, squircle, rounded square, teardrop) keeps it intact.

## Wordmark

`tl;dh` set in Bricolage Grotesque Bold with tight tracking (-0.01 to -0.035 em by size). The semicolon is `#D72B84` on dark backgrounds and `#A50B5E` on light ones. Never outline, gradient-fill or add a glow to the wordmark.

## Fonts

Bricolage Grotesque (The Bricolage Grotesque Project Authors) is licensed under the SIL Open Font License 1.1, see [licenses/BricolageGrotesque-OFL.txt](../../licenses/BricolageGrotesque-OFL.txt). The app bundles static Medium and Bold instances (optical size 36); the banner uses the 96 pt Bold and 48 pt Medium instances of the variable font from [google/fonts](https://github.com/google/fonts/tree/main/ofl/bricolagegrotesque).
