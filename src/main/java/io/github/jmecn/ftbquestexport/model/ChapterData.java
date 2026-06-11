package io.github.jmecn.ftbquestexport.model;

import java.util.LinkedHashMap;
import java.util.List;

public record ChapterData(
        String id,
        String filename,
        String group,
        String icon,
        String defaultQuestShape,
        int orderIndex,
        String title,
        List<String> subtitle,
        String autofocusId,
        List<QuestNode> quests,
        List<QuestLink> questLinks,
        List<ChapterImage> images,
        LinkedHashMap<String, IconAtlasPage> iconAtlases,
        LinkedHashMap<String, IconSpriteRect> iconSprites) {

    public static Builder builder() {
        return new Builder();
    }

    public ChapterData withIconAtlases(LinkedHashMap<String, IconAtlasPage> iconAtlases) {
        return new ChapterData(
                id, filename, group, icon, defaultQuestShape, orderIndex, title, subtitle, autofocusId, quests,
                questLinks, images, iconAtlases, iconSprites);
    }

    public ChapterData withIconSprites(LinkedHashMap<String, IconSpriteRect> iconSprites) {
        return new ChapterData(
                id, filename, group, icon, defaultQuestShape, orderIndex, title, subtitle, autofocusId, quests,
                questLinks, images, iconAtlases, iconSprites);
    }

    public ChapterData withQuests(List<QuestNode> quests) {
        return new ChapterData(
                id, filename, group, icon, defaultQuestShape, orderIndex, title, subtitle, autofocusId, quests,
                questLinks, images, iconAtlases, iconSprites);
    }

    public ChapterData withQuestLinks(List<QuestLink> questLinks) {
        return new ChapterData(
                id, filename, group, icon, defaultQuestShape, orderIndex, title, subtitle, autofocusId, quests,
                questLinks, images, iconAtlases, iconSprites);
    }

    public ChapterData withImages(List<ChapterImage> images) {
        return new ChapterData(
                id, filename, group, icon, defaultQuestShape, orderIndex, title, subtitle, autofocusId, quests,
                questLinks, images, iconAtlases, iconSprites);
    }

    public static final class Builder {
        private String id;
        private String filename;
        private String group;
        private String icon;
        private String defaultQuestShape;
        private int orderIndex;
        private String title;
        private List<String> subtitle;
        private String autofocusId;
        private List<QuestNode> quests;
        private List<QuestLink> questLinks;
        private List<ChapterImage> images;

        public Builder id(String value) {
            this.id = value;
            return this;
        }

        public Builder filename(String value) {
            this.filename = value;
            return this;
        }

        public Builder group(String value) {
            this.group = value;
            return this;
        }

        public Builder icon(String value) {
            this.icon = value;
            return this;
        }

        public Builder defaultQuestShape(String value) {
            this.defaultQuestShape = value;
            return this;
        }

        public Builder orderIndex(int value) {
            this.orderIndex = value;
            return this;
        }

        public Builder title(String value) {
            this.title = value;
            return this;
        }

        public Builder subtitle(List<String> value) {
            this.subtitle = value;
            return this;
        }

        public Builder autofocusId(String value) {
            this.autofocusId = value;
            return this;
        }

        public Builder quests(List<QuestNode> value) {
            this.quests = value;
            return this;
        }

        public Builder questLinks(List<QuestLink> value) {
            this.questLinks = value;
            return this;
        }

        public Builder images(List<ChapterImage> value) {
            this.images = value;
            return this;
        }

        public ChapterData build() {
            return new ChapterData(
                    id, filename, group, icon, defaultQuestShape, orderIndex, title, subtitle, autofocusId, quests,
                    questLinks, images, null, null);
        }
    }
}
