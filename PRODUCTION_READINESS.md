# CLENOVA — Production readiness handoff

## Source checkpoint

Phase 53–56 features remain integrated. Phase 57 removes retired sample cleanup UI, preserves the real media approval receipt during activity recreation, updates in-app help/terms, clears outdated Autopilot notifications and hardens the optional private relay. Historical preview journal entries remain explicitly labeled. Existing navigation and approved UI remain the baseline.

The app is still a development build, not a published production release. The private relay is not a public multi-user service.

## Release gates (not implied by a successful build)

- Verify real media review and Android Trash approval/cancellation on physical devices, including partial media grants, permission revocation, rotations during approval and verification, and changed/protected files. Verify originals are retained.
- Verify document removal, contacts operations, uninstall, photo/video compression and history with disposable fixtures; never use irreplaceable personal files. Verify cancellation and low-storage behavior.
- Verify Autopilot charging/battery constraints, reboot, pause/resume and notification delivery on real devices. Scheduled discovery must remain read-only.
- Verify all screens in light/dark modes, compact layouts, large font sizes, TalkBack and reduced-motion settings. Complete any remaining translations.
- Review current Android lint warnings and outstanding emulator failures; retain evidence of actual results for the release candidate.
- Configure a real HTTPS relay only if cloud chat is to ship. Keep provider keys server-side; replace shared developer authentication with per-user/device authorization, rate limits, quotas and abuse controls before public use. Test with actual Groq credentials and review provider data processing.
- Confirm release identity, version, support contact, public listing URL, signing, supported Android versions, current Play requirements, privacy/data-safety disclosures and production terms. No subscription, payment collection or paid entitlement is enabled.
- Keep recovery claims grounded in observed storage; Android Trash is not necessarily immediate space recovery.

User requested source fixes and implementation proof rather than APK packaging. Device verification, signing and store publication are separate release work, not marked complete here.

## Automated evidence

CI includes pure Java policy/regression tests, mocked private relay tests, Android lint and emulator smoke scripts. Consult the exact commit's workflow results. The background-job smoke test checks a real scheduled local scan summary and permission revocation. It does not prove physical-device behavior or media deletion success.
