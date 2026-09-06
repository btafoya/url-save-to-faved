<p align="center"><img src="logo.png" width="180" alt="URL Share Relay logo"></p>

# URL Share Relay

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A minimal Android share-target app. It receives a shared URL from another app, fetches the page's metadata, lets you review/edit the title and description, then hands it back off through Android's share sheet to whatever app you actually want to send it to.

Chrome/Twitter/etc. → **Share** → URL Share Relay → fetch metadata → preview/edit → Share Sheet → destination app

No backend, no analytics, no login, no unnecessary permissions.

## Why

Some apps produce ugly or missing link previews when you share a raw URL to them. This app sits in the middle of the share flow: it resolves the URL, pulls the real title/description/image from the page, and lets you fix up the text before it goes anywhere.

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

The receiving app ultimately controls how its own preview renders. This app can supply a title, description, and URL through Android's sharing APIs, but it can't force another app to display a particular preview format.

## License

MIT — see [LICENSE](LICENSE).
