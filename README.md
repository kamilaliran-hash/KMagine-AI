# KMagine AI

<img src="desktop/build/icon.png" width="96" alt="KA logo">

A text-to-image and photo-to-image app in the style of Grok Imagine.

- **Text → image:** type a prompt, pick a style, a shape and how many pictures you want.
- **Photo + prompt → image:** tap **＋** to upload a photo (or drag/paste it), then say what to do,
  e.g. "turn me into an anime hero" or "make it a watercolor painting".
- **Keep editing:** open any result and tap **Edit this image** to use it as the next photo.

## Image engines

Pick one in **Settings**. If you upload a photo while using an engine that can't read photos,
the app automatically uses the free Hugging Face engine for it.

| Engine | Cost / account | Text → image | Photo + text → image |
|---|---|---|---|
| Pollinations (default) | Free, no key, no account | ✅ | ❌ |
| Hugging Face FLUX | Free, no key (small daily limit; optional free HF token for a bit more) | ✅ FLUX.1-schnell | ✅ FLUX.1 Kontext |
| Puter | Free Puter sign-in; usage comes from your Puter account's allowance | ✅ | ✅ (GPT Image, Nano Banana 2, FLUX.2, Grok) |
| Grok Imagine (xAI) | Your xAI API key (about $0.02 per picture) | ✅ | ✅ |

- Hugging Face runs on free shared demo apps (black-forest-labs/FLUX.1-schnell and FLUX.1-Kontext-Dev).
  Hugging Face limits free use per person per day. The app works around it automatically:
  - **Text pictures:** when the Hugging Face limit is reached, the app carries on with Pollinations (free, unlimited).
  - **Photo pictures:** it switches to Grok if you saved an xAI key, or to Puter if you're signed in.
    Otherwise it asks you to pick Puter or Grok, or wait until the next day.
- Puter asks you to sign in (or create a free account) the first time.
- Grok: get a key at https://console.x.ai. Keys and tokens are stored only on your device.

## Folders

```
web/        the app itself (one HTML file) — shared by both apps
desktop/    Windows app (Electron) → .exe
android/    Android app (WebView) → .apk
.github/    automatic build for both
```

## Easiest way to get the .exe and .apk (no software to install)

1. Create a free account at https://github.com and make a **new repository** (private is fine).
2. On the repository page, click **Add file → Upload files**, drag in **everything inside this folder**
   (including the hidden `.github` folder — if your computer hides it, upload it separately), and click **Commit**.
3. Open the **Actions** tab. A build called *Build KMagine AI apps* starts by itself (about 5–10 minutes).
4. When it shows a green tick, click it and scroll to **Artifacts**:
   - **KMagineAI-Android** → contains `KMagineAI.apk`
   - **KMagineAI-Windows** → contains `KMagineAI-Portable.exe` (just run it) and `KMagineAI-Setup.exe` (installer)

Installing the APK: copy it to your phone, open it, and allow "Install unknown apps" when Android asks.
Windows may show "Windows protected your PC" because the app isn't code-signed — click **More info → Run anyway**.

## Building on your own computer instead

**Android** — install Android Studio, choose *Open* and select the `android` folder, wait for it to sync,
then *Build → Build App Bundle(s) / APK(s) → Build APK(s)*. Or from a terminal: `cd android && ./gradlew assembleRelease`.
The APK is in `android/app/build/outputs/apk/release/`.

**Windows** — install Node.js 20+ from https://nodejs.org, then in a terminal:

```
cd desktop
npm install
npm start        # try the app
npm run dist     # build the .exe files into desktop/dist
```

## Notes

- The APK is signed with a debug key so it installs directly. To publish on Google Play, sign it with your own release key.
- Pollinations' free tier handles about one picture every ~15 seconds and is for non-commercial use.
- Photo mode sends your photo to xAI to create the new picture.
