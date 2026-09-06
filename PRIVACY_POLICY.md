# Privacy Policy — URL Save to Faved

Last updated: 2026-09-06

URL Save to Faved is a share-target utility for [Faved](https://faved.to/). It does not use analytics or advertising, and does not send any data to servers operated by the developer — there is no backend. The only network destinations are the website you share a link from and the Faved server you configure yourself.

## What the app does

When you share a link to URL Save to Faved from another app, it:

1. Extracts the URL from the shared text.
2. Downloads the linked page's HTML directly from the URL's own server, over your device's normal internet connection.
3. Reads the page's title, description, and image metadata so you can review or edit it.
4. Either saves the result to the Faved server you've signed into (with tags you assign), or lets you share it on to another app of your choosing — your choice each time.

## What data is stored on your device

If you use the "Save to Faved" feature, you provide:

- Your Faved server URL, username, and password.
- The session cookie and CSRF token issued by that server after signing in.

This is stored in `EncryptedSharedPreferences`, encrypted at rest using the Android Keystore. It is never transmitted anywhere except back to the same Faved server, and it stays on your device — nothing is sent to the developer.

If you never use "Save to Faved," none of this applies: nothing is stored beyond normal Android app memory during a single use.

## What data is collected

None, by the developer. Specifically:

- No analytics or tracking SDKs.
- No advertising.
- No accounts or identifiers known to the developer — the only account involved is the Faved account you create and control on your own (self-hosted or cloud) server.
- No data is sent to any server operated by the developer — there is no backend.

## Network access

The app requests the `INTERNET` permission for two purposes:

- To fetch the HTML of the URL you share to it, so it can read that page's title/description/image. This goes directly from your device to the website you shared.
- If you use "Save to Faved," to sign in to and exchange bookmark/tag data with the Faved server URL you provide. This goes directly from your device to that server — a server you choose, and which may be self-hosted by you or a third party (e.g. Faved Cloud), never through infrastructure controlled by this app's developer.

Standard system TLS certificate validation is used for all connections; the app does not accept self-signed or otherwise untrusted certificates.

## Third parties

The app has no third-party integrations of its own. Two kinds of third-party servers are contacted, both at your direction:

- The server behind whatever URL you share to the app, to fetch its metadata.
- The Faved server (self-hosted or cloud) you configure for saving bookmarks.

The developer has no visibility into, or control over, what either of those servers does with the requests they receive.

## Changes

Any changes to this policy will be posted in this file.

## Contact

https://github.com/btafoya/url-save-to-faved/issues
