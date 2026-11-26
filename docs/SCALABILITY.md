# CLENOVA — Scale and reliability contract

These are foundation decisions and future release gates, not measured capacity.

## Local-first architecture
- Keep personal media and storage analysis on-device; no central AI service for core cleaning.
- Share process-scoped repositories. Use lifecycle-aware state and asynchronous disk/ML work.
- Keep feature-organized single module until build or team needs justify splitting.
- Future scanner: MediaStore, indexed Room metadata, bounded batches and persisted progress. Stream hashes after size grouping; never load entire galleries or full-size images into memory. Cache fingerprints keyed by media version, invalidate on change/revocation, scan incrementally. Use paging and bounded parallelism.
- Future monitoring: opt-in unique WorkManager jobs, cancellable scans and battery-aware constraints. No promise of exact background timing.

## Agent safety
- Agent observes and prepares; destructive execution requires explicit user approval and applicable Android confirmation.
- Separate plan and execution. Bind approval to exact media IDs, operations and protection revision; revalidate identity, permissions and Memory Guard before acting. Changed or protected items fail closed.
- Record per-item results locally; retry only unfinished, still-authorized operations. Report verified savings, not estimates as results.
- No remote upload of personal media, filenames or hashes by default. Allow pause and preference reset.

## Release gates (pending)
- Build, unit tests, lint and real-device testing.
- API 26, 30, 33/34 and current Android; denied, partial and revoked grants.
- Synthetic 10k/50k/100k-media libraries, large videos, low disk, cancellation, process death and concurrent requests.
- Measure startup, scan duration, peak memory, frame rendering, battery and crash/ANR rates on budget and midrange devices.
- Verify zero unapproved deletion and protected-item removal; stage Play rollouts and halt on regression.
- Any future backend requires separate load tests, quotas and cost monitoring.

Ten million users is an adoption target. On-device architecture spreads work across devices, but performance and reliability must be measured before claiming scale.
