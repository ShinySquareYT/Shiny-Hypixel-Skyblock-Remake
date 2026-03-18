package net.shinysquare.shiny_sb.content.fancybars;

import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;

public class BarPositioner {

    private final Map<BarAnchor, LinkedList<LinkedList<StatusBar>>> map = new EnumMap<>(BarAnchor.class);

    public BarPositioner() {
        for (BarAnchor value : BarAnchor.values()) {
            map.put(value, new LinkedList<>());
        }
    }

    public int getRowCount(BarAnchor barAnchor) {
        return map.get(barAnchor).size();
    }

    /** Adds a row to the end of an anchor */
    public void addRow(BarAnchor barAnchor) {
        map.get(barAnchor).add(new LinkedList<>());
    }

    /** Adds a row at the specified index */
    public void addRow(BarAnchor barAnchor, int row) {
        LinkedList<LinkedList<StatusBar>> rows = map.get(barAnchor);
        for (int i = row; i < rows.size(); i++) {
            for (StatusBar bar : rows.get(i)) bar.gridY++;
        }
        rows.add(row, new LinkedList<>());
    }

    /** Adds a bar to the end of a row */
    public void addBar(BarAnchor barAnchor, int row, StatusBar bar) {
        LinkedList<StatusBar> statusBars = map.get(barAnchor).get(row);
        statusBars.add(bar);
        bar.gridY = row;
        bar.gridX = statusBars.lastIndexOf(bar);
        bar.anchor = barAnchor;
    }

    /** Adds a bar at the specified x in a row */
    public void addBar(BarAnchor barAnchor, int row, int x, StatusBar bar) {
        LinkedList<StatusBar> statusBars = map.get(barAnchor).get(row);
        for (int i = x; i < statusBars.size(); i++) statusBars.get(i).gridX++;
        statusBars.add(x, bar);
        bar.gridY = row;
        bar.gridX = statusBars.indexOf(bar);
        bar.anchor = barAnchor;
    }

    /** Removes bar at x on row. Auto-removes the row if empty. */
    public void removeBar(BarAnchor barAnchor, int row, int x) {
        LinkedList<StatusBar> statusBars = map.get(barAnchor).get(row);
        StatusBar remove = statusBars.remove(x);
        remove.anchor = null;
        for (int i = x; i < statusBars.size(); i++) statusBars.get(i).gridX--;
        if (statusBars.isEmpty()) removeRow(barAnchor, row);
    }

    /** Removes the given bar from the row. Auto-removes the row if empty. */
    public void removeBar(BarAnchor barAnchor, int row, StatusBar bar) {
        LinkedList<StatusBar> barRow = map.get(barAnchor).get(row);
        int x = barRow.indexOf(bar);
        if (x < 0) return;
        barRow.remove(bar);
        bar.anchor = null;
        for (int i = x; i < barRow.size(); i++) barRow.get(i).gridX--;
        if (barRow.isEmpty()) removeRow(barAnchor, row);
    }

    /** Row must be empty before removal. */
    public void removeRow(BarAnchor barAnchor, int row) {
        LinkedList<StatusBar> barRow = map.get(barAnchor).get(row);
        if (!barRow.isEmpty())
            throw new IllegalStateException("Can't remove a non-empty row (" + barAnchor + "," + row + ")");
        map.get(barAnchor).remove(row);
        for (int i = row; i < map.get(barAnchor).size(); i++) {
            for (StatusBar statusBar : map.get(barAnchor).get(i)) statusBar.gridY--;
        }
    }

    public LinkedList<StatusBar> getRow(BarAnchor barAnchor, int row) {
        return map.get(barAnchor).get(row);
    }

    public StatusBar getBar(BarAnchor barAnchor, int row, int x) {
        return map.get(barAnchor).get(row).get(x);
    }

    public boolean hasNeighbor(BarAnchor barAnchor, int row, int x, boolean right) {
        LinkedList<StatusBar> statusBars = map.get(barAnchor).get(row);
        if (barAnchor.isRight()) {
            return (right && x < statusBars.size() - 1) || (!right && x > 0);
        } else {
            return (right && x > 0) || (!right && x < statusBars.size() - 1);
        }
    }

    public void clear() {
        map.replaceAll((barAnchor, rows) -> new LinkedList<>());
    }

    public enum BarAnchor {
        HOTBAR_LEFT(true, false,
                (scaledWidth, scaledHeight) -> new ScreenPosition(scaledWidth / 2 - 91 - 2, scaledHeight - 5),
                SizeRule.freeSize(25, 2, 6)),

        HOTBAR_RIGHT(true, true,
                (scaledWidth, scaledHeight) -> new ScreenPosition(scaledWidth / 2 + 91 + 2, scaledHeight - 5),
                SizeRule.freeSize(25, 2, 6)),

        HOTBAR_TOP(true, true,
                (scaledWidth, scaledHeight) -> new ScreenPosition(scaledWidth / 2 - 91, scaledHeight - (FancyStatusBars.isExperienceFancyBarEnabled() ? 23 : 35)),
                SizeRule.targetSize(12, 182, 2),
                anchorPosition -> new ScreenRectangle(anchorPosition.x(), anchorPosition.y() - 20, 182, 20)),

        SCREEN_TOP_LEFT(false, true,
                ((scaledWidth, scaledHeight) -> new ScreenPosition(5, 5)),
                SizeRule.freeSize(25, 2, 6)),

        SCREEN_TOP_RIGHT(false, false,
                ((scaledWidth, scaledHeight) -> new ScreenPosition(scaledWidth - 5, 5)),
                SizeRule.freeSize(25, 2, 6)),

        SCREEN_BOTTOM_LEFT(true, true,
                ((scaledWidth, scaledHeight) -> new ScreenPosition(5, scaledHeight - 5)),
                SizeRule.freeSize(25, 2, 6)),

        SCREEN_BOTTOM_RIGHT(true, false,
                ((scaledWidth, scaledHeight) -> new ScreenPosition(scaledWidth - 5, scaledHeight - 5)),
                SizeRule.freeSize(25, 2, 6)),

        SCREEN_CENTER_LEFT(false, false,
                ((scaledWidth, scaledHeight) -> new ScreenPosition(scaledWidth / 2 - 8, scaledHeight / 2 - 4)),
                SizeRule.freeSize(15, 3, 8)),

        SCREEN_CENTER_RIGHT(false, true,
                ((scaledWidth, scaledHeight) -> new ScreenPosition(scaledWidth / 2 + 8, scaledHeight / 2 - 4)),
                SizeRule.freeSize(15, 3, 8));

        private final AnchorPositionProvider positionProvider;
        private final AnchorHitboxProvider hitboxProvider;
        private final boolean up;
        private final boolean right;
        private final SizeRule sizeRule;

        BarAnchor(boolean up, boolean right, AnchorPositionProvider positionProvider,
                  SizeRule sizeRule, AnchorHitboxProvider hitboxProvider) {
            this.positionProvider = positionProvider;
            this.up = up;
            this.right = right;
            this.hitboxProvider = hitboxProvider;
            this.sizeRule = sizeRule;
        }

        BarAnchor(boolean up, boolean right, AnchorPositionProvider positionProvider, SizeRule sizeRule) {
            this(up, right, positionProvider, sizeRule,
                    anchorPosition -> new ScreenRectangle(
                            anchorPosition.x() - (right ? 0 : 20),
                            anchorPosition.y() - (up ? 20 : 0), 20, 20));
        }

        public ScreenPosition getAnchorPosition(int scaledWidth, int scaledHeight) {
            return positionProvider.getPosition(scaledWidth, scaledHeight);
        }

        public ScreenRectangle getAnchorHitbox(ScreenPosition anchorPosition) {
            return hitboxProvider.getHitbox(anchorPosition);
        }

        public boolean isUp() { return up; }
        public boolean isRight() { return right; }
        public SizeRule getSizeRule() { return sizeRule; }

        private static final List<BarAnchor> cached = List.of(values());

        public static List<BarAnchor> allAnchors() { return cached; }
    }

    public record SizeRule(boolean isTargetSize, int targetSize, int totalWidth,
                           int widthPerSize, int minSize, int maxSize) {
        public static SizeRule freeSize(int widthPerSize, int minSize, int maxSize) {
            return new SizeRule(false, -1, -1, widthPerSize, minSize, maxSize);
        }

        public static SizeRule targetSize(int targetSize, int totalWidth, int minSize) {
            return new SizeRule(true, targetSize, totalWidth, -1, minSize, targetSize);
        }
    }

    public record BarLocation(@Nullable BarAnchor barAnchor, int x, int y) {
        public static final BarLocation NULL = new BarLocation(null, -1, -1);

        public static BarLocation of(StatusBar bar) {
            return new BarLocation(bar.anchor, bar.gridX, bar.gridY);
        }

        public boolean equals(BarAnchor barAnchor, int x, int y) {
            return x == this.x && y == this.y && barAnchor == this.barAnchor;
        }
    }

    @FunctionalInterface
    interface AnchorPositionProvider {
        ScreenPosition getPosition(int scaledWidth, int scaledHeight);
    }

    @FunctionalInterface
    interface AnchorHitboxProvider {
        ScreenRectangle getHitbox(ScreenPosition anchorPosition);
    }
}
