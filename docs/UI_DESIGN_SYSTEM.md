# CLENOVA / UI foundation (refinement sprint 01)

The existing native Java UI and white/electric-blue identity remain the baseline. This pass standardizes shared primitives rather than replacing screens or their actions.

## Tokens
- Brand electric blue #007AFF; readable light accent #0064D2, dark accent #409CFF; white-on-primary #0060CE.
- Light background #F5F5F7 and white surfaces; dark background #141416 and surface #242426.
- Text, muted, success, danger, divider and border colors are centralized in UiPolish.Palette.
- 20dp shared card radius, 24dp action radius, 16dp horizontal and 14dp vertical card padding. Adaptive outer gutter 16dp (<360dp), 18dp (360–599dp), 24dp (600dp+).
- 48dp tool header touch targets; 22dp line icons. Back, history, refresh, undo and info use native vector-style drawables; existing action guards are preserved.
- Native system sans, semantic weights 400/500/600/700; SP remains user-scaled. Shared dark/light ripple feedback. Motion tokens 140/220ms are available for subsequent screen passes; existing onboarding timing is not changed.

## Implementation
UiPolish is platform-independent and owns palette/geometry/contrast policy. UiKit owns native typography, shape, surface, ripple and header-icon factories. MainActivity and seven standalone tool activities use these helpers. Specialized visualizations and individual controls retain their current behavior; the next phases redesign them deliberately.

## Acceptance
Unit tests assert core palette contrast on light/dark background and card, filled CTA contrast, theme selection, adaptive gutters and icon targets. Existing feature regression and smoke tests remain enabled. Physical-device visual validation, large-font checks, onboarding layout and timing, and screen-specific polish remain subsequent work. No billing, cleaning, storage, agent or permission logic was changed.
