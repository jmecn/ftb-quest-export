# FTB Quest Export

Forge mod: runtime export of FTB Quests into a self-contained **`quest-export/`** bundle for [QuestBook-React](https://github.com/jmecn/QuestBook-React).

## Layout

```text
<exportRoot>/
  quest-export/
    manifest.json, meta.json
    quests/index.json, quests/chapters/<filename>.json
    lang/<locale>.json
    assets/, data/
    extras/tag-members.json, filters.json, fluids.json
```