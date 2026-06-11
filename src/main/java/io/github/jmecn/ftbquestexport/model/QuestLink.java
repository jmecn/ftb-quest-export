package io.github.jmecn.ftbquestexport.model;

public record QuestLink(
        String id,
        String linkedQuest,
        double x,
        double y,
        Double size,
        String shape,
        IconDisplay iconDisplay) {

    public static Builder builder() {
        return new Builder();
    }

    public QuestLink withIconDisplay(IconDisplay display) {
        return new QuestLink(id, linkedQuest, x, y, size, shape, display);
    }

    public static final class Builder {
        private String id;
        private String linkedQuest;
        private double x;
        private double y;
        private Double size;
        private String shape;

        public Builder id(String value) {
            this.id = value;
            return this;
        }

        public Builder linkedQuest(String value) {
            this.linkedQuest = value;
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

        public QuestLink build() {
            return new QuestLink(id, linkedQuest, x, y, size, shape, null);
        }
    }
}
