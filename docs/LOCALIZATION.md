# Localization

Native app language selection: Android 13+ LocaleManager, API 26–32 stored local preference and recreated activity. Device language clears the override. Native-language names, searchable dropdown, selection check, Android RTL configuration. English default plus 14 translated locale resource sets.

Static coverage: 262 of 262 translatable resource keys in every additional locale (3668 localized entries). 6 brand/unit/format resources are intentionally shared and marked non-translatable. Support email subjects and diagnostic labels now use localized resources. All resource key sets and indexed printf placeholders were compared to the English source. This is resource coverage, not evidence of native-speaker quality or real-device RTL behavior. Native-speaker review, APK compilation, and device validation remain required. File names, brand names, and technical units are language-independent. No remote translation API or account is needed.

Run the localization audit before adding a language or releasing an update.
