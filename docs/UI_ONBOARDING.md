# CLENOVA UI sprint 02 — launch and introductions

The three original introduction destinations, headings, CTA labels and permission handoff are retained. Presentation is now native views, not full-size Canvas storyboards. No scan, deletion, permissions grant or Agent activation is simulated or triggered by a story.

- Launch: existing supplied logo and Skyorigin credit; 420ms application splash dwell (system launch screen time is separate). Completed onboarding is not reopened if activity recreation happens during splash.
- Story 1: example Agent conversation and review scope.
- Story 2: four review categories, visible immediately.
- Story 3: Discover / Review / Approve; no automatic deletion.
- Foreground presentation clocks: 5s, 5s, 6s. 16s total when auto-advance is enabled and uninterrupted. Continue is always available. Clock pauses on touch, loss of focus or backgrounding. Reduced motion, font scale above 1.15, or content taller than the story viewport prevents automatic advancement. The fixed footer CTA is outside the scrollable story body.
- Theme: shared palette and typography; dark/light stories respect appearance. Branded splash retains approved light artwork; permission and Agent setup screens are intentionally deferred to phase 03.
- Native row text remains visible throughout. The clock only highlights explanatory steps; no generated file counts, fake storage savings or blank opening frame.
- Smoke tests assert headings, progress labels, immediately clickable CTA and native story text. Policy tests cover timing, reduced motion, compact sizing and safe progress values.

Physical-device visual testing, short-screen and large-font runtime testing remain required. No changes to scan, deletion, permission or AI engines.
