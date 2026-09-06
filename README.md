<p align="center"><img src="logo.png" width="180" alt="URL Save to Faved logo"></p>

# URL Save to Faved

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A minimal Android share-target app for [Faved](https://faved.to/) — receives a shared URL from another app, fetches the page's metadata, lets you review/edit the title and description, then saves it straight to your Faved bookmarks with tags. Works against Faved Cloud or a [self-hosted Faved instance](https://github.com/denho/faved).

Chrome/Twitter/etc. → **Share** → URL Save to Faved → fetch metadata → preview/edit → **Save to Faved** (assign tags) → done

The original re-share behavior is still there too: **Share again** hands the edited title/description/URL back off through Android's share sheet to any other app, same as before.

No analytics, no accounts beyond the one Faved server you configure, no unnecessary permissions.

## What it does

1. Extracts the URL from whatever you shared to it.
2. Downloads the page and pulls its title/description/image metadata.
3. Lets you edit that before it goes anywhere.
4. Either:
   - **Save to Faved** — sign in to your Faved server (server URL, username, password, kept on-device), pick from your existing tags or create a new one on the spot, and the bookmark is saved via Faved's API, or
   - **Share again** — re-share the result through Android's normal share sheet.

The first time you tap "Save to Faved" without a configured account, the app prompts for your server details inline, signs in, then continues straight to saving.

## Faved

[Faved](https://faved.to/) is a private, open-source bookmark manager with nested tags. Run it self-hosted ([denho/faved](https://github.com/denho/faved) — PHP/SQLite, MIT licensed) or use the hosted Faved Cloud. This app talks to a single Faved server/account at a time over its REST API, using the same session-cookie login the Faved web app uses.

## Supported metadata

Extracted with the following priority, falling back down the list as needed:

| Field | Priority |
|---|---|
| Title | `og:title` → `twitter:title` → `<title>` |
| Description | `og:description` → `twitter:description` → `meta[name=description]` |
| Image | `og:image` → `twitter:image` → `link[rel=image_src]` |
| Canonical URL | `og:url` → `link[rel=canonical]` → final response URL |

Relative URLs are resolved against the final (post-redirect) response URL.

## Stack

- Kotlin
- Jetpack Compose / Material 3
- OkHttp (networking)
- Jsoup (HTML parsing)
- AndroidX Security (encrypted on-device credential storage)
- Gradle Kotlin DSL

## Getting started

Clone the repo and open it in Android Studio, or build from the command line:

```bash
git clone git@github.com:btafoya/url-save-to-faved.git
cd url-save-to-faved
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`.

Requirements: Android SDK 26+ (minSdk), compiled against SDK 36.

## Notes

- Faved server credentials and session cookie are stored on-device via `EncryptedSharedPreferences` (Keystore-backed) — never sent anywhere but your configured Faved server.
- The app trusts the standard system TLS certificate store only; a self-hosted instance needs a properly trusted certificate (e.g. Let's Encrypt via reverse proxy).
- For the "Share again" path, the receiving app ultimately controls how its own preview renders — this app can only supply a title, description, and URL through Android's sharing APIs.

## License

MIT — see [LICENSE](LICENSE).
