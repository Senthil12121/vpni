# JOS VPN India — Android starter project

Kotlin Android app using the WireGuard tunnel library. Supports importing a WireGuard `.conf` profile, connecting/disconnecting, and optional per-app routing (included apps only). The selected profile determines the actual server country; the app does not include VPN servers.

## Build the APK with GitHub Actions

1. Create a GitHub repository and upload the contents of this folder to the repository root.
2. Commit `.github/workflows/build-apk.yml`.
3. Open the repository's **Actions** tab and run **Build JOS VPN India APK** (or push to `main`/`master`).
4. Download the `JOSVPN-India-debug-apk` artifact from the completed workflow and extract `app-debug.apk`.
5. Transfer `app-debug.apk` to your Android phone and install it. You may need to allow installation from that file manager/browser.

## Use

1. Obtain a valid WireGuard `.conf` profile from a trusted provider or your own server. For multiple countries, import a separate profile for each server as needed; this starter currently holds one profile at a time.
2. Open the app and tap **Import WireGuard .conf**.
3. Optionally choose apps to route through the VPN. With no apps selected, the profile is used without an app allow-list.
4. Tap **Connect VPN** and approve Android's VPN permission.

## Important limitations and safety

- No VPN server or subscription is included. A profile's endpoint determines the country; verify it with your provider.
- This starter currently imports one profile at a time; a full multi-country server picker/profile manager is not yet implemented.
- App selection is split tunnelling, not a firewall. Apps outside the selected set can use the normal network.
- The `.conf` file contains private keys. Never post it publicly or commit it to GitHub.
- Test on a non-critical device. This is starter code and must be built and tested before relying on it.
