
# BLE SPAM 📡 — Next-Gen Spoofing Toolkit

[**Русский**](assets/README_RU.md)

[![Platform](https://img.shields.io/badge/Android-8.0%2B-brightgreen)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Telegram](https://img.shields.io/badge/Join-Telegram%20Channel-0088cc)](https://t.me/blespam)

<p align="center">
  <img src="./assets/logo.gif" width="200" alt="BLE Spammer Logo">
</p>

---

## 🚀 Overview
**BLE SPAM** is an advanced toolkit for testing Bluetooth Low Energy (BLE) protocols and simulating advertising packets. The project is based on cutting-edge research in mobile security and is optimized for modern Android devices.

**Based on the works of:**
- [Willy-JL](https://github.com/Willy-JL)
- [Spooks4576](https://github.com/Spooks4576) 
- [ECTO-1A](https://github.com/ECTO-1A)

### Supported Platforms (Target):
<img src="https://img.shields.io/badge/iOS-17+-000000?style=flat&logo=apple" alt="iOS"> <img src="https://img.shields.io/badge/Android-8.0+-3DDC84?style=flat&logo=android" alt="Android"> <img src="https://img.shields.io/badge/Windows-10+-0078D6?style=flat&logo=windows" alt="Windows">

---

## 🔥 Core Features

### Protocol Matrix
| Protocol                | Target OS          | Impact Level        |
|-------------------------|--------------------|--------------------|
| 🍏 **Apple Continuity** | iOS/iPadOS 17+     | System Reboot 💥    |
| 🤖 **Google Fast Pair** | Android 8.0+        | Persistent Spam 📈 |
| 📲 **Samsung EasySetup**| Android 10+        | UI Freeze 🛑        |
| 💻 **Microsoft Swift** | Windows 10/11      | Custom Pairing ✨    |

### Key Capabilities
* **195+ Presets:** Ready-to-use profiles for popular devices (AirPods, Pixel Buds, Galaxy Watch, etc.).
* **Timing Control:** Precise adjustment of advertising intervals (from 20ms to 2000ms).
* **Multi-Chaining:** Ability to run multiple protocols simultaneously.
* **Native Security:** Protection core and API endpoints are implemented in C++ (JNI) to prevent easy reverse engineering.
* **Crash Analytics:** Module for tracking attack effectiveness.

---

## 🛠 Setup & Self-Hosting

To ensure project security, all API keys and private endpoints have been removed from the public source code. To build a functional app, follow these steps:

### 1. Firebase Configuration
Place your `google-services.json` in the `app/` folder. Template:
```json
{
  "project_info": { "project_id": "your-id" },
  "client": [ { "api_key": [ { "current_key": "YOUR_KEY" } ] } ]
}

```

### 2. Native Layer Setup (C++)

Edit `app/src/main/cpp/native-lib.cpp`:

1. Insert your XOR key into variables `K1-K6`.
2. Add your encrypted HEX bytes for API addresses into arrays `D1-D7`.
3. Update JNI function paths to match your `package_name`.

### 3. Compilation

```bash
./gradlew assembleRelease

```

---

## ⚠️ Disclaimer

This tool is intended for **educational purposes** and authorized security auditing only. The author is not responsible for any damage caused by the use of this software. **Use responsibly!**

---

<p align="center">
Developed with ❤️ for the BLE research community.
</p>

