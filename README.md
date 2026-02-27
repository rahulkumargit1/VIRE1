# PayOffline v2.0.0

**Offline UPI payments using USSD (*99#). No internet required.**

---

## What's Fixed in v2.0.0

### 🔴 Bug Fixes
| Issue | Fix |
|-------|-----|
| "Request Failed / Network returned failure" | Proper error classification with carrier-specific messages + auto Dialer fallback |
| CALL_PHONE permission blocked | Added `ACTION_DIAL` fallback — works with ZERO permissions |
| No SIM detected | Dual-SIM aware using SubscriptionManager with per-slot TelephonyManager |
| Request timeout (no response) | 30-second timeout guard with user-friendly message |
| App would crash without permissions | Graceful degradation to dialer method |

---

## Features

### Core
- ✅ Send Money via USSD `*99*1*recipient*amount*pin#`
- ✅ Check Balance via `*99*5*pin#`
- ✅ Mini Statement via `*99*3*pin#`
- ✅ Link Bank Account via `*99*2*pin#`
- ✅ Dual SIM support with SIM selector

### USSD Method Strategy
1. **Primary:** `TelephonyManager.sendUssdRequest()` (programmatic, requires CALL_PHONE)
2. **Fallback:** `Intent.ACTION_DIAL` with USSD pre-filled (no permission needed — user taps call)

The fallback triggers **automatically** when:
- CALL_PHONE permission is denied
- Carrier returns network failure
- USSD service unavailable

### UI / UX
- Material Design 3 with Indigo brand palette
- Animated transitions between tabs
- Dark mode support (toggle in Settings)
- Haptic feedback toggle
- PIN visibility toggle (show/hide)
- Saved recipients quick-select (last 5)
- Error banners with actionable "Try via Dialer" button
- Professional app icon (custom ₹ symbol)

### Security
- Biometric app lock (fingerprint / face unlock)
- UPI PIN never stored — cleared on exit
- No analytics, no tracking, no internet

### History & Data
- Transaction history (up to 200 records) persisted locally
- Filter by type (Send, Balance, Statement, Link)
- Filter by status (Success, Failed)
- Statistics row (total, success count, fail count)
- Export history as CSV and share
- Clear history with confirmation

---

## Project Structure

```
app/src/main/java/com/example/payoffline/
├── MainActivity.kt                    # Entry point, nav, biometric auth
├── data/
│   ├── model/Models.kt                # Data classes, enums
│   └── repository/
│       ├── UssdRepository.kt          # USSD + dialer logic
│       ├── HistoryRepository.kt       # Persistence + CSV export
│       └── SettingsRepository.kt      # Settings persistence
├── viewmodel/UssdViewModel.kt         # All state management
└── ui/
    ├── theme/Color.kt / Theme.kt / Typography.kt
    ├── components/Components.kt        # Shared reusable components
    └── screens/
        ├── PayScreen.kt
        ├── BalanceScreen.kt
        ├── HistoryScreen.kt
        └── SettingsScreen.kt
```

---

## How to Build

### Requirements
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34
- Min device: Android 8.0 (API 26)

### Steps
```bash
# Clone / extract project
cd PayOffline

# Build debug APK
./gradlew assembleDebug

# APK output: app/build/outputs/apk/debug/app-debug.apk

# Install directly
./gradlew installDebug
```

---

## Permissions

| Permission | Required? | Why |
|---|---|---|
| `CALL_PHONE` | Optional (highly recommended) | Enables programmatic USSD requests. Without it, Dialer fallback is used. |
| `READ_PHONE_STATE` | Recommended | Detect available SIM cards |
| `READ_PHONE_NUMBERS` | Optional | Show SIM phone numbers in selector |
| `USE_BIOMETRIC` | Optional | App lock feature |
| `VIBRATE` | Optional | Haptic feedback |
| `READ_CONTACTS` | Optional | Future: recipient search |

---

## Carrier Compatibility

The `*99#` USSD service is provided by NPCI and supported by all major Indian carriers:
Airtel, Jio, Vi (Vodafone Idea), BSNL, MTNL

If your carrier returns a failure, use the **Dialer fallback** which works on 100% of devices.

---

## Architecture

- **MVVM** with AndroidViewModel
- **Kotlin Coroutines** for async USSD calls
- **Jetpack Compose** + Material3 for UI
- **SharedPreferences + Gson** for lightweight local persistence
- **StateFlow** for reactive UI updates
- No network calls, no external dependencies beyond standard AndroidX

---

*PayOffline — Built with ♥ in India*
