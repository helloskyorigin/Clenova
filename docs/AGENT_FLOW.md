# Agent lifecycle

- Onboarding remains hidden. Its future "Set up Agent" action should navigate to the existing ClenovaRoute.AutopilotSetup; "Not now" leaves the configuration untouched and opens Home.
- Agent tab and Home use the same setup route. Configuration is persisted by PreferencesRepository.setAutopilotPreferences; the Agent tab observes UserPreferences rather than maintaining its own setup flag.
- Configured and enabled are separate; pause cancels scheduled work and keeps the workspace. Resume schedules a first check and periodic checks. Reset clears configuration and reports.
- WorkManager scans only accessible media, on device, without deleting anything. System scheduling may be delayed. Device checks save aggregate data, not media names or paths.
- Review opens a fresh scan and the existing per-tool Android approval flow. Cached summaries are estimates, not permissions or a deletion plan. History links to actual cleanup history, not a fabricated agent log.
- No chatbot, model API, notification delivery or autonomous deletion is included. Storage goal is displayed against measured free space; adaptive prioritization is a later improvement.
