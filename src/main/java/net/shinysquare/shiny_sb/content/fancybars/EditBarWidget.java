package net.shinysquare.shiny_sb.content.fancybars;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.awt.Color;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.CommonColors;

public class EditBarWidget extends AbstractContainerWidget {

    private final EnumCyclingOption<StatusBar.IconPosition> iconOption;
    private final EnumCyclingOption<StatusBar.TextPosition> textOption;
    private final BooleanOption showMaxOption;
    private final BooleanOption showOverflowOption;
    private final ColorOption color1;
    private final ColorOption color2;
    private final ColorOption textColor;
    private final RunnableOption hideOption;
    private final StringWidget nameWidget;
    private final List<? extends AbstractWidget> options;
    private int contentsWidth = 0;

    public EditBarWidget(int x, int y, Screen parent) {
        super(x, y, 100, 99, Component.literal("Edit bar"));

        Font textRenderer = Minecraft.getInstance().font;
        nameWidget = new StringWidget(Component.empty(), textRenderer);

        MutableComponent translatable = Component.translatable("shiny_sb.bars.config.icon");
        iconOption = new EnumCyclingOption<>(0, 11, getWidth(), translatable, StatusBar.IconPosition.class);
        contentsWidth = Math.max(contentsWidth, textRenderer.width(translatable) + iconOption.getLongestOptionWidth() + 10);

        translatable = Component.translatable("shiny_sb.bars.config.text");
        textOption = new EnumCyclingOption<>(0, 22, getWidth(), translatable, StatusBar.TextPosition.class);
        contentsWidth = Math.max(contentsWidth, textRenderer.width(translatable) + textOption.getLongestOptionWidth() + 10);

        translatable = Component.translatable("shiny_sb.bars.config.showMax");
        showMaxOption = new BooleanOption(0, 33, getWidth(), translatable);
        contentsWidth = Math.max(contentsWidth, textRenderer.width(translatable) + 9 + 10);

        translatable = Component.translatable("shiny_sb.bars.config.showOverflow");
        showOverflowOption = new BooleanOption(0, 44, getWidth(), translatable);
        contentsWidth = Math.max(contentsWidth, textRenderer.width(translatable) + 9 + 10);

        translatable = Component.translatable("shiny_sb.bars.config.mainColor");
        contentsWidth = Math.max(contentsWidth, textRenderer.width(translatable) + 9 + 10);
        color1 = new ColorOption(0, 55, getWidth(), translatable, parent);

        translatable = Component.translatable("shiny_sb.bars.config.overflowColor");
        contentsWidth = Math.max(contentsWidth, textRenderer.width(translatable) + 9 + 10);
        color2 = new ColorOption(0, 66, getWidth(), translatable, parent);

        translatable = Component.translatable("shiny_sb.bars.config.textColor");
        contentsWidth = Math.max(contentsWidth, textRenderer.width(translatable) + 9 + 10);
        textColor = new ColorOption(0, 77, getWidth(), translatable, parent);

        translatable = Component.translatable("shiny_sb.bars.config.hide");
        contentsWidth = Math.max(contentsWidth, textRenderer.width(translatable) + 9 + 10);
        hideOption = new RunnableOption(0, 88, getWidth(), translatable);

        options = List.of(iconOption, textOption, showMaxOption, showOverflowOption,
                color1, color2, textColor, hideOption);
        setWidth(contentsWidth);
    }

    @Override
    public List<? extends GuiEventListener> children() { return options; }

    public int insideMouseX = 0;
    public int insideMouseY = 0;

    @Override
    protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        if (isHovered()) {
            insideMouseX = mouseX;
            insideMouseY = mouseY;
        } else {
            int i = mouseX - insideMouseX;
            int j = mouseY - insideMouseY;
            if (i * i + j * j > 30 * 30) visible = false;
        }
        context.pose().pushPose();
        context.pose().translate(getX(), getY(), 0);
        TooltipRenderUtil.renderTooltipBackground(context, 0, 0, getWidth(), getHeight(), 0);
        nameWidget.render(context, mouseX, mouseY, delta);
        for (AbstractWidget option : options) option.render(context, mouseX - getX(), mouseY - getY(), delta);
        context.pose().popPose();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {}

    /**
     * [NEOFORGE] Fabric: mouseClicked(MouseButtonEvent click, boolean doubled)
     *            NeoForge: mouseClicked(double mouseX, double mouseY, int button)
     *
     * The children are rendered with a matrix translation to (getX(), getY()), so their
     * local positions start at (0, y). We must subtract getX()/getY() from the raw
     * screen coordinates before delegating to the children so their isMouseOver() hits correctly.
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        if (!isHovered()) visible = false;
        // Translate to local widget space so children at (0, y) can match
        return super.mouseClicked(mouseX - getX(), mouseY - getY(), button);
    }

    public void setStatusBar(StatusBar statusBar) {
        iconOption.setCurrent(statusBar.getIconPosition());
        iconOption.setOnChange(statusBar::setIconPosition);
        textOption.setCurrent(statusBar.getTextPosition());
        textOption.setOnChange(statusBar::setTextPosition);

        color1.setCurrent(statusBar.getColors()[0].getRGB());
        color1.setOnChange(color -> statusBar.getColors()[0] = color);

        showMaxOption.active = statusBar.hasMax();
        showMaxOption.setCurrent(statusBar.showMax);
        showOverflowOption.active = statusBar.hasOverflow();
        showOverflowOption.setCurrent(statusBar.showOverflow);
        showMaxOption.setOnChange(showMax -> statusBar.showMax = showMax);
        showOverflowOption.setOnChange(showOverflow -> statusBar.showOverflow = showOverflow);

        color2.active = statusBar.hasOverflow();
        if (color2.active) {
            color2.setCurrent(statusBar.getColors()[1].getRGB());
            color2.setOnChange(color -> statusBar.getColors()[1] = color);
        }
        if (statusBar.getTextColor() != null) textColor.setCurrent(statusBar.getTextColor().getRGB());
        textColor.setOnChange(statusBar::setTextColor);

        hideOption.active = statusBar.enabled;
        hideOption.setRunnable(() -> {
            if (statusBar.anchor != null)
                FancyStatusBars.barPositioner.removeBar(statusBar.anchor, statusBar.gridY, statusBar);
            statusBar.enabled = false;
            FancyStatusBars.updatePositions(true);
        });

        MutableComponent formatted = statusBar.getName().copy().withStyle(ChatFormatting.BOLD);
        nameWidget.setMessage(formatted);
        setWidth(Math.max(Minecraft.getInstance().font.width(formatted), contentsWidth));
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        for (AbstractWidget option : options) option.setWidth(width);
        nameWidget.setWidth(width);
    }


    // ── Inner option widgets ───────────────────────────────────────────────────

    public class RunnableOption extends AbstractWidget {
        private Runnable runnable;

        public RunnableOption(int x, int y, int width, Component message) {
            super(x, y, width, 11, message);
        }

        public void setRunnable(Runnable r) { this.runnable = r; }

        @Override
        protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
            if (isMouseOver(mouseX, mouseY))
                context.fill(getX(), getY(), getRight(), getBottom(), 0x20FFFFFF);
            Font font = Minecraft.getInstance().font;
            context.drawString(font, getMessage(), getX() + 1, getY() + 1,
                    active ? CommonColors.WHITE : CommonColors.GRAY, true);
        }

        /**
         * [NEOFORGE] Fabric's AbstractWidget.onClick(MouseButtonEvent, boolean)
         *            → vanilla onClick(double mouseX, double mouseY)
         */
        @Override
        public void onClick(double mouseX, double mouseY) {
            EditBarWidget.this.visible = false;
            if (runnable != null) runnable.run();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput builder) {}
    }

    public static class EnumCyclingOption<T extends Enum<T>> extends AbstractWidget {
        private T current;
        private final T[] values;
        private Consumer<T> onChange = null;

        public EnumCyclingOption(int x, int y, int width, Component message, Class<T> enumClass) {
            super(x, y, width, 11, message);
            values = enumClass.getEnumConstants();
            current = values[0];
        }

        @Override
        protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
            if (isMouseOver(mouseX, mouseY))
                context.fill(getX(), getY(), getRight(), getBottom(), 0x20FFFFFF);
            Font font = Minecraft.getInstance().font;
            context.drawString(font, getMessage(), getX() + 1, getY() + 1, CommonColors.WHITE, true);
            String s = current.toString();
            context.drawString(font, s, getRight() - font.width(s) - 1, getY() + 1, CommonColors.WHITE, true);
        }

        public void setCurrent(T current) { this.current = current; }

        /** [NEOFORGE] onClick(double, double) instead of onClick(MouseButtonEvent, boolean) */
        @Override
        public void onClick(double mouseX, double mouseY) {
            current = cycleEnum(current);
            if (onChange != null) onChange.accept(current);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput builder) {}

        public void setOnChange(Consumer<T> onChange) { this.onChange = onChange; }

        int getLongestOptionWidth() {
            int m = 0;
            for (T value : values) m = Math.max(m, Minecraft.getInstance().font.width(value.toString()));
            return m;
        }
    }

    public static class BooleanOption extends AbstractWidget {
        private boolean current = false;
        private BooleanConsumer onChange = null;

        public BooleanOption(int x, int y, int width, Component message) {
            super(x, y, width, 11, message);
        }

        @Override
        protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
            if (isMouseOver(mouseX, mouseY))
                context.fill(getX(), getY(), getRight(), getBottom(), 0x20FFFFFF);
            Font font = Minecraft.getInstance().font;
            context.drawString(font, getMessage(), getX() + 1, getY() + 1,
                    active ? -1 : CommonColors.GRAY, true);
            // [NEOFORGE] drawBorder inlined from HudHelper.drawBorder()
            EditBarColorPopup.drawBorder(context, getRight() - 10, getY() + 1, 9, 9,
                    active ? -1 : CommonColors.GRAY);
            if (current && active)
                context.fill(getRight() - 8, getY() + 3, getRight() - 3, getY() + 8, CommonColors.WHITE);
        }

        /** [NEOFORGE] onClick(double, double) */
        @Override
        public void onClick(double mouseX, double mouseY) {
            current = !current;
            if (onChange != null) onChange.accept(current);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput builder) {}

        public void setCurrent(boolean current)          { this.current = current; }
        public void setOnChange(BooleanConsumer onChange) { this.onChange = onChange; }
    }

    public static class ColorOption extends AbstractWidget {
        private int current = 0;
        private Consumer<Color> onChange = null;
        private final Screen parent;

        public ColorOption(int x, int y, int width, Component message, Screen parent) {
            super(x, y, width, 11, message);
            this.parent = parent;
        }

        public void setCurrent(int current) { this.current = current; }

        @Override
        protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
            if (isMouseOver(mouseX, mouseY))
                context.fill(getX(), getY(), getRight(), getBottom(), 0x20FFFFFF);
            Font font = Minecraft.getInstance().font;
            context.drawString(font, getMessage(), getX() + 1, getY() + 1,
                    active ? -1 : CommonColors.GRAY, true);
            EditBarColorPopup.drawBorder(context, getRight() - 10, getY() + 1, 9, 9,
                    active ? -1 : CommonColors.GRAY);
            context.fill(getRight() - 8, getY() + 3, getRight() - 3, getY() + 8,
                    active ? current : CommonColors.GRAY);
        }

        /** [NEOFORGE] onClick(double, double) */
        @Override
        public void onClick(double mouseX, double mouseY) {
            Minecraft.getInstance().setScreen(
                    new EditBarColorPopup(Component.literal("Edit ").append(getMessage()), parent, this::set));
        }

        private void set(Color color) {
            current = color.getRGB();
            if (onChange != null) onChange.accept(color);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput builder) {}

        public void setOnChange(Consumer<Color> onChange) { this.onChange = onChange; }
    }

    /**
     * [NEOFORGE] Replaces Skyblocker's EnumUtils.cycle().
     * Cycles to the next enum constant, wrapping around.
     */
    @SuppressWarnings("unchecked")
    private static <T extends Enum<T>> T cycleEnum(T current) {
        T[] values = (T[]) current.getDeclaringClass().getEnumConstants();
        return values[(current.ordinal() + 1) % values.length];
    }
}
