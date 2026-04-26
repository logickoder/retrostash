# Retrostash Design System

Single source of truth for tokens shared between the Compose Multiplatform sample ([composeApp](composeApp/)) and the React landing site ([landing/](landing/)). Both consumers must reference these hex values verbatim — when a value changes here, sync [Theme.kt](composeApp/src/commonMain/kotlin/dev/logickoder/retrostash/example/presentation/Theme.kt) and [landing/src/styles.css](landing/src/styles.css) in the same commit.

Default theme: **dark**. Background `#1C1B1F` is shared by the wasmJs frame and the React landing so navigating between `/retrostash/`, `/retrostash/playground/`, and `/retrostash/api/` shows no white flash.

## Color tokens

Material 3 baseline seeded at `primary = #6750A4`.

| Token | Light | Dark | Usage |
|---|---|---|---|
| `primary` | `#6750A4` | `#D0BCFF` | CTAs, links, selected chips, active tabs |
| `onPrimary` | `#FFFFFF` | `#381E72` | Text/icons on `primary` |
| `primaryContainer` | `#EADDFF` | `#4F378B` | Result card bg (cache hit highlight) |
| `onPrimaryContainer` | `#21005D` | `#EADDFF` | Text on `primaryContainer` |
| `secondary` | `#625B71` | `#CCC2DC` | Secondary surfaces, label color |
| `onSecondary` | `#FFFFFF` | `#332D41` | Text on `secondary` |
| `secondaryContainer` | `#E8DEF8` | `#4A4458` | Event log card bg, transport tab strip |
| `onSecondaryContainer` | `#1D192B` | `#E8DEF8` | Text on `secondaryContainer` |
| `tertiary` | `#7D5260` | `#EFB8C8` | Cache-hit accent (`Bolt` icon) |
| `error` | `#B3261E` | `#F2B8B5` | Failed-request log rows |
| `surface` | `#FEF7FF` | `#1C1B1F` | Scaffold bg, page bg |
| `onSurface` | `#1C1B1F` | `#E6E1E5` | Body text |
| `surfaceVariant` | `#E7E0EC` | `#49454F` | Dividers, disabled controls |
| `onSurfaceVariant` | `#49454F` | `#CAC4D0` | Hint/label text |
| `outline` | `#79747E` | `#938F99` | Borders, dividers |

## Typography

| Role | Family | Weight | Compose token |
|---|---|---|---|
| Title | system sans-serif | SemiBold (600) | `MaterialTheme.typography.titleMedium` |
| Body | system sans-serif | Regular (400) | `MaterialTheme.typography.bodyMedium` |
| Label large | system sans-serif | Medium (500) | `MaterialTheme.typography.labelLarge` |
| Label small | system sans-serif | Regular (400) | `MaterialTheme.typography.labelSmall` |
| Code (landing) | `ui-monospace, SFMono-Regular, Consolas, "Liberation Mono", monospace` | Regular | n/a |

## Shape / spacing

| Token | Value | Usage |
|---|---|---|
| `radius-card` | `16dp` / `16px` | All `Card` / `Surface` containers |
| `radius-chip` | `8dp` / `8px` | Chips, segmented buttons, buttons |
| `spacing-x` | `16dp` / `16px` | Horizontal screen padding |
| `spacing-y` | `12dp` / `12px` | Vertical card-to-card spacing |
| `spacing-inner` | `8dp` / `8px` | Default gap inside rows |

## Token mapping

### Compose ([Theme.kt](composeApp/src/commonMain/kotlin/dev/logickoder/retrostash/example/presentation/Theme.kt))

`darkColorScheme(primary = ..., onPrimary = ..., ...)` — pass each token explicitly. Don't rely on Material 3 defaults; they drift. Light scheme follows the same shape with the Light column values.

### Tailwind ([landing/src/styles.css](landing/src/styles.css))

Tailwind v4 `@theme` block exposes tokens as CSS variables. Components consume them as utility classes (`bg-surface`, `text-on-surface`, `bg-primary text-on-primary`, etc.):

```css
@import "tailwindcss";

@theme {
  --color-primary: #D0BCFF;
  --color-on-primary: #381E72;
  --color-primary-container: #4F378B;
  --color-on-primary-container: #EADDFF;
  --color-secondary: #CCC2DC;
  --color-on-secondary: #332D41;
  --color-secondary-container: #4A4458;
  --color-on-secondary-container: #E8DEF8;
  --color-tertiary: #EFB8C8;
  --color-error: #F2B8B5;
  --color-surface: #1C1B1F;
  --color-on-surface: #E6E1E5;
  --color-surface-variant: #49454F;
  --color-on-surface-variant: #CAC4D0;
  --color-outline: #938F99;

  --radius-card: 16px;
  --radius-chip: 8px;
}
```

Light mode tokens deferred (dark-only landing for now).

## When updating

1. Edit this file first.
2. Sync [Theme.kt](composeApp/src/commonMain/kotlin/dev/logickoder/retrostash/example/presentation/Theme.kt) — the `darkColorScheme(...)` call.
3. Sync [landing/src/styles.css](landing/src/styles.css) — the `@theme` block.
4. Run both sample app and landing locally, eyeball that the surface bg matches at the `/retrostash/` ↔ `/retrostash/playground/` boundary.
