### ⏬ WhatsApp Status Saver App (AI-Powered)
![Status](https://img.shields.io/badge/Code-181717?style=for-the-badge&logo=github&logoColor=white) ![AI](https://img.shields.io/badge/Google_Gemini-8E75B2?style=for-the-badge&logo=googlegemini&logoColor=white)  ![UI](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white) ![Async](https://img.shields.io/badge/Kotlin_Coroutines-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)

> Clean, local file storage architecture designed to pull, cache, and schedule media assets instantly.

| Subsystem | Technical Execution Architecture |
| :--- | :--- |
| 🤖 **AI Smart Search & Recognition** | Integrated AI Status Magic suite featuring AI Assistant for custom prompt responses (e.g., Islamic status recommendations), deep video AI Analysis (scene-by-scene visual breakdowns and mood descriptions), Text OCR, automatic Captions, Shayari generation, Object detection, Translate, Mood analysis, and curated Magic Picks. |
| 🔒 **Private Vault & Biometric Security** | Secure media gallery protected by Android Biometric Prompt API (Fingerprint / Face Unlock), featuring AES-256 encrypted local storage and hidden directory management for private status videos and photos. |
| ✂️ **30-Second Video Splitter** | Automated video chunking engine built on Media3 / FFmpeg bindings, precisely trimming and slicing long videos into seamless 30-second clips for direct WhatsApp Status uploads. |
| 🕵️‍♂️ **Deleted Messages & Logs** |Real-time notification listener service that securely captures and stores revoked or deleted WhatsApp chats, alongside a comprehensive history dashboard for tracking status activity logs. |
| 🎵 **Audio Extractor & Converter** | High-performance media processing pipeline leveraging Media3 / FFmpeg bindings to extract, transcode, and convert video audio tracks into high-quality MP3, AAC, and WAV formats with customizable bitrate settings. |
| 🔄 **Reactive State** | Fluid UI adjustments driven completely via async Kotlin Coroutines lifecycle pipelines for real-time status fetching across WhatsApp & Business environments, video player control state, Export All Media (ZIP backup), and Cloud Sync data management operations. |
| 💎 **Material Design** | Adaptive Jetpack Compose UI with dark mode toggle, seamless status previewing with built-in video controls (Send, Share, Save), Direct Chat functionality (send WhatsApp messages without saving contacts via country code picker), and a categorized offline Saved Files gallery (Photos & Videos). |
| ⏰ **Automated Triggers** | Custom status Reminders system with a dedicated scheduler dashboard ("Today", "Scheduled", and "All Reminders" tracking lists) processing system-level alerts and notifications safely via system clock integrations. |
| 🎨 **Asset Attribution** | Media action icons, platform selector cards (WhatsApp Regular & Business), and download utility vectors optimized from [Uxwing](https://uxwing.com/). |

<details>
<summary><b>📱 View App Screenshots (Click to Expand)</b></summary>
<br/>
<table width="100%">
  <!-- Row 1: Images -->
  <tr>
    <td width="33.3%" align="center"><img src="https://github.com/user-attachments/assets/8fb01679-4c49-463d-a41c-04a4a4a5c5d8" width="100%" alt="Screen 1" /></td>
    <td width="33.3%" align="center"><img src="https://github.com/user-attachments/assets/0f0c67f3-a9fb-4bda-bcf9-8302734e7fd2" width="100%" alt="Screen 2" /></td>
    <td width="33.3%" align="center"><img src="https://github.com/user-attachments/assets/825a26ec-1eb2-42ae-865c-9c332a53fe3b" width="100%" alt="Screen 3" /></td>
  </tr>
  <!-- Row 1: Details -->
  <tr>
    <td align="center"><b>Screen 1:</b> Home Screen</td>
    <td align="center"><b>Screen 2:</b> AI Assistant Screen </td>
    <td align="center"><b>Screen 3:</b> Direct Chat </td>
  </tr>

  <!-- Row 2: Images -->
  <tr>
    <td width="33.3%" align="center"><img src="https://github.com/user-attachments/assets/d5db3e48-aec7-4117-96ae-fd6e68c428a3" width="100%" alt="Screen 4" /></td>
    <td width="33.3%" align="center"><img src="https://github.com/user-attachments/assets/ce3eb02f-528d-4f76-acbc-7de821fd6873" width="100%" alt="Screen 5" /></td>
    <td width="33.3%" align="center"><img src="https://github.com/user-attachments/assets/177fe0bf-326a-4982-baaa-8fa524266c48" width="100%" alt="Screen 6" /></td>
  </tr>
  <!-- Row 2: Details -->
  <tr>
    <td align="center"><b>Screen 4:</b> Status Video </td>
    <td align="center"><b>Screen 5:</b> AI Status Magic </td>
    <td align="center"><b>Screen 6:</b> AI Analysis </td>
  </tr>

  <!-- Row 3: Images -->
  <tr>
    <td width="33.3%" align="center"><img src="https://github.com/user-attachments/assets/c291ed40-5789-4981-8b36-7efddcd3a6fe" width="100%" alt="Screen 7" /></td>
    <td width="33.3%" align="center"><img src="https://github.com/user-attachments/assets/e4320c70-933d-4be5-9ec4-5575ba291013" width="100%" alt="Screen 8" /></td>
    <td width="33.3%" align="center"><img src="https://github.com/user-attachments/assets/2d8bfb61-94d9-48a5-8684-a8f579cfa854" width="100%" alt="Screen 9" /></td>
  </tr>
  <!-- Row 3: Details -->
  <tr>
    <td align="center"><b>Screen 7:</b> Saved Files </td>
    <td align="center"><b>Screen 8:</b> Reminder Screen </td>
    <td align="center"><b>Screen 9:</b> Spliter Screen </td>
  </tr>

   <!-- Row 4: Images -->
  <tr>
    <td width="33.3%" align="center"><img src="https://github.com/user-attachments/assets/a9f6b4bf-2fc4-4de8-95ee-cc51af6278fd" width="100%" alt="Screen 7" /></td>
    <td width="33.3%" align="center"><img src="https://github.com/user-attachments/assets/ea902a93-2ff7-486b-b1e1-2e35cb05fba6" width="100%" alt="Screen 8" /></td>
    <td width="33.3%" align="center"><img src="https://github.com/user-attachments/assets/817f2764-1f0a-4126-b6bb-f9005491a6b0" width="100%" alt="Screen 9" /></td>
  </tr>
  <!-- Row 4: Details -->
  <tr>
    <td align="center"><b>Screen 10:</b> Recovery Messages </td>
    <td align="center"><b>Screen 11:</b> Vault Screen </td>
    <td align="center"><b>Screen 12:</b> Settings Screen </td>
  </tr>
  
</table>
</details>

## 🛠 Setup & Installation

### 📋 Prerequisites
* Android Studio Ladybug (or newer)
* JDK 17+
* Android SDK 28 (Android 9.0) or higher
