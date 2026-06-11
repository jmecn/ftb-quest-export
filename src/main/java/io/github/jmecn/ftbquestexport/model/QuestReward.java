package io.github.jmecn.ftbquestexport.model;

import java.util.List;

public record QuestReward(
        String id,
        String type,
        List<String> items,
        Integer count,
        IconDisplay iconDisplay) {

    public static Builder builder() {
        return new Builder();
    }

    public QuestReward withIconDisplay(IconDisplay display) {
        return new QuestReward(id, type, items, count, display);
    }

    public static final class Builder {
        private String id;
        private String type;
        private List<String> items;
        private Integer count;

        public Builder id(String value) {
            this.id = value;
            return this;
        }

        public Builder type(String value) {
            this.type = value;
            return this;
        }

        public Builder items(List<String> value) {
            this.items = value;
            return this;
        }

        public Builder count(Integer value) {
            this.count = value;
            return this;
        }

        public QuestReward build() {
            return new QuestReward(id, type, items, count, null);
        }
    }
}
