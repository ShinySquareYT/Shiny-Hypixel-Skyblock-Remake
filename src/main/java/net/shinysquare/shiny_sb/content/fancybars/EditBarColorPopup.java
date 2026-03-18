package net.shinysquare.shiny_sb.content.fancybars;

import java.awt.Color;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.lwjgl.glfw.GLFW;

/**
 * [NEOFORGE] Replaces Fabric's AbstractPopupScreen with a plain vanilla Screen.
 *
 * AbstractPopupScreen was a Skyblocker utility that provided:
 *  - drawPopupBackground() — draws a tooltip-style backdrop
 *  - getRectangle() — delegates to Screen
 * Both are reimplemented inline here.
 */
public class EditBarColorPopup extends Screen {

    private final Consumer<Color> setColor;
    private final Screen backgroundScreen;

    private LinearLayout layout = LinearLayout.vertical();
    private BasicColorSelector colorSelector;

    protected EditBarColorPopup(Component title, Screen backgroundScreen, Consumer<Color> setColor) {
        super(title);
        this.backgroundScreen = backgroundScreen;
        this.setColor = setColor;
    }

    @Override
    protected void init() {
        super.init();
        layout = LinearLayout.vertical();
        layout.spacing(8).defaultCellSetting().alignHorizontallyCenter();
        layout.addChild(new StringWidget(title.copy().withStyle(Style.EMPTY.withBold(true)),
                Minecraft.getInstance().font));

        colorSelector = new BasicColorSelector(0, 0, 150, () -> done(null));
        layout.addChild(colorSelector);

        LinearLayout horizontal = LinearLayout.horizontal();
        horizontal.addChild(Button.builder(Component.literal("Cancel"), button -> onClose()).width(80).build());
        horizontal.addChild(Button.builder(Component.literal("Done"), this::done).width(80).build());
        layout.addChild(horizontal);

        layout.visitWidgets(this::addRenderableWidget);
        layout.arrangeElements();
        FrameLayout.centerInRectangle(layout, 0, 0, this.width, this.height);
    }

    private void done(Object ignored) {
        if (colorSelector.validColor) setColor.accept(new Color(colorSelector.getColor()));
        onClose();
    }

    @Override
    public void onClose() {
        minecraft.setScreen(backgroundScreen);
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // Dim the world behind the popup
        super.renderBackground(context, mouseX, mouseY, delta);
        // Draw tooltip-style background box behind the layout
        drawPopupBackground(context, layout.getX(), layout.getY(), layout.getWidth(), layout.getHeight());
    }

    /**
     * [NEOFORGE] Replaces AbstractPopupScreen.drawPopupBackground().
     * Draws a semi-transparent dark box with a 1-pixel border.
     */
    private static void drawPopupBackground(GuiGraphics context, int x, int y, int w, int h) {
        int padding = 6;
        int bx = x - padding, by = y - padding;
        int bw = w + padding * 2, bh = h + padding * 2;
        context.fill(bx, by, bx + bw, by + bh, 0xC0101010);
        drawBorder(context, bx, by, bw, bh, 0xFF999999);
    }

    /**
     * [NEOFORGE] Replaces Fabric's HudHelper.drawBorder().
     * Draws a 1-pixel rectangle border using four context.fill() calls.
     */
    public static void drawBorder(GuiGraphics context, int x, int y, int width, int height, int color) {
        context.fill(x,             y,              x + width, y + 1,          color); // top
        context.fill(x,             y + height - 1, x + width, y + height,     color); // bottom
        context.fill(x,             y,              x + 1,     y + height,     color); // left
        context.fill(x + width - 1, y,              x + width, y + height,     color); // right
    }

    // ─────────────────────────────────────────────────────────────────────────

    private static class BasicColorSelector extends AbstractContainerWidget {

        private final EditBox textFieldWidget;
        private int color = 0xFF000000;
        boolean validColor = false;

        private BasicColorSelector(int x, int y, int width, Runnable onEnter) {
            super(x, y, width, 15, Component.literal("edit color"));

            // [NEOFORGE] Replaces EnterConfirmTextFieldWidget (Skyblocker utility).
            // We use vanilla EditBox and override keyPressed to confirm on Enter.
            textFieldWidget = new EditBox(Minecraft.getInstance().font,
                    getX() + 16, getY(), width - 16, 15, Component.empty()) {
                @Override
                public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                    if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                        onEnter.run();
                        return true;
                    }
                    return super.keyPressed(keyCode, scanCode, modifiers);
                }
            };
            textFieldWidget.setResponder(this::onTextChange);
            textFieldWidget.setFilter(s -> s.length() <= 6);
        }

        int getColor() { return color; }

        private void onTextChange(String text) {
            try {
                color = Integer.parseInt(text, 16) | 0xFF000000;
                validColor = true;
            } catch (NumberFormatException e) {
                color = 0;
                validColor = false;
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(textFieldWidget);
        }

        @Override
        protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
            drawBorder(context, getX(), getY(), 15, 15, validColor ? -1 : 0xFFDD0000);
            context.fill(getX() + 1, getY() + 1, getX() + 14, getY() + 14, color);
            textFieldWidget.render(context, mouseX, mouseY, delta);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput builder) {}

        @Override
        public void setX(int x) {
            super.setX(x);
            textFieldWidget.setX(getX() + 16);
        }

        @Override
        public void setY(int y) {
            super.setY(y);
            textFieldWidget.setY(getY());
        }

        @Override protected int contentHeight() { return 0; }
        @Override protected double scrollRate()  { return 0; }
    }
}
