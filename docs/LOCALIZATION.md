# Localization

Native Android app-language integration, with a persistent legacy locale override on API 26–32. The selector lists 15 languages and a device-language option. All 310 translatable resource keys are present in every additional language (4340 localized entries); six invariant resources are shared. Settings and Clean workspace are localized. Third-party license texts retain their original wording. Run tools/check_localization.py for regression checks. Resource coverage is not native-speaker or device verification; APK compilation and runtime testing remain required.
