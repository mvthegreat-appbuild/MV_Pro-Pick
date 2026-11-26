# StockPro Positional (Android)

Native Kotlin & Jetpack Compose Android application for tracking StockPro positional trade calls and Motilal Oswal delivery/overnight ideas.

## Features

- **Dual Feed Tabs**: Switch between **StockPro** positional calls and **Motilal Oswal** delivery/overnight ideas, with tab selection persisted via DataStore Preferences.
- **Live Sync & Offline Fallback**: Fetches live trade feeds (`positional_trades.json`, `motilal_trades.json`) and optional `cmp.json` prices from GitHub Raw, caches responses locally for offline viewing, and bundles initial JSON snapshots in `app/src/main/assets/`.
- **Auto-Refresh & Pull-to-Refresh**: Supports pull-to-refresh, an explicit Refresh action, foreground resume checks, and automatic 5-minute background refresh cycles.
- **Live Trade Evaluation**: Resolves NSE tickers, evaluates entry/active/target/stop-loss status against CMP, highlights achieved target chips, and calculates Move %, Risk %, upside to T1/last target, and Risk-to-Reward ratios.
- **Search & Post Links**: Normalized search across stock names and NSE ticker symbols, plus one-tap links to open original Telegram posts.

## Tech Stack

- Kotlin, Jetpack Compose, Material 3
- OkHttp + Kotlinx Serialization
- AndroidX DataStore Preferences & Lifecycle ViewModel
