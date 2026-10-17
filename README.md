<p align="center">
  <b>English</b> | <a href="README.zh-CN.md">简体中文</a> | <a href="README.ja.md">日本語</a> | <a href="README.ko.md">한국어</a>
</p>

# <p align="center">Tieba Lite · TiebaLite</p>
<p align="center"><strong>Unofficial Baidu Tieba Android Client | One-Tap Full Check-In · 3000+ Forums Covered | Smart Update Check · Draft Recall | Android 5.0 → 16 Full Compatibility | Compose · Kotlin · No Ads</strong></p>
<p align="center">
    <a href="https://github.com/qaqmin/TiebaLite/releases/latest">
        <img alt="Latest Release" src="https://img.shields.io/github/v/release/qaqmin/TiebaLite?style=flat&color=blue">
    </a>
    <a href="https://github.com/qaqmin/TiebaLite/actions/workflows/build.yml">
        <img alt="Build Status" src="https://github.com/qaqmin/TiebaLite/actions/workflows/build.yml/badge.svg">
    </a>
    <a href="https://github.com/qaqmin/TiebaLite/blob/4.0-dev/LICENSE">
        <img alt="License" src="https://img.shields.io/badge/License-GPL%203.0-green.svg">
    </a>
    <img alt="API" src="https://img.shields.io/badge/API-21%2B-brightgreen">
    <img alt="Android" src="https://img.shields.io/badge/Android-16%20ready-blue">
</p>

---

> ### 👤 Maintainer's Corner
> When you notice this repo hasn't shipped a new version or fixed bugs for a while, the maintainer is most likely **traveling, delivering food, gaming, or writing novels**,
> or shuttling between the **US / Korea / Japan stock and forex markets** — or perhaps claiming unemployment benefits. Life's script is never monotonous.
>
> But rest assured: **every reported bug will be investigated and fixed, one by one.**
>
> - 📱 **Test devices**: Samsung Galaxy S-series flagships + Z Fold series (HK ROM)
> - 🚀 **Planned**: OPPO / vivo / Xiaomi real-device coverage down the road
> - 🐉 **HarmonyOS status**: No HarmonyOS device on hand for now, so HarmonyOS-side bugs are hard to locate and fix in the short term; friends with HarmonyOS devices are welcome to download the source and try fixing them
>
> 📌 **Account Migration Notice (2026-10-04)**: The original GitHub account **min09577** was banned for reasons beyond our control (all of its repositories now return 404).
> This project has fully migrated to the new account **[qaqmin09577](https://github.com/qaqmin09577/TiebaLite)**;
> the Codeberg mirror (min09577/TiebaLite) continues to sync and serve as a fallback update channel.
> The default in-app update channel has switched to GitHub; users on older versions can still receive updates as usual.
>
> 📌 **Fallen Accounts (2026-10-07)**: This project's GitHub accounts have now been banned twice — the original **min09577** and the first migration account **qaqmin09577** were both banned for reasons beyond our control (all repositories return 404). These are two sacrificed accounts, recorded here in memoriam. The project now lives on the newest GitHub account **[qaqmin](https://github.com/qaqmin/TiebaLite)** and the Codeberg mirror; users on older versions are unaffected (update checks fall back to the mirror channel automatically).

---

## ☕ Optional Sponsorship

This project is written in spare time, powered purely by passion. I'm glad if it helps you. Still, the API tokens consumed by testing have exceeded my expectations, and the long debugging hours have eaten into a fair share of rest time (lol).

If this project happens to solve a problem for you, or saves you some tinkering time, feel free to tip as you wish. All donations go toward "refilling" the hefty API bills — and they are the biggest motivation that lets me keep updating it without reservation.

Of course, it is entirely voluntary — please don't feel any pressure. If you find it useful, a **Star** alone is a huge encouragement.

<p align="center">
    <img src="docs/images/sponsor_qr.png" alt="Sponsor QR (Binance · BEP-20)" width="360">
</p>

<p align="center"><i>Binance Wallet QR · USDT · BNB Smart Chain / BEP-20</i></p>

---

## ✨ Why TiebaLite

| Feature | Description |
|------|------|
| 📝 **One-Tap Full Check-In** | Smart paginated fetching of all followed forums; a single run covers 3000+ forums with real-time notification progress |
| ⏰ **Scheduled Auto Check-In** | Precise wake-up even in Doze mode; zero background residency — the full check-in completes on time, automatically |
| 📝 **Draft Recall** | Replies are auto-saved; the draft list shows forum name / content preview and jumps straight back to the original floor |
| 🔄 **Reverse Order · OP Only** | The sorting fallback chain is fully wired up; works stably when entering from Favorites or Search |
| 🎯 **Notifications Jump to Original Thread** | "Replies to Me" jumps straight to the corresponding floor in the original thread — full context at a glance |
| 🎬 **Smooth Video Playback** | Tieba video http→https auto-upgrade + bdstatic allowlist; the black box is fully cracked |
| 🛡️ **Zero Navigation Crashes** | All routing traps across the project have been swept; long-press copy / forum rules / in-forum search are rock solid |
| 🔍 **Smart Update Check** | Built-in GitHub Release probe; one-tap cloud version comparison with direct APK download |
| 🖼️ **Batch Image Download** | PhotoView multi-select mode: pick across pages and save all selected images in one tap |
| 📋 **In-App Log Window** | One tap from the About page; records key operations in real time with copy & share for troubleshooting |
| 🚫 **Zero Ads** | No banners, no promotions, no feed ads — a pure browsing experience |
| 🎨 **Material Design** | Built with Jetpack Compose; native Material You dynamic theming |
| ⚡ **Light & Smooth** | APK is only ~10MB, adaptive refresh rate, no redundant features |
| 🔒 **Privacy First** | No tracking, communicates only with the Baidu Tieba API |
| 📱 **Exceptional Generation Coverage** | Spans Android 5.0 (API 21) to Android 16 (API 36) — 11 years of OS versions, with 16KB page alignment |
| 👥 **Multi-Account Switching** | Tap your avatar to switch accounts instantly; full multi-account management |
| 📐 **Content Density** | Compact / Standard / Comfortable spacing modes |
| 💾 **Offline Cache** | Automatically loads cached thread lists when the network drops |
| 🏗️ **Modern Tech Stack** | Kotlin 2.0.21 + Compose BOM 2024.12 + Hilt + Protobuf |
| 📦 **One-Tap Install** | Download the APK directly from the [Releases page](https://github.com/qaqmin/TiebaLite/releases/latest) |

---

## 📖 Introduction

TiebaLite is an **unofficial** Baidu Tieba Android client, written in Kotlin with a Jetpack Compose UI. It supports an exceptionally long device range from **Android 5.0 to Android 16** with full 16KB page alignment. At its core it is a reverse-engineering implementation of the Baidu Tieba API, aiming to provide the lightest, cleanest Tieba client without sacrificing the core browsing experience.

> **⚠️ Notice:** This software and its source code are for learning and communication only. Commercial use is strictly prohibited. Not affiliated with Baidu, Inc.

## 🆕 v4.0.60 — Album Doesn't Exit Fix (Issue #1 Root Cause)

> ### 🖼️ Confirming a Selection Now Returns to the Reply
> Fixed "the image gets inserted, but the album page stays open" — the original issue #1 symptom, reproduced on both phone and emulator: the picker was being started **twice** per tap. The global event bus subscription used unstable effect keys, so during recomposition two collectors overlapped and one event reached the launcher twice, stacking two picker activities; after confirming the selection in the top one, the empty instance underneath was revealed. The bus subscription is now a single stable collector, and the launch is guarded against re-entry.

## 🆕 v4.0.59 — First-Pick Reliability Fix (Issue #1 Follow-Up)

> ### 🖼️ The First Pick Now Works
> Fixed "the first image selection always fails, the second works" on real devices (reproduced on Redmi K70 / HyperOS): when the picker opens the app goes to the background, and ROMs may reclaim the process or recreate the activity — the panel's request id then changes and the picker result (carrying the old id) was filtered out and dropped. Picker results are no longer filtered by id (the global event has a single consumer).
> v4.0.58 fixed the emulator (Android 12) picker-not-resolvable case; this version fixes the real-device first-pick failure.

## 🆕 v4.0.58 — Photo Picker Compatibility Fix

> ### 🖼️ Adding Images Works Everywhere
> Fixed "cannot add images" on Android 12 ROMs (e.g. MuMu emulator) that ship the photo picker module but whose picker activity cannot be resolved — the app now checks the intent resolves first and falls back to the built-in picker instead of doing nothing.
> Real devices (Android 13+) using the system photo picker are unaffected.

## 🆕 v4.0.57 — Reply & Sub-Floor Image Fixes (Issue #1)

> ### 🖼️ Images That Actually Show Up
> Fixed "adding an image to a reply does nothing": the picker-result subscription had a recomposition race (unstable keys caused repeated re-subscribes, dropping events in the gap with no collector) — now a single stable collection, with logs for empty results.
> Sub-floor images no longer render blank on bad data: blank URLs and invalid sizes are guarded and logged.
> If issues persist, please share app logs (About → View logs → Copy) — key paths now emit diagnostics.

## 🆕 v4.0.56 — Update Channel Migrated to the New Account

> ### 🔀 Channel Migration
> The in-app update check's GitHub channel now points at the new account **qaqmin/TiebaLite** (the previous account qaqmin09577 was banned); the Codeberg fallback channel is unchanged.
> README download links, badges, and the Issues entry have been updated to the new account as well.

## 🆕 v4.0.55 — Update Check Hardening · Cleaner Builds

> ### 🔄 Smarter Update Checks
> Update checks now use ETag conditional requests — 304 responses don't count against GitHub's anonymous rate limit (60/hour/IP), so far fewer users will hit 403s; failures still fall back to the Codeberg mirror automatically.
>
> ### 🧹 Cleaner Builds
> Gradle's clean task now wipes every module's build directory (previously only the root's), so APKs from earlier builds can no longer leak into release artifacts.

## 🆕 v4.0.54 — Account Migration · GitHub as Primary Channel

> ### 🏠 New Home: qaqmin09577/TiebaLite
> The project has fully migrated to the new GitHub account after the original min09577 account was banned for reasons beyond our control (its repositories return 404).
> The default in-app update channel now points to GitHub, while the Codeberg mirror continues publishing in parallel as a fallback channel;
> the About page and project metadata are updated accordingly, and the signing key is unchanged — overwrite updates carry over seamlessly.

## 🆕 v4.0.53 — Build Pipeline Switch · Stable Output from the Mac Remote Builder

> ### 🛠️ Hardened Release Pipeline
> The build pipeline moved to the Mac remote build machine (a stable output line during the Codeberg CI turbulence);
> a full release build completes in about 27 minutes with signatures byte-identical to local builds.

## 🆕 v4.0.52 — Semantic Versioning · Dual-Channel In-App Updates

> ### 🔄 Dual Upgrade for Versioning and Update Experience
> The version scheme switched from the v4.0.0-ai.N preview series to the semantic v4.0.N series.
> In-app updates now support dual channels (Codeberg / GitHub), selectable from the About page;
> a failed startup update check silently falls back to the secondary channel, and the stable channel filters the latest official release from the releases list.

## 🆕 v4.0.0.50 — Full Topic List Page Now Clickable

> ### 🧭 "More Topics" Is Now Tappable Too
> Completed the second entry to the topic ranking: items in the full list page now jump to the search page,
> sharing the same transition as in-page topic cards, with special characters preserved intact (#39).
> The hot topic ranking is now fully clickable end to end, with no dead zones left.

## 🆕 v4.0.0.49 — Hot Topic Cards Now Clickable

> ### 🔥 Tap a Topic, Get Results
> Fixed hot topic ranking cards not responding to taps: tapping a topic card now jumps
> to the search page and searches that topic, with special characters in topic names jumping losslessly (#39).

## 🆕 v4.0.0.48 — Full Rendering of Image Comments in Sub-Floors

> ### 🖼️ Images in Sub-Flors Are Finally Visible
> After upgrading the API client version number, image comments now deliver full image data —
> real image rendering plus tap-to-enlarge, no more "[image]" placeholders (#27).
> Unknown content types get a logging fallback so future official data format changes stay observable.

## 🆕 v4.0.0.47 — Home Bottom Bar Navigation Adaptation

> ### 📱 The Home Bottom Bar Behaves in Dark Mode Too
> The home page bottom navigation bar now adapts to the system navigation bar: the background color extends into the safe area,
> and dark mode no longer shows transparent seams; gesture navigation gets a deterministic 28dp fallback (#36).
> Three-button navigation devices see no visual change; every home tab stays rock solid.

## 🆕 v4.0.0.46 — Immersive Image Transitions + List Scroll Position Memory

> ### 🖼️ No Jumps When Viewing Images, No Startle When Immersing
> The image viewer now uses system transitions for enter/exit: the status bar fades smoothly,
> and the share panel keeps the immersive state across round trips — no more "jumping" (#34).
>
> ### 📍 Visit a User Page, Come Back Right Where You Were
> Sub-floor and main floor lists now remember scroll position —
> tap into a profile and return, and you land where you left off instead of hunting again (#8).

## 🆕 v4.0.0.45 — Sub-Floor Reply Binding Fix + Forum Name Routing Hardening

> ### 🎯 Whoever You Reply To, Is Who You Reply To
> Fixed sub-floor reply downgrade: the reply route now consumes subPostId,
> precisely locking onto the target comment inside a sub-floor instead of downgrading to the floor (follow-up hardening for PR#33).
>
> ### 🛡️ Special Forum Names Don't Break Routing
> Forum name parameters in three reply navigation paths are now encoded, so special characters like
> `&` / `#` / spaces no longer break routing — fully aligned with the established encoding convention.

## 🆕 v4.0.0.44 — Custom Startup Page + Image Placeholders in Sub-Floors

> ### 🚀 Open Straight to the Page You Want
> New "Startup Page" setting: Home / Feed / Messages / Me — pick any one as the default startup page,
> with linked fallback for the "hide feed entry" option; no configuration goes out of bounds (#30).
>
> ### 🖼️ Not a Single Image Comment Is Lost
> Content rendering now covers the new image type, so image comments are no longer silently dropped;
> the preview row gains a "[image]" placeholder, and detail-page images support tap-to-enlarge (#27).

## 🆕 v4.0.0.43 — Floor Reply Positioning Fix + Reply Page Status Bar Avoidance

> ### 💬 Every Reply Lands in Its Proper Floor
> Fixed "cannot become the first sub-floor": floor reply positioning parameters are no longer lost;
> tapping "Comment" or long-pressing "Reply" now goes straight to that floor's sub-floor instead of accidentally posting to the main thread (#26).
>
> ### 📐 The Reply Page Top Bar No Longer Fights the Status Bar
> Reply editor top avoidance is now complete, keeping a safe distance between the title and the status bar,
> with consistent behavior in light and dark themes; three reply paths pass on-device regression with zero crashes (#29).

## 🆕 v4.0.0.42 — Full Presentation of the Likes List

> ### 👍 Every Like You Received, Clearly Visible
> Fixed the blank "Received Likes" list: a dedicated data model was built for like messages,
> fully presenting the liker's avatar, nickname, time, and "liked your thread: title",
> verified on two real devices with full real-data rendering and zero crashes (#16).
>
> ### 🧭 Every Like Can Be Traced to Its Source
> Tap an item to jump to the liked thread, tap an avatar to enter the liker's profile;
> empty-state fallback, refresh and pagination deduplication, and null-value hardening were added in sync,
> aligning the list experience fully with "Replies to Me".

## 🆕 v4.0.0.41 — Second Round of Gesture Hot Zone Hardening

> ### 🎯 No Longer Dependent on System Reporting — Avoidance Now "Deterministic"
> Some ROMs (ColorOS 16, etc.) report both `navigationBars` and
> `safeGestures` as 0 under gesture navigation, invalidating inset-based avoidance.
> A unified bottom-bar safe-area component is now introduced: real values take priority, and **when both are zero, a deterministic 28dp gesture hot zone height is used as fallback**,
> with clicks actively consumed inside the avoidance area to prevent pass-through.
>
> ### 📋 The "More" Popup Is Hardened Too
> The mis-tapped "OP Only / Favorite" options live in the bottom popup — gesture hot zone avoidance is now added there as well,
> lifting the popup content entirely out of the hot zone (#20).

## 🆕 v4.0.0.40 — Report Jump Fix + Full-Screen Gesture Avoidance

> ### 🧭 The Reporting Flow, One Step to the Finish
> Fixed missing interpolation in the web redirect after a successful report, and brought the redirect URL
> under the unified encoding convention, restoring the full closed loop of the reporting flow.
>
> ### 🖐️ Gesture Navigation, No More Mis-Taps at the Bottom
> Tapping the bottom of a thread under full-screen gestures used to mis-trigger "OP Only / Favorite" — the root cause was the bottom bar avoidance
> only accounting for the system navigation bar and missing the gesture hot zone. Both are now avoided
> (`navigationBars ∪ safeGestures`), with correct behavior for gesture and three-button navigation alike;
> sub-floor pages are hardened in sync (#20).

## 🆕 v4.0.0.39 — Special Character Routing Hardening

> ### 🛡️ Copy and External Links, Now Rock Solid
> Copying a reply containing links, or tapping web links inside threads (cloud drive links, etc.) caused instant crashes —
> the root cause was links and special characters (`/ ? & :`) being concatenated into navigation routes without URI encoding,
> breaking route matching (#19).
>
> ### 🔐 End-to-End Encoding, Lossless Round Trips
> Copy dialogs and web route parameters are now uniformly passed through `Uri.encode`, closing the loop across six call sites,
> covering floor copy, sub-floor copy, external link jumps, and video cards; the reading side is auto-decoded by the framework,
> restoring Chinese, spaces, emoji, and full URLs exactly as they were.

## 🆕 v4.0.0.38 — Startup Stability Refactor + Favorites Preference Wiring

> ### 🚀 Cold-Start Crash Root Cause Fixed (Community Contribution Merged · Thanks @wufeng5702)
> The "crashes on open" plague on devices like Redmi K50 / Note 11T Pro / Nothing Phone 2 is settled:
> the root cause was `LocalNavigator` not being provided in the top-level composition scope, leaving the navigation context dangling on some startup paths.
> It is now globally injected at the top level of `MainActivityV2` via `CompositionLocalProvider`,
> with `NotificationsPage` passing parameters explicitly, `HotPage` eliminating parameter shadowing, and over twenty read sites fully covered (#6 / #9 / #17).
>
> ### 🧬 Java 21 API Forward Compatibility
> `removeFirst()` / `removeLast()` are uniformly replaced with equivalent `removeAt()` forms,
> avoiding `NoSuchMethodError` when builds from newer JDKs run on older Android runtimes.
>
> ### 🎛️ Favorites Browsing Preferences Now Take Effect End to End
> The two switches "default OP Only when entering from Favorites / default reverse browsing" previously existed only in definition and never participated in initialization.
> The `from=FROM_STORE` route passing chain (Routes → MainActivity → Favorites page jump) is now wired up,
> applying preferences on thread entry; refresh / retry / continue-reading all carry the favorites context without fallback, and manual switching still applies instantly (#3).

## 🆕 v4.0.0.37 — Replies Go Straight to Your Own Profile

> ### 🎯 Profile "Replies" Entry Precisely Placed
> The "Replies" count in the profile previously misused the local database primary key `account.id` as the Tieba UID,
> leading to other people's profiles. It now uses the real Tieba UID `account.uid`.
>
> ### 📑 Precise Tab Positioning
> Tapping "Replies" now goes straight to the "Replies" tab of the user detail page (`user/{uid}?tab=1`),
> with the three-level Composable function parameter chain (Page → Content → Normal/Expanded) fully wired — no manual switching needed.

## 🆕 v4.0.0.35 — Reply Taps Restored

> ### 🎯 Profile "Replies" Entry Enabled
> The "Replies" count in the profile previously did nothing; jump logic is now wired in:
> tap → straight into the second-level menu user detail page, reusing the existing Replies tab.
>
> ### 🔗 Second-Level Menu Reply Taps Unified
> Tapping content in the second-level menu "Replies" list (the red-box area) used to crash on routing literal traps:
> - `subposts/0` (hardcoded threadId=0) → SubPostsPage load failure
> - `thread/threadId` (literal instead of variable) → route resolution crash
>
> **Fix strategy: uniformly changed to `thread/{threadId}?postId={postId}&scrollToReply=true`**
> Tapping reply content is now the same as tapping the thread itself — both go straight to the corresponding floor in the original thread.

## 🆕 v4.0.0.34 — Scheduled Check-In Clock Calibration

> ### ⏰ Cross-Day Postponement Mechanism
> Occasional missed scheduled check-ins previously traced back to the `initAutoSign` time judgment logic:
> - Today's target time already passed → no alarm set at all (missed check-in)
> - After the alarm fired, reschedule judged it as "already passed" due to millisecond-level error → no appointment for the next day
>
> **Fix strategy:**
> - Zero out seconds / milliseconds to eliminate judgment error
> - If the target time has passed, postpone to the next day so the alarm is always valid
> - Covers all three call sites: app startup / alarm reschedule / boot restart
>
> From now on, scheduled check-ins fire on time and run stably around the clock.

## 🆕 v4.0.0.33 — Sorting and OP-Only: Fallback Fully Wired

> ### 🔄 `forumId` Fallback Chain Fix
> ThreadPage had two forumId variables long mixed up:
> - `forumId` (route parameter; null in Favorites and similar scenarios)
> - `curForumId` (`forumId ?: forum.id` fallback value)
>
> The reverse-order button, OP-only, load-more, LoadPrevious, onRefresh, onReload, immersive mode, and more — **10 places** — all misused the bare `forumId`,
> so entering a thread from Favorites or Search gave `forumId=null → forum_id=0`,
> the API returned an error, and sorting failed.
>
> **All migrated to `curForumId`**; once the Tieba data structure finishes loading, a valid forum ID is available and the sorting chain is fully connected.

### 📍 Where to Find Reverse Browsing

Step 1: after entering a thread, tap the **"⋮"** menu button on the right of the bottom comment bar:

![Reverse browsing - Step 1](docs/images/draft_reverse_step1.png)

Step 2: find the **"Reverse Browsing"** button in the popup menu and tap it to take effect:

![Reverse browsing - Step 2](docs/images/draft_reverse_step2.png)

## 🆕 v4.0.0.32 — Notification Replies Return to Position

> ### 🎯 Notifications Jump Straight to the Original Thread Context
> Tapping a notification in "Replies to Me" previously tried to jump directly to the sub-floor (SubPostsPage), but notification data has no `forumId` field,
> so the SubPostsPage API call failed → blank page.
>
> Fix: **jump to the original thread and auto-scroll to the corresponding floor**. `thread/{threadId}?postId={postId}&scrollToReply=true`
> with all three parameters present, reusing the earlier floor positioning capability — one tap, right on target.
>
> User experience: notification tap → original thread → auto-scroll to the replied floor → full context at a glance, no more blanks.

## 🆕 v4.0.0.31 — Video Playback Pipeline Refactor

> ### 🎬 Double Insurance Cracks the Video Black Box
> Tieba video links are mostly plain http, and the CDN domain `bdstatic.com` was not in the network security allowlist,
> so the system layer silently intercepted them → pitch-black video.
>
> **Double insurance fix:**
> - **Network config layer**: added `bdstatic.com` to `network_security_config.xml`, expanding the plaintext allowlist
> - **Player layer**: `DefaultVideoPlayerController` auto-upgrades network video sources from http to https
>
> Whether http or https direct links, everything plays steadily. Video experience on Xiaomi, Redmi, Samsung, and other mainstream devices is restored across the board.

## 🆕 v4.0.0.30 — Long-Press Menu Crash Full Sweep

> ### 🔪 Rounding Up the Routing Literal Traps
> Several past iterations scattered typos of the form `navigator.navigate("Routes.XXX/$it")` — the route constant name was concatenated into the URL as literal text, crashing Compose resolution.
> Seizing on one long-press "copy" error, the whole project was grep-audited, **collecting 5 similar hazards in one pass**:
> - `SubPostsPage.kt` × 2 (sub-floor copy entries)
> - `ThreadPage.kt` × 2 (main floor / floor copy entries)
> - `ForumThreadListPage.kt` × 1 (forum rules entry)
>
> All changed to correct forms like `navigator.navigate("copy_dialog/$it")`.
> "Copy" now lands firmly, with no side leakage.

## 🆕 v4.0.0.29 — In-Forum Search Instant Penetration

> ### 🔎 Side Leakage Fix: the Variable Literal Trap
> In-forum search navigation previously passed pure string literals — `forumName` and `forumId` were passed as literal text rather than variable evaluation.
> The route received the two characters "forumName" instead of the actual forum name, crashing resolution.
> A single-line `$` interpolation fix makes variables evaluate immediately, and the search entry is solid again.

## 🆕 v4.0.0.27 — Drafts: Precise Floor Recall

> ### 📝 Smart Draft Engine
> Reply content auto-saves to disk in real time; exit and it's stored. The drafts list shows **forum name + content preview + save time**, and tapping jumps straight to **the corresponding floor in the original thread** —
> no longer vague fragments, but a precise moment of context.
>
> ### 🔍 Hierarchical Linked Routing Fix
> Sub-floor, reply page, and thread jump — all three layers of route parameters are now fully wired. `forumId` / `postId` / `subPostId` are no longer lost halfway,
> and every jump lands precisely on the target.
>
> ### 📋 In-App Log Panel
> About page → "View Logs" at the bottom → real-time operation stream with one-tap copy & share. No more guessing when troubleshooting.

## 🆕 v4.0.0.19 — Smart Update Check + High-Priority Scheduled Check-In

> ### 🔍 Smart Version Patrol
> A GitHub Release probe is built into the About page for one-tap cloud version comparison. It smartly strips build hash suffixes (`+sha`) and precisely matches semantic version numbers.
> On finding an upgrade it offers **direct download** and a **GitHub entry** — safe system-browser jumps, with the whole exception path guarded by `try-catch` to eliminate crashes.
>
> ### ⏰ High-Precision Scheduled Alarm
> `AlarmManager.setExactAndAllowWhileIdle` (Android 12+) replaces the old `setRepeating`.
> Precise wake-up from sleep: even if the process is swiped away and the device sinks into Doze, the system still force-raises the check-in service on time.
> The next day's alarm is automatically reserved once check-in completes — **zero background residency, on time around the clock**.

## 🆕 v4.0.0.16 — Sub-Floor Fix + Overwrite Install

- 🏷️ **Overwrite install is ready** — signing keys are unified in-repo; seamless updates from now on
- 🐛 Fixed blank display of "View All Replies" in sub-floors (missing route parameters)

## 🆕 v4.0.0.13 — Batch Image Download

> 🖼️ PhotoView multi-select mode: tap multi-select → pick across pages → save all selected images to the gallery in one tap

## 🆕 v4.0.0.12 — Performance Optimizations

- ⚡ Removed blocking `RateLimitInterceptor` / `RetryInterceptor`; restored native OkHttp retries
- 🚀 Scroll smoothness restored, fixing the performance regression introduced in v4.0.0.5
- 🗑️ Removed unused temporary data models like `OneKeySignInBean`

## 🆕 v4.0.0.11 — Unified Signing Key

- 🔧 Fixed inconsistent signing keys forcing an uninstall + reinstall on every update
- ✅ **From this version on, all subsequent versions can be installed directly as overwrite updates — no need to uninstall the old version**

## 🆕 v4.0.0.10 — The Full Check-In Era

> ### ⚡ One-Tap Full Check-In
> The smart pagination engine pulls in every forum you follow, one by one, **covering up to 3000 forums in a single run**.
> Say goodbye to the official client's "only the first 100 forums" limit — not a single forum is missed.
>
> ### 🔋 ⚠️ Important: Please Disable Battery Optimization!
> The check-in process needs to keep running in the background. **Be sure to set Tieba Lite's battery optimization policy to "Unrestricted"**,
> otherwise the system may force-sleep the process mid-check-in, interrupting it.
>
> **Settings path:** System Settings → Apps → Tieba Lite → Battery → Unrestricted
>
> *(Paths vary slightly by brand: Xiaomi - App info → Battery saver → Unrestricted / Huawei - App launch → Manual management / OPPO-vivo - Power consumption protection → Allow background running)*

## 🆕 v4.0.0.5 Release Notes

- 🎮 Adaptive high refresh rate — automatically matches your device's highest refresh rate
- 👥 Fast multi-account switching — tap the avatar in the user page to pop up the account menu
- 📐 Content density options — Compact / Standard / Comfortable spacing
- 💾 Offline cache — automatically loads cached threads when disconnected
- 🚀 Compose performance optimizations — Strong Skipping + stability markers
- 🔧 ProGuard R8 full mode + ABI trimming
- 🛡️ API stability enhancements — request retries + rate limiting
- 🖼️ Image download prompts — save to gallery
- ✍️ Draft reminders — unsent reply count display
- 📋 Check-in enhancements — merges dual data sources, covering more followed forums

## 💡 Pro Tips

| Scenario | Advice |
|------|------|
| 🔋 **Full check-in** | Be sure to set this app's battery optimization to "Unrestricted" in system settings to avoid background process kills interrupting the check-in |
| ⏰ **Scheduled auto check-in** | Once enabled, no need to keep the app in the background; Doze sleep still wakes it on time — just ensure the battery policy is unrestricted |
| 📝 **Draft auto-save** | Exit while replying and it's saved; drafts can be restored anytime. New-version drafts carry full context (forum + floor) and jump straight there on tap |
| 🔄 **In-app updates** | About page → Check for updates → download the Release APK; after downloading, tap the notification to install (no need to uninstall the old version) |
| 📋 **Troubleshooting aid** | About page → View logs → copy from the top right → attach logs when filing issues for much faster fixes |
| 🌐 **Multi-language** | Full UI and documentation in four languages: Chinese, Japanese, Korean, and English |

## 💬 Feedback & Contribution

🐛 Found a bug? 💡 Have ideas? Submit them via GitHub Issues:

<p align="center">
    <a href="https://github.com/qaqmin/TiebaLite/issues">
        <img alt="GitHub Issues" src="https://img.shields.io/github/issues/qaqmin/TiebaLite?style=flat&color=red&label=%F0%9F%90%9B%20Bug%20%2F%20Feature">
    </a>
</p>

> Please include **app logs** when submitting (About page → View logs → Copy) — the more detail, the faster the fix.

## 👨‍💻 Original Author

This project was originally developed and maintained by **[HuanCheng65](https://github.com/HuanCheng65)** until it was archived in 2024.

> 🙏 **All original code, architecture design, and core contributions belong to the original author; we express our sincere respect for their hard work.**

## 🔗 Related Projects

+ [Starry-OvO/aiotieba: Asynchronous I/O Client for Baidu Tieba](https://github.com/Starry-OvO/aiotieba)
+ [n0099/tbclient.protobuf: Baidu Tieba Client Protocol Buffers definitions](https://github.com/n0099/tbclient.protobuf)

---

## 📋 Version History

| Version | Date | Description |
|---|---|---|
| v4.0.0-beta.1 | 2024-02-02 | Original release by HuanCheng65 |
| v4.0.0.1 | 2026-06-09 | Documentation improvements, four-language disclaimer |
| v4.0.0.2 | 2026-06-09 | Security fix, network security config |
| v4.0.0.3 | 2026-06-09 | CI fix, build success |
| v4.0.0.4 | 2026-06-12 | AGP 8.5.2 + Gradle 8.7 + source fixes |
| v4.0.0.5 | 2026-06-13 | compose-destinations removal + Kotlin 2.0.21 + full dependency upgrades |
| v4.0.0.6 | 2026-06-14 | Fast multi-account switching + content density + offline cache |
| v4.0.0.7 | 2026-06-14 | API stability enhancements (retry + rate limiting) + image download prompts |
| v4.0.0.8 | 2026-06-14 | Full check-in upgrade (first attempt) |
| v4.0.0.9 | 2026-06-14 | Check-in architecture rewrite · paginated fetching of the full followed forum list |
| v4.0.0.10 | 2026-06-14 | **🎉 One-Tap Full Check-In · 3000+ Forums Covered** — pagination engine + failure skip tolerance |
| v4.0.0.11 | 2026-06-15 | Unified signing key; overwrite installs without uninstting from now on |
| v4.0.0.12 | 2026-06-15 | Performance optimizations · removed blocking interceptors · scroll smoothness restored |
| v4.0.0.13 | 2026-06-15 | **🖼️ Batch Image Download** — PhotoView multi-select + one-tap save |
| v4.0.0.15 | 2026-06-15 | keystore allowlist fix · overwrite install enabled |
| v4.0.0.16 | 2026-06-15 | Sub-floor blank fix · route parameter completion |
| v4.0.0.19 | 2026-06-17 | **🔍 Smart Update Check + ⏰ Scheduled Check-In** — setExactAndAllowWhileIdle + direct links · Doze-wakeable |
| v4.0.0.22 | 2026-06-17 | **📦 Release Channel Fix** — in-app upgrades now download the official APK instead of the debug build |
| v4.0.0.24 | 2026-06-17 | **📝 Full Draft Box Implementation** — Draft model extension · list browsing · tap to jump |
| v4.0.0.27 | 2026-06-17 | **🎯 Draft Positioning + Log Panel** — tap straight to the corresponding floor · real-time log window on the About page |
| v4.0.0.29 | 2026-08-16 | 🐛 **In-Forum Search Crash Fix** — variable interpolation gap fixed · one character changed, global stability |
| v4.0.0.30 | 2026-08-16 | **🔪 Route Literal Traps Rounded Up** — 5 `Routes.` typos found project-wide via grep · long-press copy restored |
| v4.0.0.31 | 2026-08-16 | **🎬 Video Playback Fix** — bdstatic allowlist + player http→https double insurance |
| v4.0.0.32 | 2026-08-16 | **🎯 Notification Positioning Restored** — "Replies to Me" jumps to the original thread · auto-scroll to the corresponding floor |
| v4.0.0.33 | 2026-08-16 | **🔄 Sorting Fallback Fully Wired** — 10 `forumId` → `curForumId` sites in ThreadPage · reverse order restored |
| v4.0.0.34 | 2026-08-16 | **⏰ Scheduled Check-In Clock Calibration** — cross-day postponement + millisecond zeroing · three call sites unified |
| v4.0.0.35 | 2026-08-16 | **🎯 Reply Taps Restored** — profile entry enabled + second-level menu reply taps jump uniformly |
| v4.0.0.36 | 2026-08-16 | **🎯 Replies Go Straight to Your Profile** — account.uid fix + user/{uid}?tab=1 positioning |
| v4.0.0.37 | 2026-08-16 | 🛠️ **Build Fix** — initialTab three-level function parameter chain completed |
| v4.0.0.38 | 2026-08-30 | 🚀 **Startup Stability Refactor** — top-level LocalNavigator injection root-fixes cold-start crashes · favorites preferences wired · community contribution #15 merged |
| v4.0.0.39 | 2026-08-31 | 🛡️ **Special Character Routing Hardening** — crash on copying link content / external link jumps root-fixed · Uri.encode unified across six call sites |
| v4.0.0.40 | 2026-08-31 | 🧭 **Report Jump Fix + Gesture Avoidance** — report success web jump restored · bottom bar gesture hot zone avoidance · mis-taps root-fixed |
| v4.0.0.41 | 2026-09-01 | 🖐️ **Second Round of Gesture Hot Zone Hardening** — deterministic 28dp fallback when both insets are zero · "More" popup avoidance completed |
| v4.0.0.42 | 2026-09-02 | 👍 **Full Likes List** — "Received Likes" blank root-fixed · items jump to threads/profiles · empty-state fallback and deduplication |
| v4.0.0.43 | 2026-09-05 | 💬 **Floor Reply Positioning Fix** — floor replies go straight to sub-floors · reply page status bar avoidance · on-device regression of three paths |
| v4.0.0.44 | 2026-09-07 | 🚀 **Custom Startup Page + Sub-Floor Image Placeholders** — four startup page choices · linked fallback for hiding feed · image comments never lost · tap-to-enlarge |
| v4.0.0.45 | 2026-09-08 | 🎯 **Sub-Floor Reply Binding Fix** — reply targets precisely locked · special forum name routing hardened |
| v4.0.0.46 | 2026-09-08 | 🖼️ **Immersive Image Transitions + Scroll Position Memory** — smooth status bar transitions · immersive share panel · return to where you were |
| v4.0.0.47 | 2026-09-09 | 📱 **Home Bottom Bar Navigation Adaptation** — dark mode overlap fixed · background extends into safe area · gesture hot zone fallback |
| v4.0.0.48 | 2026-09-10 | 🖼️ **Full Rendering of Image Comments in Sub-Floors** — API version upgrade · real image rendering + tap-to-enlarge · unknown type logging fallback |
| v4.0.0.49 | 2026-09-09 | 🔥 **Hot Topic Cards Now Clickable** — topic cards jump to search · special characters lossless · no regression in bare navigation / deepLink |
| v4.0.0.50 | 2026-09-11 | 🧭 **Full Topic List Page Now Clickable** — "More Topics" entry completed · search jump transition · no dead zones end to end |
| v4.0.0-ai.50 | 2026-09-16 | 🧪 **First ai Preview Channel Release** — full topic list page clickable · preview channel on its own version line, stable channel unaffected |
| v4.0.0-ai.51 | 2026-09-22 | 🧪 **First Fully Green CI Preview** — Android SDK installed on runners · jitpack TLS retries · ButterKnife leftovers removed · publishing moved to the native Codeberg API |
| v4.0.52 | 2026-09-23 | **🔢 Semantic Versioning Series** — version line switched to v4.0.N · stable channel filters the latest official release from the releases list |
| v4.0.53 | 2026-10-04 | **🛠️ Mac Remote Build Line** — builds moved to the Mac build machine (a stable output line during CI turbulence) · signatures byte-identical to local builds |
| v4.0.60 | 2026-10-16 | **🖼️ Album Doesn't Exit Fix** — picker no longer started twice per tap (fixes "image inserted but album stays open"; issue #1 root cause) |
| v4.0.59 | 2026-10-16 | **🖼️ First-Pick Reliability** — picker results no longer filtered by request id (fixes "first selection always fails" on HyperOS/Redmi K70; issue #1 follow-up) |
| v4.0.58 | 2026-10-16 | **🖼️ Photo Picker Compatibility** — intent resolvability check with Matisse fallback (fixes "cannot add images" on Android 12 ROMs like MuMu) |
| v4.0.57 | 2026-10-16 | **🖼️ Reply & Sub-Floor Image Fixes** — picker subscription race fixed (stable collection) · blank-URL/size guards with logging (issue #1) |
| v4.0.56 | 2026-10-07 | **🔀 Update Channel Migrated** — GitHub update channel now points at qaqmin/TiebaLite (previous account banned) · README links updated to match |
| v4.0.55 | 2026-10-07 | **🔄 Update Check Hardening** — ETag conditional requests (304s bypass GitHub's anonymous limit) + automatic 403 fallback · clean task fix (stale APKs no longer leak into artifacts) |
| v4.0.54 | 2026-10-04 | **🏠 Migrated to New Account qaqmin09577** — default update channel switched to GitHub · Codeberg becomes fallback · dual-track publishing · overwrite updates unaffected |
| [▶ Latest Release](https://github.com/qaqmin/TiebaLite/releases/latest) | | **← Download APK here** |

---

## 🛠️ Build Instructions

### Prerequisites

- **JDK 17+**
- **Android SDK** with **compileSdk 34**
- Android Studio (recommended)

### Signing Configuration

Create a `keystore.properties` file for release signing:

```properties
storeFile=your_keystore_file.jks
storePassword=your_store_password
keyAlias=your_key_alias
keyPassword=your_key_password
```

> ⚠️ **Note:** Do NOT commit the `keystore.properties` file to version control.

### Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

Build outputs are located in the `app/build/outputs/apk/` directory.

---

## ⚠️ Disclaimer

<details>
<summary>🇨🇳 中文</summary>

1. 本软件为**非官方**贴吧客户端，与百度公司无任何关联。
2. 本软件及源码**仅供学习交流使用，严禁用于商业用途**。
3. 使用本软件所产生的一切后果由使用者自行承担。
4. 本软件不保证功能的完整性和稳定性。

</details>

<details>
<summary>🇯🇵 日本語</summary>

1. 本ソフトウェアは**非公式**の贴吧クライアントであり、百度社とは一切の関係がありません。
2. 本ソフトウェアおよびソースコードは**学習・交流のみを目的としており、商業利用は厳禁**です。
3. 本ソフトウェアの使用により生じる一切の結果は、使用者自身が責任を負います。
4. 本ソフトウェアは機能の完全性と安定性を保証するものではありません。

</details>

<details>
<summary>🇰🇷 한국어</summary>

1. 본 소프트웨어는 **비공식**贴吧 클라이언트이며, 바이두와는 아무런 관련이 없습니다.
2. 본 소프트웨어およびソースコード는 **학습 및 교류 목적으로만 사용되며, 상적 사용은 엄격히 금지**됩니다.
3. 본 소프트웨어의 사용으로 인해 발생하는 모든 결과는 사용자가 책임을 집니다.
4. 본 소프트웨어는 기능의 완전성과 안정성을 보장하지 않습니다.

</details>

<details>
<summary>🇺🇸 English</summary>

1. This software is an **unofficial** Tieba client and is not affiliated with Baidu, Inc.
2. This software and its source code are **for learning and communication purposes only. Commercial use is strictly prohibited**.
3. All consequences arising from the use of this software are borne by the user.
4. This software does not guarantee the completeness and stability of its features.

</details>

---

<p align="center">
    <sub>Original Author: <a href="https://github.com/HuanCheng65">HuanCheng65</a> | License: GPL v3</sub>
</p>
