package io.github.jmecn.ftbquestexport.model;

import java.util.List;

public record ChapterImage(
        String image,
        String baked,
        double x,
        double y,
        double width,
        double height,
        Double rotation,
        String click,
        Integer order,
        Double alpha,
        Integer color,
        Boolean alignToCorner,
        Boolean animated,
        Integer frameCount,
        Integer frameTime,
        List<Integer> frameSequence,
        Integer frameWidth,
        Integer frameHeight,
        String dependency,
        Boolean editorsOnly,
        List<String> hover) {

    public static Builder builder() {
        return new Builder();
    }

    public ChapterImage withBaked(String baked) {
        return new ChapterImage(
                image, baked, x, y, width, height, rotation, click, order, alpha, color, alignToCorner, animated,
                frameCount, frameTime, frameSequence, frameWidth, frameHeight, dependency, editorsOnly, hover);
    }

    public ChapterImage withAnimation(
            Boolean animated,
            Integer frameCount,
            Integer frameTime,
            List<Integer> frameSequence,
            Integer frameWidth,
            Integer frameHeight) {
        return new ChapterImage(
                image, baked, x, y, width, height, rotation, click, order, alpha, color, alignToCorner, animated,
                frameCount, frameTime, frameSequence, frameWidth, frameHeight, dependency, editorsOnly, hover);
    }

    public static final class Builder {
        private String image;
        private double x;
        private double y;
        private double width;
        private double height;
        private Double rotation;
        private String click;
        private Integer order;
        private Double alpha;
        private Integer color;
        private Boolean alignToCorner;
        private String dependency;
        private Boolean editorsOnly;
        private List<String> hover;

        public Builder image(String value) {
            this.image = value;
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

        public Builder width(double value) {
            this.width = value;
            return this;
        }

        public Builder height(double value) {
            this.height = value;
            return this;
        }

        public Builder rotation(Double value) {
            this.rotation = value;
            return this;
        }

        public Builder click(String value) {
            this.click = value;
            return this;
        }

        public Builder order(Integer value) {
            this.order = value;
            return this;
        }

        public Builder alpha(Double value) {
            this.alpha = value;
            return this;
        }

        public Builder color(Integer value) {
            this.color = value;
            return this;
        }

        public Builder alignToCorner(Boolean value) {
            this.alignToCorner = value;
            return this;
        }

        public Builder dependency(String value) {
            this.dependency = value;
            return this;
        }

        public Builder editorsOnly(Boolean value) {
            this.editorsOnly = value;
            return this;
        }

        public Builder hover(List<String> value) {
            this.hover = value;
            return this;
        }

        public ChapterImage build() {
            return new ChapterImage(
                    image, null, x, y, width, height, rotation, click, order, alpha, color, alignToCorner, null, null,
                    null, null, null, null, dependency, editorsOnly, hover);
        }
    }
}
