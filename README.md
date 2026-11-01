# NetShield 2.0 — Offline-First Internet Failure Detection & Recovery Tool

[![Java Version](https://img.shields.io/badge/Java-21%20LTS-007396?style=flat&logo=openjdk)](https://openjdk.org/)
[![JavaFX Version](https://img.shields.io/badge/JavaFX-21.0.2-FF8800?style=flat&logo=javafx)](https://openjfx.io/)
[![Build Tool](https://img.shields.io/badge/Maven-3.9+-C71A36?style=flat&logo=apache-maven)](https://maven.apache.org/)
[![Database](https://img.shields.io/badge/SQLite-JDBC%203.45-003B57?style=flat&logo=sqlite)](https://www.sqlite.org/)
[![Tests](https://img.shields.io/badge/JUnit-5%20(28%20Passing)-25A162?style=flat&logo=junit5)](https://junit.org/)
[![Platform](https://img.shields.io/badge/Platform-Windows%2010%2F11-0078D6?style=flat&logo=windows)](https://microsoft.com/windows)

> **College CSE Production-Quality Capstone / Final Year Project (Enhanced 2.0 Edition)**  
> NetShield is a high-performance, offline-first Windows desktop diagnostic application built to detect, classify, and guide the recovery of local and upstream network failures. It features real multi-layered network diagnostics, a prioritized rule-based expert diagnosis engine, a cyber-themed dark JavaFX dashboard, offline troubleshooting handbooks, visual traceroute hop mapping, Wi-Fi radio analyzer, real-time bandwidth speedometer, automated one-click recovery wizard, system tray monitoring, and executive PDF/HTML report generation.

---

## 🌟 Key Highlights & NetShield 2.0 Enhancements

1. **Genuine Desktop Application**: Native Windows JavaFX 21 desktop application — not a web wrapper, not an Electron app, and not a theoretical management system.
2. **Offline-First Resilience**: Runs 100% locally with zero internet dependency for its own operation. All troubleshooting handbooks, historical databases, diagnosis engines, and chart visualizers work even when completely disconnected from the network.
3. **No Cheating / Real Diagnostics**: Every diagnostic metric is measured in real time using native Java networking (`java.net`), raw RFC 1035 UDP DNS sockets, and safe Windows system probes. NetShield never claims a test succeeded unless it actually completed.
4. **Prioritized Rule-Based Diagnosis Engine**: Rather than generic "Internet Down" alerts, NetShield evaluates an evidence chain against 11 specialized diagnosis rules, reporting exact root causes with confidence ratings (`CERTAIN`, `HIGH`, `MEDIUM`, `LOW`) and uncertainty disclosure.
5. **Visual Hop-by-Hop Route Trace (Traceroute)**: Interactive visual route topology mapping intermediate routers (`Host -> Gateway -> ISP Node -> Backbone -> Target`), measuring per-hop latency and automatically identifying network bottlenecks where latency spikes.
6. **Wi-Fi Signal & Radio Telemetry**: Live Wi-Fi signal gauge displaying percentage and dBm, SSID, BSSID, protocol (Wi-Fi 6 802.11ax/ac/n), band (2.4 GHz vs 5 GHz), channel, link rates, and wireless interference advisories.
7. **Real-Time Bandwidth Speedometer**: Live digital streaming throughput meter benchmarking download speed (Mbps), estimated upload, TCP latency, and total bytes transferred.
8. **Automated One-Click Safe Recovery Wizard**: Guided step-by-step remediation with before-and-after verification diff (e.g. executes safe DNS flush, re-runs verification probe, and displays Before vs After comparison).
9. **Automated Device Notifications & System Tray Integration**: Minimizes to Windows System Tray with dynamic color-coded shield icon. Automatically alerts the host device via native Windows toast/balloon notifications and audible alerts whenever network connectivity drops, recovers, or changes IP/SSID.
10. **Executive Print-to-PDF / HTML Reports**: One-click generation of professional, print-styled HTML reports with SVG badges, evidence chains, recovery plans, and multi-layer metrics for academic project submissions.
11. **Strict Safety Policy (Non-Destructive)**: Adheres strictly to the safety rule: *Never run destructive commands, never change adapter configurations automatically, never disable firewalls, and never execute administrator elevation behind the user's back*.
12. **Privacy-First Local Storage**: All session logs, latency trends, and settings are stored locally in an embedded SQLite database (`%APPDATA%\NetShield\netshield.db`). Zero telemetry or external data collection.

---

## 🏛️ System Architecture

```
+-------------------------------------------------------------------------------------------------------+
|                                           USER INTERFACE                                              |
|                               Modern Dark-Themed JavaFX 21 Dashboard                                  |
| [Live Diagnostics] [Route & Hop Map] [Wi-Fi & Radio] [Speed & Bandwidth] [History] [Offline KB] [Config]|
+---------------------------------------------------+---------------------------------------------------+
                                                    |
                                                    v
+-------------------------------------------------------------------------------------------------------+
|                                       APPLICATION CONTROLLER                                          |
|                com.netshield.ui.MainDashboardController (JavaFX Task & Scheduled Executor)            |
|       + NotificationManager (Tray & State-Change Alerts)   + RecoveryWizardDialog (Guided Fix)        |
+---------------------+---------------------+---------------------+---------------------+---------------+
                      |                     |                     |                     |
                      v                     v                     v                     v
+-----------------------------+ +---------------------+ +--------------------+ +------------------------+
|    DIAGNOSTICS PIPELINE     | |  DIAGNOSIS ENGINE   | | ENHANCED MODULES   | |   PERSISTENCE & EXPORT |
|  Interface Diagnostic       | |  Priority Rules:    | | WifiDiagnostic     | | SQLite Embedded DB     |
|  Default Gateway Reachable  | |   * NoAdapterRule   | | TracerouteDiagnost.| | HistoryDao (Sessions)  |
|  DNS Resolution (System/UDP)| |   * ApipaDhcpRule   | | SpeedTestService   | | SettingsDao (Config)   |
|  HTTPS Endpoints & TLS      | |   * CaptivePortal   | +--------------------+ | ReportExporter (CSV)   |
|  Latency, Jitter, PacketLoss| |   * GatewayTimeout  |                        | HtmlReportGenerator    |
|  Captive Portal Intercept   | |   * LocalDnsFailure | +--------------------+ +------------------------+
+-----------------------------+ |   * CompleteDnsFail | | OFFLINE KNOWLEDGE  |
              |                 |   * IspOutageRule   | | KnowledgeBaseRepo  |
              v                 |   * HighLossRule    | | 8 In-Depth Guides  |
+-----------------------------+ |   * PartialOutage   | +--------------------+
|    SYSTEM & OS ADAPTERS     | |   * HealthyNetwork  |
|  SafeCommandRunner          | |   * Inconclusive    |
|  PlatformUtils (Windows)    | +---------------------+
+-----------------------------+
```

---

## 🔬 Multi-Layer Diagnostics & Enhanced Modules

| Module / Stage | Target Layer | Technique & Protocol | Diagnostic Purpose |
|---|---|---|---|
| **1. Network Interfaces** | Layer 1 & 2 (Physical & Data Link) | `NetworkInterface.getNetworkInterfaces()` | Detects unplugged cable, disabled Wi-Fi, missing NIC drivers, and APIPA (`169.254.x.x`). |
| **2. Default Gateway** | Layer 3 (Network) | Windows routing table (`route print 0.0.0.0`), ICMP echo ping & TCP router probes | Detects local router freeze, switch port drop, gateway IP mismatch. |
| **3. DNS Resolution** | Layer 7 & Layer 4 (UDP) | System resolver (`InetAddress`) + Raw RFC 1035 UDP queries directly to `1.1.1.1` / `8.8.8.8` port 53 | Pinpoints whether local router DNS failed while upstream public DNS works. |
| **4. HTTPS Reachability** | Layer 4 & Layer 7 (Transport & App) | HTTP/HTTPS handshake to `1.1.1.1`, `google.com`, `cloudflare.com` | Identifies SSL handshake failures, clock skew, and port 443 blocking. |
| **5. Latency & Packet Loss** | Layer 3 / Layer 4 (QoS) | Multi-sample RTT probes calculating Min, Max, Average, Jitter, and Loss % | Identifies bufferbloat, Wi-Fi interference, packet loss spikes. |
| **6. Captive Portal Detection** | Layer 7 (Application) | HTTP probe to `http://connectivitycheck.gstatic.com/generate_204` (redirect-following disabled) | Catches hotel/campus Wi-Fi splash screen redirections (HTTP 302/307). |
| **7. Visual Route Trace** | Layer 3 (Routing) | Hop-by-hop `tracert -d` intermediate router path analysis | Visualizes intermediate hops and flags the exact hop causing latency spikes. |
| **8. Wi-Fi & Radio Analyzer** | 802.11 Wireless | Safe `netsh wlan show interfaces` telemetry | Measures signal % and dBm, SSID, BSSID, protocol (802.11ax), channel, and 2.4/5GHz interference. |
| **9. Bandwidth Speedometer** | Layer 7 (Throughput) | Streaming chunked download/upload speed test with real-time updates | Computes peak Mbps download throughput and TCP latency. |

---

## 🔔 Automated Device Notifications

NetShield 2.0 includes an automated network state-change detector configured specifically to alert the host device whenever network conditions change during continuous monitoring:

- **Outage Detected**: When connectivity transitions to `CRITICAL`, immediately triggers a native Windows tray bubble notification and plays an audible alert tone.
- **Connection Restored**: When internet returns to `HEALTHY`, automatically notifies: *"Internet Connection Restored (Latency: 14ms)"*.
- **Quality Degraded**: Triggers an alert when packet loss or jitter exceeds safe thresholds.
- **Device IP Changed**: Alerts if the host device receives a new DHCP lease or changes subnet.
- **Wi-Fi Access Point Roamed**: Alerts if the host device roams between different access points or SSIDs.

---

## 🖥️ Complete Dashboard Tabs

1. **Live Diagnostics**: Hero status banner, pipeline progress bar, 6 live stage cards, factual evidence list, recovery action cards with copyable commands and safe helper triggers, and event console.
2. **Route & Hop Map**: Visual interactive hop-by-hop traceroute diagram with latency badges, timeout indicators, and automatic bottleneck detection.
3. **Wi-Fi & Radio**: Large signal gauge (% and dBm), network metadata grid (SSID, BSSID, Protocol, Band, Channel, Link speed), and interference advisory.
4. **Speed & Bandwidth**: Real-time digital speedometer gauge with live throughput readout (Mbps), progress bar, and data transfer statistics.
5. **History & Analytics**: Stat tiles (Total Scans, Uptime %, Average Web Latency, Outages Recorded), real-time JavaFX `LineChart`, and a searchable session history table with CSV and JSON export.
6. **Offline Knowledge Base**: 8 comprehensive offline troubleshooting articles with instant search filtering, problem symptoms, technical explanations, and copyable diagnostic commands.
7. **Configuration & Endpoints**: Custom configuration of DNS test hostnames, direct recursive DNS servers, HTTPS target endpoints, captive portal URL, socket timeouts (ms), ping sample counts, and repeat monitor intervals.

---

## 🚀 Build, Test, and Run Commands

The project includes both standard Maven commands and the self-contained wrapper `mvnw.cmd`:

### 1. Run All 28 Unit Tests
```powershell
.\mvnw.cmd test
```
All 28 unit tests execute offline using in-memory databases and mock diagnostics:
```
[INFO] Results:
[INFO] Tests run: 28, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### 2. Clean and Run Unit Tests
```powershell
.\mvnw.cmd clean test
```

### 3. Launch the NetShield Application
```powershell
.\mvnw.cmd javafx:run
```
NetShield compiles, initializes the SQLite database in `%APPDATA%\NetShield\netshield.db`, registers the system tray icon, loads the dark theme stylesheet, and displays the desktop dashboard.

### 4. Package Application JAR
```powershell
.\mvnw.cmd package -DskipTests
```
Generates `target/net-shield-1.0.0.jar`.

---

## 💻 Exact IntelliJ IDEA Setup Instructions

1. **Open Project**: Launch IntelliJ IDEA -> **File** -> **Open...** -> Select `c:\Users\dhara\OneDrive\Desktop\net-shield` -> Click **OK**.
2. **Configure Project SDK**: **File** -> **Project Structure...** -> Under **Project Settings** -> **Project**:
   - **SDK**: Select **Java 21 (JDK 21)**.
   - **Language level**: Set to **21 - Sealed types, pattern matching, virtual threads**.
   - Click **Apply** and **OK**.
3. **Reload Maven Dependencies**: Open the **Maven** tool window on the right side of IntelliJ -> Click the **Reload All Maven Projects** icon.
4. **Run via Maven Tool Window**: In the **Maven** sidebar -> Expand `net-shield` -> `Plugins` -> `javafx` -> Double-click **`javafx:run`**.
5. **(Optional) Run Configuration**: **Run** -> **Edit Configurations...** -> Add **Application** -> Main Class: `com.netshield.NetShieldLauncher` -> JRE: `21`.

---

## 🧪 Unit Test Coverage Summary (28 Passing Tests)

- `DiagnosisRuleTest` (9 tests): Validates all 9 primary inference rules (No Adapter, APIPA, Captive Portal, Gateway Outage, Local DNS Failure, Complete DNS Failure, ISP Outage, High Loss/Jitter, Healthy Network).
- `DiagnosticServiceMockTest` (2 tests): Verifies 6-stage orchestration and cancellation without touching live internet.
- `WifiDiagnosticTest` (3 tests): Verifies parsing of Wi-Fi 6 5GHz, congested 2.4GHz, and disconnected wireless states.
- `TracerouteDiagnosticTest` (2 tests): Verifies intermediate router hop parsing, bottleneck spike detection, and timeout handling.
- `NotificationManagerTest` (1 test): Verifies device alerting on network outages, restoration, IP changes, and Wi-Fi roaming.
- `HtmlReportGeneratorTest` (1 test): Verifies executive print-to-PDF HTML report generation with evidence and metrics.
- `DatabasePersistenceTest` (3 tests): Verifies SQLite session persistence, uptime/outage calculations, and settings reloading.
- `SafeCommandRunnerTest` (4 tests): Verifies command whitelist execution and strict rejection of unauthorized or destructive binaries.
- `ReportExporterTest` (3 tests): Verifies CSV, JSON, and formatted plain text diagnostic report exports.

---

## 👨‍💻 Academic Project Details

- **Project Title**: NetShield 2.0 — Offline-First Internet Failure Detection and Recovery Tool
- **Domain**: Computer Networks / Distributed Systems / Software Engineering (CSE Capstone)
- **Primary Focus**: Layer-by-layer network fault localization, rule-based inference, and safe offline remediation.
