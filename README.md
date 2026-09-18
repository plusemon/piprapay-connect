<div align="center">

# ⚡ PipraPay Connect

### **Automated MFS Gateway Node & Backend Sync Engine for Android**

[![Platform](https://img.shields.io/badge/Platform-Android_7.0+_(API_24--36)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose_M3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM_%2B_Room_%2B_WorkManager-00599C?style=for-the-badge)](https://developer.android.com/topic/architecture)
[![Security](https://img.shields.io/badge/Security-AES--256_GCM_Keystore-059669?style=for-the-badge&logo=shield&logoColor=white)](https://developer.android.com/topic/security/data)
[![Release](https://img.shields.io/badge/Release-v1.0.1-10B981?style=for-the-badge&logo=github)](https://github.com)

<p align="center">
  <b>PipraPay Connect</b> is an enterprise-grade Android gateway node application that automatically listens for incoming <b>bKash</b>, <b>Nagad</b>, <b>Rocket</b>, and <b>Upay</b> Mobile Financial Service (MFS) SMS payment notifications, extracts verified transaction details in real time, and securely syncs them to the <b>PipraPay Merchant Backend Engine</b>.
</p>

[Key Features](#-key-features) •
[Architecture](#-system-architecture) •
[MFS Parser Matrix](#-supported-mfs-providers) •
[Quick Start](#-quick-start--installation) •
[QR Configuration](#-qr-code-pairing-specification) •
[API Specification](#-rest-api-specification) •
[CI/CD Release](#-automated-cicd-pipeline)

---

</div>

## 📖 Overview

In Bangladesh, merchants receive customer payments via Mobile Financial Services (MFS) such as **bKash Merchant/Personal**, **Nagad**, **DBBL Rocket**, and **Upay**. Traditionally, verifying each transaction requires human staff to check SMS alerts on a physical phone.

**PipraPay Connect** transforms any standard Android phone into an automated, non-stop transaction verification terminal:
1. **Instant Capture**: Intercepts incoming payment SMS with priority 999 telephony broadcasts.
2. **Precision Parsing**: Extracts Provider, Transaction ID (`TrxID`), Sender Phone Number, and BDT Amount (`৳`).
3. **Anti-OTP Protection**: Strictly drops verification codes, PIN reset alerts, and sensitive user messages.
4. **Reliable Offline-First Sync**: Stores transactions locally in an encrypted Room SQLite database with retry queues managed by Android WorkManager.
5. **Background Resilience**: Runs a persistent foreground service with ongoing status notifications and one-tap battery optimization bypass.

---

## ✨ Key Features

| Feature | Description |
|:---|:---|
| 🟢 **Live Foreground Listener** | High-priority background service (`FOREGROUND_SERVICE_TYPE_DATA_SYNC`) ensures non-stop SMS listening even when the phone is locked. |
| 🛡️ **Zero-Compromise Security** | Hardware-backed `EncryptedSharedPreferences` with `AES-256 GCM` & `AES-256 SIV` encryption for merchant API keys and device IDs. |
| 🚫 **Anti-Fraud & OTP Guard** | Built-in regex and semantic keyword blacklist rejects 2FA codes, OTPs, PIN resets, and non-transaction messages. |
| ⚡ **Dual Sync Engine** | Instant asynchronous HTTP dispatch upon SMS arrival + persistent **WorkManager** fallback queue with exponential backoff. |
| 📊 **Real-Time Merchant Dashboard** | Bengali Taka (`৳`) metrics card, live service status with animated pulse indicator, latency monitor, and filterable transaction list. |
| 🔍 **Search & Multi-Level Filtering** | Instant search across TrxID, sender number, and amount, plus filter chips for provider (`bKash`, `Nagad`, `Rocket`, `Upay`) and status (`SYNCED`, `PENDING`, `FAILED`). |
| 🧪 **Integrated SMS Simulator** | Built-in developer tool to simulate incoming MFS SMS messages directly inside the app without needing a physical SIM card. |
| 📷 **Rapid QR Onboarding** | Point-and-pair configuration import — scans JSON credentials from the PipraPay merchant panel for instant setup. |
| 🔋 **Battery Optimization Helper** | Built-in detection and direct intent launcher to whitelist the app from OEM battery killers. |

---

## 🏗️ System Architecture

```
                                    +-----------------------------------------+
                                    |         Incoming Cellular SMS           |
                                    +-----------------------------------------+
                                                         |
                                                         v
                                    +-----------------------------------------+
                                    |    SmsBroadcastReceiver (Priority 999)  |
                                    |   - Group multipart PDUs                |
                                    |   - Telephony.Sms.Intents capture       |
                                    +-----------------------------------------+
                                                         |
                                                         v
                                    +-----------------------------------------+
                                    |           MfsSmsParser Engine           |
                                    |   - Anti-OTP Keyword Gatekeeper         |
                                    |   - Provider Signature Matcher          |
                                    |   - TrxID, Sender & Amount Extraction   |
                                    +-----------------------------------------+
                                                         |
                                           +-------------+-------------+
                                           |                           |
                                           v                           v
                      +-----------------------------+       +------------------------+
                      |   Local Room Database       |       |  Heads-up Notification  |
                      |   - Unique TrxID indexing   |       |  "bKash ৳1,500.00      |
                      |   - Status: PENDING         |       |   TrxID: 9ABC123XYZ"   |
                      +-----------------------------+       +------------------------+
                                     |
                                     v
                      +-----------------------------+
                      |  WorkManager / SyncWorker   |
                      |  - Exponential Backoff      |
                      |  - Network Constraint Check |
                      +-----------------------------+
                                     |
                                     v
                      +-----------------------------+
                      |      PipraPay REST API      |
                      |    POST /api/sms/receive    |
                      |  - Device-Key Auth          |
                      |  - Bearer Token Auth        |
                      +-----------------------------+
                                     |
                                     v
                      +-----------------------------+
                      |    Merchant Webhook / POS   |
                      |   Instant Order Fulfillment |
                      +-----------------------------+
```

---

## 📱 Supported MFS Providers

PipraPay Companion comes equipped with tailored parsers for all prominent mobile financial services in Bangladesh:

| Provider | Brand Color | SMS Sender Address | Example Transaction Pattern | Extracted Fields |
|:---|:---:|:---|:---|:---|
| **bKash** | `#E2136E` | `bKash` | `"You have received Tk 1,500.00 from 01712345678. Ref: ... TrxID 9ABC123XYZ"` | Amount: `1500.00`<br>Sender: `01712345678`<br>TrxID: `9ABC123XYZ` |
| **Nagad** | `#F7941D` | `NAGAD` | `"Amount: Tk 2,500.00, Sender: 01812345678, TxnID: 7XYZ456789"` | Amount: `2500.00`<br>Sender: `01812345678`<br>TrxID: `7XYZ456789` |
| **Rocket** | `#8C3494` | `16216` / `Rocket` | `"Cash In Tk 1,200.00 from 01912345678 successful. TxnId: 1029384756"` | Amount: `1200.00`<br>Sender: `01912345678`<br>TrxID: `1029384756` |
| **Upay** | `#002D62` | `Upay` | `"You have received Tk 800.00 from 01612345678. TrxID: UP1234567"` | Amount: `800.00`<br>Sender: `01612345678`<br>TrxID: `UP1234567` |
| **Generic MFS** | `#10B981` | *Any Address* | Fallback regex matching `TrxID / TxnID` + `Tk / BDT` currency amounts | Flexible extraction |

### 🛑 Anti-OTP Security Filter
Incoming messages containing any of the following triggers are **immediately discarded** before parsing:
```
"otp", "verification code", "security code", "pin reset", "password reset",
"do not share", "never share", "login code", "secret code", "temporary code", "authorization code"
```

---

## 🚀 Quick Start & Installation

### 1. Download Pre-built Release APK
1. Navigate to the [Releases](https://github.com) section of this repository.
2. Download the latest `piprapay-companion-v1.0.1.apk`.
3. Transfer or open the APK on your Android device (Android 7.0 / API 24 or newer).
4. Tap **Install** (Allow installation from unknown sources if prompted).

### 2. Grant Device Permissions
On first launch, PipraPay Companion will request the essential permissions:
* **SMS Receive & Read (`android.permission.RECEIVE_SMS`)**: Intercept incoming carrier transaction SMS.
* **Notifications (`android.permission.POST_NOTIFICATIONS`)**: Display foreground keep-alive service status and detected transaction banners.
* **Ignore Battery Optimization**: Prevents Android OEM battery managers (MIUI, ColorOS, OneUI) from stopping the listener.

### 3. Pair with Merchant Account
* **Option A (QR Code / Paste JSON)**: Go to the **QR Setup** tab in the app, click **Paste Clipboard** or scan the JSON configuration from your PipraPay Web Portal.
* **Option B (Manual Setup)**: Open **Settings**, enter your **Server Base URL**, paste your **API Key**, and verify connectivity with the **Test Server Connection** button.

---

## 📷 QR Code Pairing Specification

PipraPay Companion accepts standardized JSON payloads for automated zero-touch pairing:

```json
{
  "server_url": "https://api.piprapay.com/",
  "api_key": "pipra_live_a1b2c3d4e5f6g7h8",
  "device_key": "STORE-DHAKA-POS01"
}
```

### JSON Schema Attributes
| Field | Type | Required | Description |
|:---|:---:|:---:|:---|
| `server_url` | `string` | **Yes** | Fully qualified HTTPS endpoint of the PipraPay server instance. |
| `api_key` | `string` | **Yes** | Secret merchant authentication token (`Bearer` token). |
| `device_key` | `string` | Optional | Custom identifier for the physical device (auto-generated if omitted). |

---

## 🌐 REST API Specification

PipraPay Companion communicates with the merchant server via clean, RESTful JSON payloads over TLS 1.3:

### 1. Synchronize SMS Transaction
```http
POST /api/sms/receive HTTP/1.1
Host: api.piprapay.com
Content-Type: application/json
Accept: application/json
Authorization: Bearer <MERCHANT_API_KEY>
```

#### Request Payload (`SmsSyncRequest`)
```json
{
  "device_key": "DEV-B8F29A01",
  "provider": "BKASH",
  "trx_id": "9ABC123XYZ",
  "sender_number": "01712345678",
  "amount": 1500.00,
  "raw_sms": "You have received Tk 1,500.00 from 01712345678. Ref: Invoice101. Fee Tk 0.00. Balance Tk 45,200.00. TrxID 9ABC123XYZ at 16/09/2026 14:32",
  "timestamp": 1789578720000
}
```

#### Successful Response (`200 OK`)
```json
{
  "success": true,
  "status": "SYNCED",
  "trx_id": "9ABC123XYZ",
  "message": "Acknowledged by PipraPay Engine"
}
```

### 2. Server Ping & Health Check
```http
GET /api/ping HTTP/1.1
Authorization: Bearer <MERCHANT_API_KEY>
```
```http
GET /api/health HTTP/1.1
```
Returns `200 OK` with JSON `{ "status": "healthy" }` to evaluate network latency and endpoint readiness.

---

## 🛠️ Building From Source

### Prerequisites
* **Android Studio Ladybug (2024.2+)** or newer
* **JDK 21** (e.g. Eclipse Temurin 21)
* **Android SDK**: Compile SDK 36 (Android 16), Min SDK 24 (Android 7.0)
* **Gradle**: Uses standard Gradle Kotlin DSL (`build.gradle.kts`)

### Local Build Commands

```bash
# Clone the repository
git clone https://github.com/your-username/piprapay-companion.git
cd piprapay-companion

# Assemble debug APK
./gradlew assembleDebug

# Output APK path:
# app/build/outputs/apk/debug/app-debug.apk

# Run JVM & Robolectric unit tests
./gradlew :app:testDebugUnitTest

# Verify Roborazzi screenshot tests
./gradlew :app:verifyRoborazziDebug
```

---

## 🚢 Automated CI/CD Pipeline

The repository includes a ready-to-run GitHub Actions release workflow configured in [`.github/workflows/release.yml`](.github/workflows/release.yml).

### Triggering a Release via Git Tag

```bash
# Tag the release
git tag -a v1.0.1 -m "Release v1.0.1 - PipraPay Companion"

# Push to GitHub
git push origin v1.0.1
```

### Triggering via GitHub Actions UI
1. Open the repository on GitHub.
2. Navigate to the **Actions** tab.
3. Select **"Build and Publish Release APK"** from the left panel.
4. Click **"Run workflow"**, choose your version tag (e.g., `v1.0.1`), and execute.
5. The workflow will:
   * Compile `./gradlew assembleDebug`
   * Stage `piprapay-companion-v1.0.1.apk`
   * Create a GitHub Release with download assets and auto-generated release notes.

---

## 📂 Project Structure

```
piprapay-companion/
├── .github/
│   └── workflows/
│       └── release.yml          # GitHub Actions APK builder & release pipeline
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml # Permissions, BroadcastReceiver, Foreground Service
│   │   │   ├── java/com/example/
│   │   │   │   ├── PipraPayApplication.kt
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── data/
│   │   │   │   │   ├── api/        # Retrofit & OkHttp client (PipraPayApi)
│   │   │   │   │   ├── db/         # Room Database & TransactionDao
│   │   │   │   │   ├── model/      # TransactionEntity definition
│   │   │   │   │   ├── prefs/      # EncryptedSharedPreferences (AES-256 GCM)
│   │   │   │   │   └── repository/ # Single-source-of-truth TransactionRepository
│   │   │   │   ├── parser/         # MfsSmsParser (bKash, Nagad, Rocket, Upay)
│   │   │   │   ├── receiver/       # SmsBroadcastReceiver (Telephony capture)
│   │   │   │   ├── service/        # PipraPayService (Foreground keep-alive)
│   │   │   │   ├── sync/           # WorkManager SyncWorker (Background sync)
│   │   │   │   └── ui/             # Jetpack Compose Screens, ViewModels & Themes
│   │   │   └── res/                # M3 Styles, Drawables, Mipmap Icons, Strings
│   │   └── test/                   # Robolectric & Roborazzi Screenshot tests
│   └── build.gradle.kts            # App module dependencies & configuration
├── gradle/
│   └── libs.versions.toml          # Gradle Version Catalog
├── RELEASE.md                      # Release workflow and publishing guide
└── README.md                       # Documentation & Project Guide
```

---

## 🔒 Security & Privacy Notice

* **Local Data Encryption**: All merchant API keys and device identifiers are stored in hardware-backed `EncryptedSharedPreferences`.
* **Zero Cloud Third Parties**: Transaction data flows directly from the merchant device to the merchant server; no third-party telemetry, ads, or middleman analytics are included.
* **Minimal Permission Scope**: Only permissions directly necessary for telephony SMS capture, network transmission, and foreground persistence are requested.

---

## 📄 License

Distributed under the **Apache License 2.0**. See `LICENSE` for more information.

<div align="center">
  <sub>Built with ❤️ for Bangladesh's Merchant & E-Commerce Ecosystem. Powered by Jetpack Compose & Kotlin.</sub>
</div>
