# Localization

The Settings language menu uses native names, search, a check mark, and immediate application. Android 13+ uses LocaleManager and Android system app language settings. API 26–32 stores a local preference and recreates the activity with localized resources. Device language clears the override. RTL uses Android configuration and auto-mirrored directional controls.

English is the default resource set. Additional languages are listed in res/xml/locales_config.xml. Missing translations fall back to English; localization coverage and native-speaker review must be checked before public release. Do not claim complete translation until every visible string, including debug fixtures, and every newly added screen is audited.

No network translation service or user account is required.
