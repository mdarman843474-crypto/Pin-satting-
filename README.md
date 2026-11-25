# PIN Settings

Original Settings jaisi list. Har row asli Android Settings page kholti hai, isliye wahan ke saare toggle asli kaam karte hain.
Upar se PIN lock aur Device Owner protection.

## Isme kya hai
- Wi-Fi, Bluetooth, Mobile networks, Portable hotspot
- Fingerprints / face data / screen lock, Safety and emergency, Privacy protection, Apps
- Original Settings: Original Settings lock, Max session time
- Protection: Device owner status, Uninstall protection, Factory reset lock, App control lock, Change PIN, Remove protection

## APK banana
**GitHub se (PC ke bina)**
1. Is folder ki saari files ek naye GitHub repo me upload karo.
2. `.github/workflows/build.yml` honi chahiye. Na ho to `BUILD_WORKFLOW.yml` ka content us path par paste karo.
3. Actions tab me "Build APK" chalao (Run workflow).
4. Khatam hone par Artifacts me `PinSettings-apk` download karo. Zip kholkar `app-debug.apk` install karo.

**Android Studio se:** project kholo, Build, Build APK(s).

## Device owner (ek baar)
1. Phone se Google / Mi account kuch der ke liye hatao.
2. APK install karo, app kholo, PIN set karo.
3. PC ya dusre phone se chalao:
   `adb shell dpm set-device-owner com.pinsettings.app/.AdminReceiver`
4. App kholo, Protection ke toggle ON karo.

Xiaomi par `MANAGE_DEVICE_ADMINS` wala error aaye to Developer options me "USB debugging (Security settings)" ON karo.

## Original Settings lock
- "Original Settings lock" ON karo: phone ki asli Settings band ho jati hai (pehle suspend, na ho to hide).
- Lock ON ho to sirf ye 8 rows khulti hain. Row dabane par Settings khulti hai aur Home / Recents / notification shade band ho jate hain.
- Back dabake app me wapas aate hi Home wapas aa jata hai aur Settings turant dobara lock ho jati hai.
- Power menu khula rehta hai. Restart karne par bhi Settings dobara lock ho jati hai.
- "Max session time" sirf safety timer hai: itni der baad Settings apne aap band.
- Agar koi row ka page phone par nahi khulta to poori Settings nahi khulti, message aata hai. Wo row ka naam bhej do.
- Baaki Settings chahiye to is app me lock OFF karo, kaam karo, phir ON karo.
- Phone ke apne App lock se Settings lock mat karo. Wo lock koi app bypass nahi kar sakti, har page par password maangega. Sirf is app ka lock use karo.
- Lock ON na ho paye ya Home band na ho to app message dikhati hai. Wo bhej do.
- App hi na khule aur Settings hide hui ho to: `adb shell pm unhide com.android.settings`

## Dhyan rakho
- PIN yaad rakho. Bhool gaye to protection sirf factory reset (Recovery) se hategi.
- Factory reset lock sirf Settings wale reset ko rokta hai. Recovery (Power + Volume Up) se reset ho sakta hai.
- Wi-Fi, Bluetooth, hotspot ke toggle Android third-party app ko khud karne nahi deta. Isliye app asli Settings page kholti hai.
- Settings page ke andar ke related pages khul sakte hain (jaise Wi-Fi ke andar ke options). Home aur Recents band rehte hain.
- Xiaomi ki Security app jaisi dusri apps lock nahi hoti. Clear data se bachne ke liye "App control lock" bhi ON rakho.
- Kuch phones par kuch rows (jaise Safety and emergency) seedha page nahi kholti, tab main Settings khulti hai.
