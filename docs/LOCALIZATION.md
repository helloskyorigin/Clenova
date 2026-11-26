# Localization

Native Android app-language integration, with a persistent legacy locale override on API 26–32. The selector lists 15 languages and a device-language option. All 279 translatable resource keys are present in every additional language (3906 localized entries); six invariant resources are shared. Privacy & Control dialogs and reset confirmations are localized. Run tools/check_localization.py for regression checks. Resource coverage is not native-speaker or device verification; APK compilation and runtime testing remain required.
