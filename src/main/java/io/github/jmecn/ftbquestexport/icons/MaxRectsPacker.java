package io.github.jmecn.ftbquestexport.icons;

import io.github.jmecn.ftbquestexport.QuestExportConstants;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class MaxRectsPacker {

    private final int pageWidth;
    private final int pageHeight;
    private final int padding;
    private final List<PackPage> pages = new ArrayList<>();

    public MaxRectsPacker(int pageWidth, int pageHeight, int padding) {
        this.pageWidth = pageWidth;
        this.pageHeight = pageHeight;
        this.padding = padding;
    }

    public static MaxRectsPacker defaults() {
        return new MaxRectsPacker(
                QuestExportConstants.ATLAS_MAX_WIDTH_PX,
                QuestExportConstants.ATLAS_MAX_HEIGHT_PX,
                QuestExportConstants.ATLAS_PADDING_PX);
    }

    public List<PackPage> pages() {
        return pages;
    }

    public List<PackRect> packAll(List<PackItem> items) {
        List<PackRect> rects = new ArrayList<>();
        for (PackItem item : items) {
            rects.add(new PackRect(item.name(), item.width(), item.height()));
        }
        rects.sort(Comparator
                .comparingInt((PackRect r) -> r.width() * r.height())
                .thenComparingInt(PackRect::width)
                .thenComparingInt(PackRect::height)
                .reversed());

        List<PackRect> placed = new ArrayList<>();
        for (PackRect rect : rects) {
            placed.add(packOne(rect));
        }
        return placed;
    }

    private PackRect packOne(PackRect image) {
        int pad = padding;
        int innerW = pageWidth - pad * 2;
        int innerH = pageHeight - pad * 2;
        int rectW = image.width() + pad;
        int rectH = image.height() + pad;

        if (rectW > innerW + pad || rectH > innerH + pad) {
            throw new IllegalArgumentException(
                    "Sprite " + image.name() + " (" + image.width() + "x" + image.height()
                            + ") exceeds max page " + pageWidth + "x" + pageHeight);
        }

        for (PackPage page : pages) {
            int[] node = findPosition(page, rectW, rectH);
            if (node != null) {
                return place(page, image, node[0], node[1], pad);
            }
        }

        PackPage page = new PackPage(pages.size());
        page.freeRects().add(new FreeRect(pad, pad, innerW, innerH));
        pages.add(page);
        int[] node = findPosition(page, rectW, rectH);
        if (node == null) {
            throw new IllegalStateException("Failed to pack " + image.name() + " on fresh page");
        }
        return place(page, image, node[0], node[1], pad);
    }

    private PackRect place(PackPage page, PackRect image, int x, int y, int pad) {
        int rectW = image.width() + pad;
        int rectH = image.height() + pad;
        PackRect placed = image.withPosition(x, y, page.index());
        page.rects().add(placed);
        splitFreeRects(page, x, y, rectW, rectH);
        pruneFreeList(page);
        return placed;
    }

    private int[] findPosition(PackPage page, int width, int height) {
        int[] bestNode = null;
        int bestScore1 = 1_000_000_000;
        int bestScore2 = 1_000_000_000;

        for (FreeRect free : page.freeRects()) {
            if (free.width < width || free.height < height) {
                continue;
            }
            int[] score = scoreContactPoint(free, width, height, free.x, free.y, page);
            if (score[0] < bestScore1 || (score[0] == bestScore1 && score[1] < bestScore2)) {
                bestScore1 = score[0];
                bestScore2 = score[1];
                bestNode = new int[] {free.x, free.y};
            }
        }
        return bestNode;
    }

    private int[] scoreContactPoint(FreeRect free, int width, int height, int x, int y, PackPage page) {
        int leftoverW = free.width - width;
        int leftoverH = free.height - height;
        int shortSide = Math.min(leftoverW, leftoverH);

        int contact = 0;
        if (x == padding) {
            contact += height;
        }
        if (y == padding) {
            contact += width;
        }
        if (x + width == pageWidth - padding) {
            contact += height;
        }
        if (y + height == pageHeight - padding) {
            contact += width;
        }
        for (PackRect rect : page.rects()) {
            if (rect.x() == x + width || rect.x() + rect.width() == x) {
                contact += Math.min(rect.height(), height);
            }
            if (rect.y() == y + height || rect.y() + rect.height() == y) {
                contact += Math.min(rect.width(), width);
            }
        }
        return new int[] {-contact, shortSide};
    }

    private void splitFreeRects(PackPage page, int x, int y, int width, int height) {
        FreeRect used = new FreeRect(x, y, width, height);
        for (int i = 0; i < page.freeRects().size(); ) {
            if (splitFreeNode(page, page.freeRects().get(i), used)) {
                page.freeRects().remove(i);
            } else {
                i++;
            }
        }
    }

    private boolean splitFreeNode(PackPage page, FreeRect free, FreeRect used) {
        if (!intersects(free, used)) {
            return false;
        }

        if (used.x < free.x + free.width && used.x + used.width > free.x) {
            if (used.y > free.y && used.y < free.y + free.height) {
                page.freeRects().add(new FreeRect(free.x, free.y, free.width, used.y - free.y));
            }
            if (used.y + used.height < free.y + free.height) {
                page.freeRects()
                        .add(new FreeRect(
                                free.x,
                                used.y + used.height,
                                free.width,
                                free.y + free.height - (used.y + used.height)));
            }
        }

        if (used.y < free.y + free.height && used.y + used.height > free.y) {
            if (used.x > free.x && used.x < free.x + free.width) {
                page.freeRects().add(new FreeRect(free.x, free.y, used.x - free.x, free.height));
            }
            if (used.x + used.width < free.x + free.width) {
                page.freeRects()
                        .add(new FreeRect(
                                used.x + used.width,
                                free.y,
                                free.x + free.width - (used.x + used.width),
                                free.height));
            }
        }
        return true;
    }

    private void pruneFreeList(PackPage page) {
        for (int i = 0; i < page.freeRects().size(); ) {
            boolean removed = false;
            for (int j = i + 1; j < page.freeRects().size(); ) {
                FreeRect a = page.freeRects().get(i);
                FreeRect b = page.freeRects().get(j);
                if (contains(a, b)) {
                    page.freeRects().remove(j);
                } else if (contains(b, a)) {
                    page.freeRects().remove(i);
                    i--;
                    removed = true;
                    break;
                } else {
                    j++;
                }
            }
            if (!removed) {
                i++;
            }
        }
    }

    private static boolean intersects(FreeRect a, FreeRect b) {
        return !(a.x >= b.x + b.width
                || a.x + a.width <= b.x
                || a.y >= b.y + b.height
                || a.y + a.height <= b.y);
    }

    private static boolean contains(FreeRect a, FreeRect b) {
        return a.x <= b.x
                && a.y <= b.y
                && a.x + a.width >= b.x + b.width
                && a.y + a.height >= b.y + b.height;
    }

    public int[] pageContentSize(int pageIndex) {
        PackPage page = pages.get(pageIndex);
        if (page.rects().isEmpty()) {
            return new int[] {1, 1};
        }
        int maxX = 0;
        int maxY = 0;
        for (PackRect rect : page.rects()) {
            maxX = Math.max(maxX, rect.x() + rect.width());
            maxY = Math.max(maxY, rect.y() + rect.height());
        }
        return new int[] {Math.max(1, maxX + padding), Math.max(1, maxY + padding)};
    }

    public record PackItem(String name, int width, int height) {}

    public record PackRect(String name, int width, int height, int x, int y, int page) {
        PackRect(String name, int width, int height) {
            this(name, width, height, 0, 0, 0);
        }

        PackRect withPosition(int x, int y, int page) {
            return new PackRect(name, width, height, x, y, page);
        }
    }

    public static final class PackPage {
        private final int index;
        private final List<FreeRect> freeRects = new ArrayList<>();
        private final List<PackRect> rects = new ArrayList<>();

        public PackPage(int index) {
            this.index = index;
        }

        public int index() {
            return index;
        }

        List<FreeRect> freeRects() {
            return freeRects;
        }

        public List<PackRect> rects() {
            return rects;
        }
    }

    private static final class FreeRect {
        final int x;
        final int y;
        final int width;
        final int height;

        FreeRect(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
}
