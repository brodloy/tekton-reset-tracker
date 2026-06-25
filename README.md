![Tekton Reset Tracker](banner.png)

**Count how many Chambers of Xeric Challenge Mode raids you reset at Tekton, and the time you waste doing it.**

[![RuneLite](https://img.shields.io/badge/RuneLite-Plugin-ea580c)](https://runelite.net/) [![Java](https://img.shields.io/badge/Java-11-007396?logo=openjdk&logoColor=white)](https://adoptium.net/temurin/releases/?version=11) [![License](https://img.shields.io/badge/License-BSD%202--Clause-blue.svg)](LICENSE) [![Game](https://img.shields.io/badge/Old%20School-RuneScape-5d0000)](https://oldschool.runescape.wiki/w/Tekton)

---

Plenty of Challenge Mode raids get reset at Tekton - the team kills him, the time is not good enough, and you re-roll. Tekton Reset Tracker keeps count: when you reach Tekton in a CM raid and leave without moving on, it logs a reset and the time that raid cost you, all in a side panel. Kill a good Tekton and carry on into the raid and nothing is counted.

## Features

📊 **Lifetime and session stats**

Resets and total time wasted, both all-time and for the current session. Lifetime totals persist across client restarts; the session figure clears each time you launch the client.

🎯 **Challenge Mode only**

Normal raids are ignored. Only CM raids count towards your totals.

📍 **Live raid status**

The panel shows whether you are in a CM raid with a running timer, in a Normal raid, or not raiding at all. Kill Tekton and it reads "Completed Tekton" with the timer still running (you can still re-roll); move on to the next room and it turns green, "Moved past Tekton", and the raid no longer counts.

💬 **Optional chat message**

Prints your reset count and time wasted in-game whenever a reset is recorded. Can be turned off in the plugin settings.

📋 **Copy and reset**

Buttons to copy your stats to the clipboard or wipe all counts.

> A reset counts when you reach Tekton in a CM raid and leave - or log out - without progressing past him, whether or not you killed him. Carrying on to the next room, or completing the raid, never counts, and neither do Normal-mode raids.

## License

BSD 2-Clause. Old School RuneScape and Tekton are © Jagex Ltd; this is a fan-made plugin, not affiliated with or endorsed by Jagex. The icon is the in-game Tektiny pet sprite.
