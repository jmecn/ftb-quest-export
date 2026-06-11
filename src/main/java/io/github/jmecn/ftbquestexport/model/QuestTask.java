package io.github.jmecn.ftbquestexport.model;

import java.util.List;

public record QuestTask(
        String id,
        String type,
        String title,
        Boolean optional,
        List<String> items,
        String filterRaw,
        String fluid,
        String stage,
        String entity,
        String dimension,
        String biome,
        String toObserve,
        Long value,
        IconDisplay iconDisplay) {

    public static Builder builder() {
        return new Builder();
    }

    public QuestTask withIconDisplay(IconDisplay display) {
        return new QuestTask(
                id, type, title, optional, items, filterRaw, fluid, stage, entity, dimension, biome, toObserve,
                value, display);
    }

    public static final class Builder {
        private String id;
        private String type;
        private String title;
        private Boolean optional;
        private List<String> items;
        private String filterRaw;
        private String fluid;
        private String stage;
        private String entity;
        private String dimension;
        private String biome;
        private String toObserve;
        private Long value;

        public Builder id(String value) {
            this.id = value;
            return this;
        }

        public Builder type(String value) {
            this.type = value;
            return this;
        }

        public Builder title(String value) {
            this.title = value;
            return this;
        }

        public Builder optional(Boolean value) {
            this.optional = value;
            return this;
        }

        public Builder items(List<String> value) {
            this.items = value;
            return this;
        }

        public Builder filterRaw(String value) {
            this.filterRaw = value;
            return this;
        }

        public Builder fluid(String value) {
            this.fluid = value;
            return this;
        }

        public Builder stage(String value) {
            this.stage = value;
            return this;
        }

        public Builder entity(String value) {
            this.entity = value;
            return this;
        }

        public Builder dimension(String value) {
            this.dimension = value;
            return this;
        }

        public Builder biome(String value) {
            this.biome = value;
            return this;
        }

        public Builder toObserve(String value) {
            this.toObserve = value;
            return this;
        }

        public Builder value(Long value) {
            this.value = value;
            return this;
        }

        public QuestTask build() {
            return new QuestTask(
                    id, type, title, optional, items, filterRaw, fluid, stage, entity, dimension, biome, toObserve,
                    value, null);
        }
    }
}
