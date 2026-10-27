# nono app

A standalone **Android TV** IPTV player (package `com.nono.iptv`), built on Media3/ExoPlayer, Leanback, and Room.

## Features in this scaffold
- **Leanback browse UI** — channels grouped into rows by playlist group, one card per channel.
- **Full-screen Media3 player** behind the browse UI; selecting a card tunes to that stream.
- **M3U import** — streaming line-by-line parser for `#EXTINF` metadata (tvg-id, tvg-logo, group-title), fetched over OkHttp with a configurable User-Agent.
- **Room storage** — channels persisted with chunked batch inserts (500/chunk) so large playlists import without blowing SQLite limits.
- **Live-tuned playback** — `DefaultLoadControl` configured for low-latency live streams; HLS, MPEG-TS and progressive handled automatically.

First launch imports a public sample playlist (`iptv-org`) so you see channels immediately; swap `PlaylistRepository.DEFAULT_PLAYLIST_URL` or call `importPlaylist(url)` for your own.

## Project layout
```
app/src/main/java/com/nono/iptv/
├── MainActivity.kt                     # hosts PlayerView + BrowseFragment
├── data/
│   ├── ChannelEntity.kt                # Room entity
│   ├── ChannelDao.kt                   # chunked inserts, group queries
│   ├── AppDatabase.kt                  # Room database
│   └── PlaylistRepository.kt           # OkHttp fetch + import
├── parser/M3uParser.kt                 # streaming BufferedReader parser
├── player/PlayerEngine.kt              # Media3 + OkHttp datasource + live buffering
└── ui/
    ├── ChannelCardPresenter.kt         # Leanback ImageCardView
    ├── ChannelBrowseViewModel.kt
    └── ChannelBrowseFragment.kt        # BrowseSupportFragment (rows = groups)
```

## Build
Requires the Android SDK (API 34) and JDK 17.

```bash
./gradlew assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`. Or open the folder in Android Studio (Koala / AGP 8.5) and Run on an Android TV emulator or device.

> First build downloads AGP, Kotlin, Media3, Room, Leanback, OkHttp and Glide from Google's Maven and Maven Central, so it needs network access.

## Stack
AGP 8.5.2 · Kotlin 1.9.24 · KSP · Media3 1.3.1 · Room 2.6.1 · Leanback 1.0.0 · OkHttp 4.12.0 · Glide 4.16.0
