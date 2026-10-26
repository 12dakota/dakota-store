# Dakota Store

Personal Android app store that installs APKs from your GitHub Releases.

Currently configured for:
- `12dakota/provonlong-web-browser` (Provonlong Web Browser)

## Features
- Lists your configured GitHub repos
- Fetches the latest release via the public GitHub API
- Finds `.apk` assets
- Downloads and launches the system package installer

## Local Build
1. Open this folder in Android Studio (Ladybug or newer recommended).
2. Let it sync Gradle (it will download the Android SDK components if needed and generate the Gradle Wrapper).
3. Run on a device or emulator (API 24+).
4. On first install of an APK you may need to allow “Install unknown apps” for this store app.

## GitHub Actions Build
A workflow is included at `.github/workflows/build.yml`.

**To use it:**
1. Create a new public repository on GitHub (e.g. `dakota-store`).
2. Push this project (including the generated `gradlew` and `gradle/wrapper/` after opening in Android Studio once).
3. The workflow will automatically build a debug APK on every push to `main`.
4. Download the APK from the Actions artifacts tab.
5. Optionally tag a release (`git tag v1.0 && git push --tags`) to attach the APK to a GitHub Release.

The workflow uses `android-actions/setup-android` and builds with `./gradlew assembleDebug`.

## Add more apps
Edit `app/src/main/java/com/dakota/store/AppList.kt` and add more `RepoConfig` entries.

## Notes
- Uses public GitHub API (no token required for public releases).
- Download happens in a background thread; the UI shows a toast.
- APKs are cached in the app’s cache directory and installed via FileProvider.
- This is a minimal starter. You can expand it with progress bars, auto-update checks, icons, etc.

Built for personal use with your GitHub repos.