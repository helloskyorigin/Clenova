# Agent lifecycle

- Onboarding remains hidden. Its future "Set up Agent" action should navigate to the existing ClenovaRoute.AutopilotSetup; "Not now" leaves the configuration untouched and opens Home.
- Agent tab and Home use the same setup route. Configuration is persisted by PreferencesRepository.setAutopilotPreferences; the Agent tab observes UserPreferences rather than maintaining its own setup flag.
- Configured and enabled are separate; pause cancels scheduled work and keeps the workspace. Resume schedules a first check and periodic checks. Reset clears configuration and reports.
- WorkManager scans only accessible media, on device, without deleting anything. System scheduling may be delayed. Device checks save aggregate data, not media names or paths.
- Review opens a dedicated category plan. A full-permission background snapshot may be reused once within 90 seconds in the same process, with current protection rules applied. Otherwise a real scan is performed. Per-tool Android approval and live file checks remain mandatory. Review invalidates after system cleanup callbacks and protection changes. Cached summaries are estimates, not permissions. History links to actual cleanup history.
- No chatbot, model API, notification delivery or autonomous deletion is included. Storage goal is displayed against measured free space; adaptive prioritization is a later improvement.
