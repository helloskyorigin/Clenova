# Localization

Native app language selection: Android 13+ LocaleManager, API 26–32 stored local preference and recreated activity. Device language clears the override. Native-language names, searchable dropdown, selection check, Android RTL configuration. English default plus 14 translated locale resource sets.

Coverage: 181 resource keys per added language out of 262 default strings. Settings, navigation, key feature labels and important safety/error text are translated. Remaining descriptions, some dialogs, debug fixtures and technical strings fall back to English. Native-speaker and real-device validation remain required; this is not complete localization. No remote translation API or account is needed.

The Android locale config lists all implemented language selections. Audit every new string and all debug fixtures before public release.
