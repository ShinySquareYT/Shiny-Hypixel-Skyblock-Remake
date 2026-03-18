package net.shinysquare.shiny_sb.content.fancybars;

import com.mojang.blaze3d.platform.Window;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectBooleanMutablePair;
import it.unimi.dsi.fastutil.objects.ObjectBooleanPair;
import it.unimi.dsi.fastutil.objects.ObjectObjectMutablePair;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import net.shinysquare.shiny_sb.content.fancybars.BarPositioner.BarLocation;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class StatusBarsConfigScreen extends Screen {

    // [NEOFORGE] Identifier.withDefaultNamespace → ResourceLocation.withDefaultNamespace (same API, different name)
    private static final ResourceLocation HOTBAR_TEXTURE = ResourceLocation.withDefaultNamespace("hud/hotbar");
    private static final int HOTBAR_WIDTH = 182;
    private static final float RESIZE_THRESHOLD = 0.75f;
    private static final int BAR_MINIMUM_WIDTH = 30;
    private static final ScreenDirection[] DIRECTION_CHECK_ORDER = {
            ScreenDirection.LEFT, ScreenDirection.RIGHT, ScreenDirection.UP, ScreenDirection.DOWN
    };

    private final Map<ScreenRectangle, Pair<StatusBar, BarLocation>> rectToBar = new HashMap<>();
    private final ObjectBooleanPair<@Nullable StatusBar> resizeHover = new ObjectBooleanMutablePair<>(null, false);
    private final Pair<@Nullable StatusBar, @Nullable StatusBar> resizedBars = ObjectObjectMutablePair.of(null, null);

    private @Nullable StatusBar cursorBar = null;
    private ScreenPosition cursorOffset = new ScreenPosition(0, 0);
    private BarLocation currentInsertLocation = new BarLocation(null, 0, 0);

    private boolean resizing = false;
    private EditBarWidget editBarWidget;

    /**
     * [NEOFORGE] Cursor management.
     * Fabric provided context.requestCursor(CursorTypes.RESIZE_EW) on GuiGraphics — a Fabric
     * extension. In NeoForge/vanilla, we use GLFW directly to set a standard cursor.
     * The cursor is created lazily and reset to default at the start of each render call.
     */
    private static long resizeCursorHandle = 0L;

    private static long getResizeCursor() {
        if (resizeCursorHandle == 0L) {
            resizeCursorHandle = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HRESIZE_CURSOR);
        }
        return resizeCursorHandle;
    }

    // Track whether we set the resize cursor this frame so we can reset it if not hovering
    private boolean setResizeCursorThisFrame = false;

    public StatusBarsConfigScreen() {
        super(Component.nullToEmpty("Status Bars Config"));
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // [NEOFORGE] blitSprite() without RenderPipelines argument
        context.blitSprite(HOTBAR_TEXTURE, width / 2 - HOTBAR_WIDTH / 2, height - 22, HOTBAR_WIDTH, 22);
        editBarWidget.render(context, mouseX, mouseY, delta);

        Window window = minecraft.getWindow();
        int scaleFactor = window.calculateScale(0, minecraft.isEnforceUnicode()) - window.getGuiScale() + 3;
        if ((scaleFactor & 2) == 0) scaleFactor++;

        ScreenRectangle mouseRect = new ScreenRectangle(
                new ScreenPosition(mouseX - scaleFactor / 2, mouseY - scaleFactor / 2),
                scaleFactor, scaleFactor);

        // Reset cursor state for this frame; set again below if hovering a resize edge
        setResizeCursorThisFrame = false;

        if (cursorBar != null) {
            renderDragging(context, mouseX, mouseY, delta, mouseRect);
        } else {
            renderHover(context, mouseX, mouseY, mouseRect);
        }

        // [NEOFORGE] Apply or reset cursor after render logic is complete
        long windowHandle = minecraft.getWindow().getWindow();
        if (setResizeCursorThisFrame) {
            GLFW.glfwSetCursor(windowHandle, getResizeCursor());
        } else {
            GLFW.glfwSetCursor(windowHandle, 0L); // 0 = default OS cursor
        }
    }

    private void renderDragging(GuiGraphics context, int mouseX, int mouseY, float delta, ScreenRectangle mouseRect) {
        cursorBar.renderCursor(context, mouseX + cursorOffset.x(), mouseY + cursorOffset.y(), delta);
        boolean inserted = false;
        boolean updatePositions = false;

        rectLoop:
        for (ScreenRectangle screenRect : rectToBar.keySet()) {
            for (ScreenDirection direction : DIRECTION_CHECK_ORDER) {
                boolean overlaps = screenRect.getBorder(direction).step(direction).overlaps(mouseRect);
                if (overlaps) {
                    Pair<StatusBar, BarLocation> barPair = rectToBar.get(screenRect);
                    BarLocation barSnap = barPair.right();
                    if (barSnap.barAnchor() == null) break;

                    if (direction.getAxis().equals(ScreenAxis.VERTICAL)) {
                        int neighborInsertY = getNeighborInsertY(barSnap, !direction.isPositive());
                        inserted = true;
                        if (!currentInsertLocation.equals(barSnap.barAnchor(), barSnap.x(), neighborInsertY)) {
                            if (cursorBar.anchor != null)
                                FancyStatusBars.barPositioner.removeBar(cursorBar.anchor, cursorBar.gridY, cursorBar);
                            FancyStatusBars.barPositioner.addRow(barSnap.barAnchor(), neighborInsertY);
                            FancyStatusBars.barPositioner.addBar(barSnap.barAnchor(), neighborInsertY, cursorBar);
                            currentInsertLocation = BarLocation.of(cursorBar);
                            updatePositions = true;
                        }
                    } else {
                        int neighborInsertX = getNeighborInsertX(barSnap, direction.isPositive());
                        inserted = true;
                        if (!currentInsertLocation.equals(barSnap.barAnchor(), neighborInsertX, barSnap.y())) {
                            if (cursorBar.anchor != null)
                                FancyStatusBars.barPositioner.removeBar(cursorBar.anchor, cursorBar.gridY, cursorBar);
                            FancyStatusBars.barPositioner.addBar(barSnap.barAnchor(), barSnap.y(), neighborInsertX, cursorBar);
                            currentInsertLocation = BarLocation.of(cursorBar);
                            updatePositions = true;
                        }
                    }
                    break rectLoop;
                }
            }
        }

        if (updatePositions) {
            FancyStatusBars.updatePositions(true);
            return;
        }

        for (BarPositioner.BarAnchor barAnchor : BarPositioner.BarAnchor.allAnchors()) {
            ScreenRectangle anchorHitbox = barAnchor.getAnchorHitbox(barAnchor.getAnchorPosition(width, height));
            if (FancyStatusBars.barPositioner.getRowCount(barAnchor) != 0) {
                if (FancyStatusBars.barPositioner.getRowCount(barAnchor) == 1) {
                    LinkedList<StatusBar> row = FancyStatusBars.barPositioner.getRow(barAnchor, 0);
                    if (row.size() == 1 && row.getFirst() == cursorBar && anchorHitbox.overlaps(mouseRect))
                        inserted = true;
                }
                continue;
            }
            context.fill(anchorHitbox.left(), anchorHitbox.top(), anchorHitbox.right(), anchorHitbox.bottom(), 0x99FFFFFF);
            if (anchorHitbox.overlaps(mouseRect)) {
                inserted = true;
                if (currentInsertLocation.barAnchor() == barAnchor) continue;
                if (cursorBar.anchor != null)
                    FancyStatusBars.barPositioner.removeBar(cursorBar.anchor, cursorBar.gridY, cursorBar);
                FancyStatusBars.barPositioner.addRow(barAnchor);
                FancyStatusBars.barPositioner.addBar(barAnchor, 0, cursorBar);
                currentInsertLocation = BarLocation.of(cursorBar);
                FancyStatusBars.updatePositions(true);
            }
        }

        if (!inserted) {
            if (cursorBar.anchor != null)
                FancyStatusBars.barPositioner.removeBar(cursorBar.anchor, cursorBar.gridY, cursorBar);
            currentInsertLocation = BarLocation.NULL;
            FancyStatusBars.updatePositions(true);
            cursorBar.setX(width + 5);
        }
    }

    private void renderHover(GuiGraphics context, int mouseX, int mouseY, ScreenRectangle mouseRect) {
        if (resizing) {
            StatusBar rightBar = resizedBars.right();
            StatusBar leftBar  = resizedBars.left();
            boolean hasRight = rightBar != null;
            boolean hasLeft  = leftBar  != null;
            BarPositioner.BarAnchor barAnchor = !hasRight ? leftBar.anchor : rightBar.anchor;
            int middleX = !hasRight
                    ? leftBar.getX()  + leftBar.getWidth()
                    : rightBar.getX();

            if (barAnchor != null) {
                BarPositioner.SizeRule sizeRule = barAnchor.getSizeRule();
                float widthPerSize = sizeRule.isTargetSize()
                        ? (float) sizeRule.totalWidth() / sizeRule.targetSize()
                        : sizeRule.widthPerSize();
                boolean doResize = true;

                if (mouseX < middleX) {
                    if (middleX - mouseX > widthPerSize / RESIZE_THRESHOLD) {
                        if (hasRight && rightBar.size + 1 > sizeRule.maxSize()) doResize = false;
                        if (hasLeft  && leftBar.size  - 1 < sizeRule.minSize()) doResize = false;
                        if (doResize) {
                            if (hasRight) rightBar.size++;
                            if (hasLeft)  leftBar.size--;
                            FancyStatusBars.updatePositions(true);
                        }
                    }
                } else {
                    if (mouseX - middleX > widthPerSize / RESIZE_THRESHOLD) {
                        if (hasRight && rightBar.size - 1 < sizeRule.minSize()) doResize = false;
                        if (hasLeft  && leftBar.size  + 1 > sizeRule.maxSize()) doResize = false;
                        if (doResize) {
                            if (hasRight) rightBar.size--;
                            if (hasLeft)  leftBar.size++;
                            FancyStatusBars.updatePositions(true);
                        }
                    }
                }
            } else {
                if (hasLeft) {
                    leftBar.setWidth(Math.max(BAR_MINIMUM_WIDTH, mouseX - leftBar.getX()));
                } else if (hasRight) {
                    int endX = rightBar.getX() + rightBar.getWidth();
                    rightBar.setX(Math.min(endX - BAR_MINIMUM_WIDTH, mouseX));
                    rightBar.setWidth(endX - rightBar.getX());
                }
            }
        } else {
            rectLoop:
            for (ScreenRectangle screenRect : rectToBar.keySet()) {
                for (ScreenDirection direction : new ScreenDirection[]{ScreenDirection.LEFT, ScreenDirection.RIGHT}) {
                    boolean overlaps = screenRect.getBorder(direction).step(direction).overlaps(mouseRect);
                    if (overlaps && !editBarWidget.isMouseOver(mouseX, mouseY)) {
                        Pair<StatusBar, BarLocation> barPair = rectToBar.get(screenRect);
                        BarLocation barLocation = barPair.right();
                        StatusBar bar = barPair.left();
                        if (!bar.enabled) break;
                        boolean right = direction.equals(ScreenDirection.RIGHT);
                        if (barLocation.barAnchor() != null) {
                            if (barLocation.barAnchor().getSizeRule().isTargetSize()
                                    && !FancyStatusBars.barPositioner.hasNeighbor(barLocation.barAnchor(), barLocation.y(), barLocation.x(), right)) {
                                break;
                            }
                            if (!barLocation.barAnchor().getSizeRule().isTargetSize()
                                    && barLocation.x() == 0
                                    && barLocation.barAnchor().isRight() != right) {
                                break;
                            }
                        }
                        resizeHover.first(bar);
                        resizeHover.right(right);
                        // [NEOFORGE] context.requestCursor(CursorTypes.RESIZE_EW) → GLFW set in render()
                        setResizeCursorThisFrame = true;
                        break rectLoop;
                    } else {
                        resizeHover.first(null);
                    }
                }
            }
        }
    }

    private static int getNeighborInsertX(BarLocation barLocation, boolean right) {
        BarPositioner.BarAnchor barAnchor = barLocation.barAnchor();
        int gridX = barLocation.x();
        if (barAnchor == null) return 0;
        return right ? (barAnchor.isRight() ? gridX + 1 : gridX) : (barAnchor.isRight() ? gridX : gridX + 1);
    }

    private static int getNeighborInsertY(BarLocation barLocation, boolean up) {
        BarPositioner.BarAnchor barAnchor = barLocation.barAnchor();
        int gridY = barLocation.y();
        if (barAnchor == null) return 0;
        return up ? (barAnchor.isUp() ? gridY + 1 : gridY) : (barAnchor.isUp() ? gridY : gridY + 1);
    }

    @Override
    protected void init() {
        super.init();
        FancyStatusBars.updatePositions(true);
        editBarWidget = new EditBarWidget(0, 0, this);
        editBarWidget.visible = false;
        addWidget(editBarWidget);
        FancyStatusBars.statusBars.values().forEach(this::setup);
        updateScreenRects();

        this.addRenderableWidget(Button.builder(
                        Component.literal("?"),
                        button -> minecraft.setScreen(
                                new PopupScreen.Builder(this, Component.translatable("shiny_sb.bars.config.explanationTitle"))
                                        .addButton(Component.translatable("gui.ok"), PopupScreen::onClose)
                                        .setMessage(Component.translatable("shiny_sb.bars.config.explanation"))
                                        .build()))
                .bounds(width - 20, (height - 15) / 2, 15, 15)
                .build());
    }

    private void setup(StatusBar statusBar) {
        this.addRenderableWidget(statusBar);
        statusBar.setOnClick(this::onBarClick);
    }

    @Override
    public void removed() {
        super.removed();
        FancyStatusBars.statusBars.values().forEach(statusBar -> statusBar.setOnClick(null));
        if (cursorBar != null) cursorBar.inMouse = false;
        FancyStatusBars.updatePositions(false);
        FancyStatusBars.saveBarConfig();
        // Reset cursor when screen is closed
        GLFW.glfwSetCursor(minecraft.getWindow().getWindow(), 0L);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    /**
     * [NEOFORGE] Was onBarClick(StatusBar, MouseButtonEvent).
     * Now uses the updated StatusBar.OnClick signature: (StatusBar, double, double, int).
     */
    private void onBarClick(StatusBar statusBar, double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            cursorOffset = new ScreenPosition(
                    (int) (statusBar.getX() - mouseX),
                    (int) (statusBar.getY() - mouseY));
            cursorBar = statusBar;
            cursorBar.inMouse = true;
            cursorBar.enabled = true;
            currentInsertLocation = BarLocation.of(cursorBar);
            if (statusBar.anchor != null)
                FancyStatusBars.barPositioner.removeBar(statusBar.anchor, statusBar.gridY, statusBar);
            FancyStatusBars.updatePositions(true);
            cursorBar.setX(width + 5);
            updateScreenRects();
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            int x = (int) Math.min(mouseX - 1, width  - editBarWidget.getWidth());
            int y = (int) Math.min(mouseY - 1, height - editBarWidget.getHeight());
            editBarWidget.visible = true;
            editBarWidget.setStatusBar(statusBar);
            editBarWidget.setX(x);
            editBarWidget.setY(y);
        }
    }

    private void updateScreenRects() {
        rectToBar.clear();
        FancyStatusBars.statusBars.values().forEach(statusBar -> {
            if (!statusBar.enabled) return;
            rectToBar.put(
                    new ScreenRectangle(new ScreenPosition(statusBar.getX(), statusBar.getY()),
                            statusBar.getWidth(), statusBar.getHeight()),
                    Pair.of(statusBar, BarLocation.of(statusBar)));
        });
    }

    /**
     * [NEOFORGE] Fabric: mouseReleased(MouseButtonEvent click)
     *            NeoForge/vanilla: mouseReleased(double mouseX, double mouseY, int button)
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (cursorBar != null) {
            cursorBar.inMouse = false;
            if (currentInsertLocation == BarLocation.NULL) {
                cursorBar.x = (float) ((mouseX + cursorOffset.x()) / width);
                cursorBar.y = (float) ((mouseY + cursorOffset.y()) / height);
                cursorBar.width = Math.clamp(cursorBar.width, (float) BAR_MINIMUM_WIDTH / width, 1);
            }
            currentInsertLocation = BarLocation.NULL;
            cursorBar = null;
            FancyStatusBars.updatePositions(true);
            updateScreenRects();
            return true;
        } else if (resizing) {
            resizing = false;
            StatusBar bar = null;
            if (resizedBars.left()  != null) bar = resizedBars.left();
            else if (resizedBars.right() != null) bar = resizedBars.right();
            if (bar != null && bar.anchor == null) {
                bar.x     = (float) bar.getX()     / width;
                bar.width = (float) bar.getWidth()  / width;
            }
            resizedBars.left(null);
            resizedBars.right(null);
            updateScreenRects();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /**
     * [NEOFORGE] Fabric: mouseClicked(MouseButtonEvent click, boolean doubled)
     *            NeoForge/vanilla: mouseClicked(double mouseX, double mouseY, int button)
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        StatusBar first = resizeHover.first();
        if (!editBarWidget.isMouseOver(mouseX, mouseY) && button == 0 && first != null) {
            BarPositioner.BarAnchor barAnchor = first.anchor;
            if (barAnchor != null) {
                if (resizeHover.rightBoolean()) {
                    resizedBars.left(first);
                    resizedBars.right(FancyStatusBars.barPositioner.hasNeighbor(barAnchor, first.gridY, first.gridX, true)
                            ? FancyStatusBars.barPositioner.getBar(barAnchor, first.gridY, first.gridX + (barAnchor.isRight() ? 1 : -1))
                            : null);
                } else {
                    resizedBars.right(first);
                    resizedBars.left(FancyStatusBars.barPositioner.hasNeighbor(barAnchor, first.gridY, first.gridX, false)
                            ? FancyStatusBars.barPositioner.getBar(barAnchor, first.gridY, first.gridX + (barAnchor.isRight() ? -1 : 1))
                            : null);
                }
            } else {
                if (resizeHover.rightBoolean()) { resizedBars.left(first);  resizedBars.right(null); }
                else                            { resizedBars.right(first); resizedBars.left(null);  }
            }
            resizing = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
