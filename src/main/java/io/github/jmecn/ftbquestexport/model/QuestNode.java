package io.github.jmecn.ftbquestexport.model;

import java.util.List;

public record QuestNode(
        String id,
        Boolean invisible,
        double x,
        double y,
        Double size,
        String shape,
        String title,
        String titleItem,
        Long titleCount,
        String subtitle,
        List<String> description,
        String icon,
        List<String> iconItems,
        List<String> dependencies,
        Boolean hideDependencyLines,
        String guidePage,
        List<QuestTask> tasks,
        List<QuestReward> rewards,
        IconDisplay iconDisplay) {

    public static Builder builder() {
        return new Builder();
    }

    public QuestNode withIconDisplay(IconDisplay display) {
        return new QuestNode(
                id, invisible, x, y, size, shape, title, titleItem, titleCount, subtitle, description, icon,
                iconItems, dependencies, hideDependencyLines, guidePage, tasks, rewards, display);
    }

    public QuestNode withTasks(List<QuestTask> tasks) {
        return new QuestNode(
                id, invisible, x, y, size, shape, title, titleItem, titleCount, subtitle, description, icon,
                iconItems, dependencies, hideDependencyLines, guidePage, tasks, rewards, iconDisplay);
    }

    public QuestNode withRewards(List<QuestReward> rewards) {
        return new QuestNode(
                id, invisible, x, y, size, shape, title, titleItem, titleCount, subtitle, description, icon,
                iconItems, dependencies, hideDependencyLines, guidePage, tasks, rewards, iconDisplay);
    }

    public static final class Builder {
        private String id;
        private Boolean invisible;
        private double x;
        private double y;
        private Double size;
        private String shape;
        private String title;
        private String titleItem;
        private Long titleCount;
        private String subtitle;
        private List<String> description;
        private String icon;
        private List<String> iconItems;
        private List<String> dependencies;
        private Boolean hideDependencyLines;
        private String guidePage;
        private List<QuestTask> tasks;
        private List<QuestReward> rewards;

        public Builder id(String value) {
            this.id = value;
            return this;
        }

        public Builder invisible(Boolean value) {
            this.invisible = value;
            return this;
        }

        public Builder x(double value) {
            this.x = value;
            return this;
        }

        public Builder y(double value) {
            this.y = value;
            return this;
        }

        public Builder size(Double value) {
            this.size = value;
            return this;
        }

        public Builder shape(String value) {
            this.shape = value;
            return this;
        }

        public Builder title(String value) {
            this.title = value;
            return this;
        }

        public Builder titleItem(String value) {
            this.titleItem = value;
            return this;
        }

        public Builder titleCount(Long value) {
            this.titleCount = value;
            return this;
        }

        public Builder subtitle(String value) {
            this.subtitle = value;
            return this;
        }

        public Builder description(List<String> value) {
            this.description = value;
            return this;
        }

        public Builder icon(String value) {
            this.icon = value;
            return this;
        }

        public Builder iconItems(List<String> value) {
            this.iconItems = value;
            return this;
        }

        public Builder dependencies(List<String> value) {
            this.dependencies = value;
            return this;
        }

        public Builder hideDependencyLines(Boolean value) {
            this.hideDependencyLines = value;
            return this;
        }

        public Builder guidePage(String value) {
            this.guidePage = value;
            return this;
        }

        public Builder tasks(List<QuestTask> value) {
            this.tasks = value;
            return this;
        }

        public Builder rewards(List<QuestReward> value) {
            this.rewards = value;
            return this;
        }

        public QuestNode build() {
            return new QuestNode(
                    id, invisible, x, y, size, shape, title, titleItem, titleCount, subtitle, description, icon,
                    iconItems, dependencies, hideDependencyLines, guidePage, tasks, rewards, null);
        }
    }
}
