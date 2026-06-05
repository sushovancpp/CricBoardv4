# 🏏 CricBoard — Android Cricket Scoring App 

A **fully offline** local cricket tournament scorer for Android, built with Java.
No internet needed. All match data stored on-device.

--- 

## ✨ Features

| Feature | Details |
|---|---|
| 🪙 **Toss Management** | Who won, bat or bowl |
| 🏏 **Ball-by-ball scoring** | 0, 1, 2, 3, 4, 6, Wide, No Ball, Wicket |
| 🔄 **Strike rotation** | Auto-rotates on odd runs & end of over |
| 📋 **Full Scorecard** | Batsman stats (R, B, 4s, 6s, SR) + Bowler figures (O, R, W, Econ) |
| 🎳 **Bowler tracking** | Set bowler each over, reuse same bowler |
| ➕ **Batsman entry** | Add new batsmen after wickets |
| 🏆 **Auto result** | Win by wickets / runs / tie detection |
| 🗂️ **Match history** | All past matches saved locally |
| 📍 **Venue tracking** | Optional venue for each match |
| ⚙️ **Flexible overs** | 5, 6, 10, 15, 20, 25, 50 or custom |

---

## 🗂️ Project Structure

```
CricBoard/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/cricboard/
│   │   ├── model/
│   │   │   ├── BallEvent.java       ← Ball type: runs/wide/noball/wicket
│   │   │   ├── BatsmanScore.java    ← Batsman stats
│   │   │   ├── BowlerScore.java     ← Bowler figures
│   │   │   ├── Innings.java         ← Full innings state
│   │   │   ├── Match.java           ← Match with both innings
│   │   │   └── CricketEngine.java   ← Pure cricket logic (no Android deps)
│   │   ├── data/
│   │   │   └── MatchRepository.java ← JSON persistence to /files/matches.json
│   │   └── ui/
│   │       ├── MainActivity.java    ← Match list + FAB
│   │       ├── matches/
│   │       │   ├── NewMatchActivity.java ← Create match form
│   │       │   └── MatchAdapter.java     ← RecyclerView adapter
│   │       ├── scoring/
│   │       │   └── ScoringActivity.java  ← Toss + Live scoring + Innings break
│   │       └── scorecard/
│   │           └── ScorecardActivity.java ← Full scorecard view
│   └── res/
│       ├── layout/
│       │   ├── activity_main.xml
│       │   ├── activity_new_match.xml
│       │   ├── activity_scoring.xml   ← Main scoring screen
│       │   ├── activity_scorecard.xml
│       │   ├── dialog_openers.xml
│       │   ├── dialog_wicket.xml
│       │   └── item_match.xml
│       └── values/
│           ├── colors.xml  ← Dark cricket green theme
│           ├── strings.xml
│           └── themes.xml
```

---

## 🚀 How to Open in Android Studio

1. **Open Android Studio** → "Open an Existing Project"
2. **Select the `CricBoard` folder**
3. Wait for Gradle sync
4. Connect a device or start an emulator (API 24+)
5. Click **Run ▶**

> Minimum SDK: Android 7.0 (API 24)
> Target SDK: Android 14 (API 34)

---

## 📱 App Flow

```
Launch → Match List
           │
           ├── [+] New Match
           │       └── Enter teams, overs, venue → Create
           │               └── Toss Screen
           │                       └── Set openers + bowler → LIVE SCORING
           │                               ├── Ball buttons (0-6, WD, NB, OUT)
           │                               ├── Strike swap
           │                               ├── Add new batsman after wicket
           │                               ├── Set bowler each over
           │                               └── Innings break → 2nd Innings → Result
           │
           └── [Tap match] → Scoring (if live) / Scorecard (if done)
```

---

## 🎨 Design

- **Dark cricket green** theme throughout
- Monospace fonts for scorecard data
- Color-coded ball log: 🔴 Wicket, 🔵 Four, 🟣 Six, 🟡 Wide, 🟠 No Ball
- Large tap targets for scoring buttons (field use friendly)

---

## 💾 Data Storage

Matches are saved as `matches.json` in the app's private files directory.
No permissions required. Data persists across app restarts.
