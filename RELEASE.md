# BizliPay Connect — GitHub Release Guide

This document outlines how to publish a new release of **BizliPay Connect** to GitHub.

---

## 🚀 Release Version Information

- **Current Version:** `v1.0.1`
- **Version Code:** `2`
- **Application ID:** `com.bizlipay.connect`
- **Target SDK:** Android 16 (API 36)
- **Minimum SDK:** Android 7.0 (API 24)

---

## 📦 How to Publish a Release on GitHub

The repository includes an automated GitHub Actions CI/CD pipeline in [`.github/workflows/release.yml`](.github/workflows/release.yml). When triggered, it automatically compiles the Android APK, packages it, and publishes a new GitHub Release with the APK attached.

You can publish a new release using any of the 3 methods below:

### Method 1: GitHub Web UI (Actions Dispatch — Easiest)

1. Open your repository on GitHub in your web browser.
2. Click on the **Actions** tab.
3. In the left sidebar, click **"Build and Publish Release APK"**.
4. Click the **"Run workflow"** dropdown button on the right.
5. Enter:
   - **Release version tag:** `v1.0.1` (or your desired version tag)
   - **Release highlights:** Enter release summary/changelog notes.
6. Click **"Run workflow"**.
7. GitHub Actions will build `bizlipay-connect-v1.0.1.apk` and publish it under **Releases** automatically.

---

### Method 2: Create a Release in the GitHub Releases UI

1. Go to your repository on GitHub.
2. Click **Releases** (on the right sidebar of the repo homepage).
3. Click **"Draft a new release"**.
4. Click **"Choose a tag"**, type `v1.0.1`, and select **"Create new tag: v1.0.1 on publish"**.
5. Set the Release title (e.g. `BizliPay Connect v1.0.1`).
6. Click **"Generate release notes"** or type your changelog.
7. Click **"Publish release"**.
8. The GitHub Actions workflow will trigger immediately, build the connect APK, and attach `bizlipay-connect-v1.0.1.apk` directly to the release assets.

---

### Method 3: Push a Git Tag via CLI

If you have cloned the repository locally or push via terminal:

```bash
# Ensure you are on the latest main branch
git checkout main
git pull origin main

# Create an annotated tag for the new release
git tag -a v1.0.1 -m "Release v1.0.1 - BizliPay Connect"

# Push the tag to GitHub
git push origin v1.0.1
```

The GitHub Actions workflow will automatically start, assemble the APK, generate release notes, and publish the release to GitHub.

---

## 🛠 What the Release Workflow Does

1. **Checks out the repository** with full commit history for automatic changelog generation.
2. **Sets up JDK 21** with Gradle dependency caching for fast build speeds.
3. **Prepares signing keys** from GitHub Secrets (e.g., `KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) or repository fallback keystore.
4. **Runs `./gradlew assembleRelease --stacktrace`** to produce an optimized, shrunk, production-ready release APK.
5. **Packages & stages the artifact** to `bizlipay-connect-<version>.apk`.
6. **Publishes the release** to GitHub Releases with downloadable APK assets and release notes.

---

## 📱 Syncing Changes from Google AI Studio

If you made edits inside AI Studio:
1. Use the **Export / Push to GitHub** option in the AI Studio menu to sync all recent changes to your GitHub repository.
2. Trigger the release using **Method 1** or **Method 2** above!
