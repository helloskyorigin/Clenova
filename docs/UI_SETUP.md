# CLENOVA UI sprint 03 — Access, Agent activation and device preparation

- Shared SetupLayout: scrollable body, fixed footer, adaptive gutters, light/dark palette and readable native typography. No oversized illustration or continuous decorative pulse.
- Permission cards explain each access, progress through four access decisions, show the previous observed result and expose Android errors. Existing Android request codes, result checks and skip rules are preserved. CTA is disabled only while an OS request is pending, not while a cosmetic transition runs.
- Agent activation shows the saved confirmation only after saveAgentSetup succeeds. Manual mode still requires a saved preference before proceeding. Existing setup-from-tab routing is preserved. No simulated Agent action.
- Preparation progress derives from five completed DeviceOverview stages. Storage and counts are actual available metadata. A completed stage with limited/unavailable data receives a neutral marker rather than a green success check. Retry is an explicit fixed-footer action on failure.
- Completion dwell reduced from 850 to 420ms, only after actual preparation completes. The existing final onboarding persistence and Home handoff remain intact. Native animation honors reduced motion.
- CI checks PreparationPolicy, earlier regressions and onboarding smoke assertions. Full visual checks on real devices, large fonts and short screens remain required.

No permissions, file removal, scan engines, AI backend or automatic deletion behavior changed.
