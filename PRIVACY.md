# Privacy Policy for IsekaiPlayer

**Effective Date:** September 26, 2026  
**Last Updated:** September 26, 2026  

## 1. Introduction

Welcome to **IsekaiPlayer** ("the App"), developed by **onlymash** ("we", "us", or "our"). We deeply respect your privacy and are committed to protecting it.

This Privacy Policy explains how our App handles information when you install and use it.

> **Key Takeaway:**  
> **IsekaiPlayer is a standalone local and network media player.** We do not collect, transmit, store, sell, or share any of your personal information, device identifiers, or usage data. The App contains **no third-party analytics, tracking, or advertising SDKs**.

---

## 2. Information Collection and Use

### No Personal Data Collection
We do **not** collect any personal data or personally identifiable information (PII) from you, such as your name, email address, phone number, precise location, or contact list.

### No Device Identifier Tracking
We do **not** collect, read, or harvest any persistent device identifiers (such as Android ID, IMEI, MAC address, AAID / Advertising ID, or SIM card serial numbers).

### Local Data Processing Only
All media playback, processing, and indexing occur **100% locally on your device**:
* **Local Media Playback:** Media files stored on your local device are scanned, decoded, and rendered purely on your device.
* **Network & Local Network Streaming:** When you stream content over SMB, FTP, WebDAV, or direct HTTP(S) links, connections are established directly between your device and the target server/NAS. No intermediate servers or proxy services operated by us are involved, and your streaming URLs or playback logs are never recorded or transmitted to us.
* **Playback History & Preferences:** Your playback progress, history, subtitles preferences, and player configurations are saved locally on your device using standard Android local storage mechanisms (DataStore / local database). This data is never synchronized to or stored on any external server.

---

## 3. Permissions Used by the App

To function properly as a full-featured media player, the App requests certain system permissions. We follow the principle of **least privilege** and only request permissions necessary for core functionality:

| Permission | Purpose & Usage | Requirement |
| :--- | :--- | :--- |
| **Storage & File Management**<br>(`MANAGE_EXTERNAL_STORAGE`) | Allows the App to perform media scanning, discovery, and file management operations for videos, audio, and subtitles stored on your device. | Required for local file playback and file management |
| **Network Access**<br>(`INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_LOCAL_NETWORK`) | Allows the App to stream user-requested media from local network shares (SMB, FTP, WebDAV) or user-provided network URLs. `ACCESS_LOCAL_NETWORK` is required on Android 17+ to access local network devices. | Required for network streaming features |
| **Notifications**<br>(`POST_NOTIFICATIONS`) | Displays playback controls (play/pause, skip, progress bar) in the Android system notification shade. | Optional (Can be disabled in system settings) |
| **Foreground Service**<br>(`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `FOREGROUND_SERVICE_DATA_SYNC`) | Enables continuous, uninterrupted media playback in the background (`FOREGROUND_SERVICE_MEDIA_PLAYBACK`) and background file operations (`FOREGROUND_SERVICE_DATA_SYNC`). | Required for background playback and file operations |

*We do not use any permissions for tracking, profiling, or background data gathering.*

---

## 4. Third-Party Services and Analytics

* **No Analytics SDKs:** The App does not integrate any third-party analytics frameworks (e.g., Firebase Analytics, Google Analytics, Flurry, Adjust).
* **No Advertising SDKs:** The App is completely ad-free and contains no advertising networks or tracking software (e.g., AdMob).
* **No Third-Party Data Sharing:** Because we do not collect any user data, we cannot and do not share, sell, trade, or rent any personal information to third parties under any circumstances.

---

## 5. Data Retention, Security, and Control

* **Local Retention:** All App settings, bookmarks, and playback history reside strictly within the App's private local storage on your device.
* **User Control:** You have full control over your local data. You can clear your playback history directly within the App or remove all App data at any time via your device's **Settings > Apps > IsekaiPlayer > Storage > Clear Data**.
* **Automatic Deletion:** Uninstalling the App immediately and permanently deletes all local data associated with the App from your device.

---

## 6. Children's Privacy

Our App is safe for general audiences and does not knowingly collect any personal information from children under the age of 13 (or 16 in the European Union). Because we do not collect any personal data from any user, our App complies fully with COPPA (Children's Online Privacy Protection Act) and GDPR children's privacy regulations.

---

## 7. Changes to This Privacy Policy

We may update this Privacy Policy from time to time to reflect changes in our practices or legal obligations. Any updates will be posted on this page with an updated "Last Updated" date. We encourage you to review this Privacy Policy periodically.

---

## 8. Contact Us

If you have any questions, concerns, or feedback regarding this Privacy Policy or our privacy practices, please contact us at:

* **Developer:** onlymash
* **Email:** `fiepi.dev@gmail.com`
* **Source Code / Project Repository:** `https://github.com/onlymash/IsekaiPlayer`
