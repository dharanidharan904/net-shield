# NetShield Android — Mobile Network Checking App

NetShield Android is a native Android application designed to diagnose network connectivity faults directly on Android mobile devices (smartphones and tablets).

---

## 📱 Features on Android
* **6-Layer Network Diagnostics**:
  1. **Physical NIC**: Detects Wi-Fi, Mobile Data (4G/5G), and Ethernet connectivity via `ConnectivityManager`.
  2. **Default Gateway**: Queries DHCP gateway router and inspects LAN reachability.
  3. **DNS Resolvers**: Validates domain name resolution via `InetAddress.getAllByName`.
  4. **WAN / HTTPS Reachability**: Direct TLS handshake to public endpoints (`https://1.1.1.1`).
  5. **Latency & Jitter**: Real-time socket round-trip time (RTT) measurements in milliseconds.
  6. **Captive Portal Detection**: Sniffs HTTP 204 redirect checks to identify Wi-Fi captive logins.
* **Live Wi-Fi Telemetry**:
  * Real-time SSID, Signal Strength (%), RSSI (dBm), Operating Frequency (2.4 GHz vs 5 GHz), and Link Speed (Mbps).
* **⚡ One-Tap Auto-Heal**:
  * Automatically re-associates Wi-Fi radio and clears stale mobile sockets with an instant before-and-after re-verification probe.

---

## 🚀 How to Build & Install on Your Android Phone

### Step 1: Open in Android Studio
1. Launch **Android Studio**.
2. Click **File -> Open...**
3. Select the folder:
   ```text
   c:\Users\dhara\OneDrive\Desktop\net-shield\android-app
   ```
4. Wait for Gradle Sync to complete.

### Step 2: Build the APK
1. In Android Studio, click **Build -> Build Bundle(s) / APK(s) -> Build APK(s)**.
2. Android Studio will compile and output the APK file at:
   ```text
   android-app\app\build\outputs\apk\debug\app-debug.apk
   ```

### Step 3: Install on Your Android Phone
* **Option A (Via USB Cable)**:
  1. Enable **Developer Options** and **USB Debugging** on your Android phone.
  2. Plug your phone into your laptop via USB.
  3. Click the green **Run (▶)** button in Android Studio.
* **Option B (Direct APK Transfer)**:
  1. Copy `app-debug.apk` to your phone via WhatsApp, Google Drive, or USB.
  2. Tap the APK on your phone and click **Install**.

---

## 🌐 Instant Alternative: Android Mobile Web Companion
If you want to check your network from your Android phone **right now without installing Android Studio**:
1. Run NetShield on your Windows laptop:
   ```powershell
   .\mvnw.cmd javafx:run
   ```
2. Click the **"📱 Android Mobile"** button in the top navigation bar.
3. It will give you your local LAN URL (e.g. `http://192.168.1.104:8080`).
4. On your Android phone, connect to your home Wi-Fi and open `http://192.168.1.104:8080` in Chrome!
