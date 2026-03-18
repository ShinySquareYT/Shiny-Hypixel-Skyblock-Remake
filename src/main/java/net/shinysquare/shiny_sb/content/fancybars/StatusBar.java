package net.shinysquare.shiny_sb.content.fancybars;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.shinysquare.shiny_sb.ShinyHypixelSBRemake;
import net.shinysquare.shiny_sb.config.ShsbmConfigManager;
import net.shinysquare.shiny_sb.config.UIAndVisualsConfig;
import net.shinysquare.shiny_sb.utils.Utils;
import net.shinysquare.shiny_sb.skyblock.StatusBarTracker;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.util.function.Consumer;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

public class StatusBar implements LayoutElement, Renderable, GuiEventListener, NarratableEntry {

    private static final ResourceLocation BAR_FILL = ResourceLocation.fromNamespaceAndPath(ShinyHypixelSBRemake.MOD_ID, "bars/bar_fill");
    private static final ResourceLocation BAR_BACK  = ResourceLocation.fromNamespaceAndPath(ShinyHypixelSBRemake.MOD_ID, "bars/bar_back");

    public static final int ICON_SIZE = 9;

    // [NEOFORGE] Identifier → ResourceLocation (Yarn mapping vs official mapping)
    private final ResourceLocation icon;
    private final net.shinysquare.shsbm.content.fancybars.StatusBarType type;
    private Color[] colors;
    private @Nullable Color textColor;

    public Color[] getColors()                      { return colors; }
    public boolean hasOverflow()                    { return type.hasOverflow(); }
    public boolean hasMax()                         { return type.hasMax(); }
    public @Nullable Color getTextColor()           { return textColor; }
    public void setTextColor(@Nullable Color c)     { this.textColor = c; }
    public Component getName()                      { return type.getName(); }

    // [NEOFORGE] OnClick uses (double, double, int) — MouseButtonEvent doesn't exist in NeoForge.
    // See mouseClicked() and the OnClick interface at the bottom of this file.
    private @Nullable OnClick onClick = null;

    public int gridX = 0, gridY = 0;
    public float x = 0, y = 0, width = 0;
    public net.shinysquare.shsbm.content.fancybars.BarPositioner.@Nullable BarAnchor anchor = null;
    public int size = 1;
    public float fill = 0, overflowFill = 0;
    public boolean inMouse = false;
    /** Used to hide bars dynamically (e.g. the oxygen bar) */
    public boolean visible = true;
    public boolean enabled = true;

    private Object value = "???";
    private @Nullable Object max = "???";
    private @Nullable Object overflow = "???";

    private int renderX = 0, renderY = 0, renderWidth = 0;

    private IconPosition iconPosition = IconPosition.LEFT;
    private TextPosition textPosition = TextPosition.BAR_CENTER;

    public boolean showMax = false, showOverflow = false;

    public StatusBar(net.shinysquare.shsbm.content.fancybars.StatusBarType type) {
        this.icon   = ResourceLocation.fromNamespaceAndPath(ShinyHypixelSBRemake.MOD_ID, "bars/icons/" + type.getSerializedName());
        this.colors = type.getColors();
        this.textColor = type.getTextColor();
        this.type   = type;
    }

    protected int transparency(int color) {
        if (inMouse) return (color & 0x00FFFFFF) | 0x44_000000;
        return color;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        renderBar(context);
        if (enabled) renderText(context);
    }

    protected ResourceLocation getIcon() { return icon; }

    @SuppressWarnings("incomplete-switch")
    public void renderBar(GuiGraphics context) {
        if (renderWidth <= 0) return;
        int transparency = transparency(-1);

        // [NEOFORGE] blitSprite() has no pipeline argument in vanilla/NeoForge.
        // Tinting is done via RenderSystem.setShaderColor(); see blitSpriteColored().
        switch (iconPosition) {
            case LEFT  -> blitSpriteColored(context, getIcon(), renderX, renderY, ICON_SIZE, ICON_SIZE, transparency);
            case RIGHT -> blitSpriteColored(context, getIcon(), renderX + renderWidth - ICON_SIZE, renderY, ICON_SIZE, ICON_SIZE, transparency);
        }

        int barWidth = iconPosition.equals(IconPosition.OFF) ? renderWidth : renderWidth - ICON_SIZE - 1;
        int barX     = iconPosition.equals(IconPosition.LEFT) ? renderX + ICON_SIZE + 1 : renderX;
        blitSpriteColored(context, BAR_BACK, barX, renderY + 1, barWidth, 7, transparency);
        drawBarFill(context, barX, barWidth);
    }

    /**
     * [NEOFORGE] Replaces context.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, w, h, argb).
     *
     * Fabric patched blitSprite() to accept a RenderPipeline + ARGB. Vanilla/NeoForge 1.21.1
     * does not expose a pipeline argument. Color tinting is achieved through
     * RenderSystem.setShaderColor(), which must be reset to (1,1,1,1) immediately after.
     */
    protected static void blitSpriteColored(GuiGraphics context, ResourceLocation sprite,
                                             int x, int y, int width, int height, int argb) {
        float a = ((argb >> 24) & 0xFF) / 255f;
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8)  & 0xFF) / 255f;
        float b = ( argb        & 0xFF) / 255f;
        RenderSystem.setShaderColor(r, g, b, a);
        context.blitSprite(sprite, x, y, width, height);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    protected void drawBarFill(GuiGraphics context, int barX, int barWidth) {
        renderNineSliceColored(context, BAR_FILL, barX + 1, renderY + 2,
                (int) ((barWidth - 2) * fill), 5, transparency(colors[0].getRGB()));

        if (hasOverflow() && overflowFill > 0) {
            renderNineSliceColored(context, BAR_FILL, barX + 1, renderY + 2,
                    (int) ((barWidth - 2) * Math.min(overflowFill, 1)), 5, transparency(colors[1].getRGB()));
        }
    }

    /**
     * [NEOFORGE] Replaces Fabric's HudHelper.renderNineSliceColored().
     *
     * GuiGraphics.blitSprite() natively handles nine-slice sprites defined in assets/<ns>/atlases.
     * Color tinting uses RenderSystem.setShaderColor() as above.
     */
    public static void renderNineSliceColored(GuiGraphics context, ResourceLocation sprite,
                                               int x, int y, int width, int height, int argb) {
        if (width <= 0 || height <= 0) return;
        float a = ((argb >> 24) & 0xFF) / 255f;
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8)  & 0xFF) / 255f;
        float b = ( argb        & 0xFF) / 255f;
        RenderSystem.setShaderColor(r, g, b, a);
        context.blitSprite(sprite, x, y, width, height);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    public void updateValues(float fill, float overflowFill, Object text,
                             @Nullable Object max, @Nullable Object overflow) {
        this.value       = text;
        this.fill        = Math.clamp(fill, 0, 1);
        this.overflowFill = Math.clamp(overflowFill, 0, 1);
        this.max         = max;
        this.overflow    = overflow;
    }

    public void updateWithResource(StatusBarTracker.Resource resource) {
        updateValues(resource.value() / (float) resource.max(),
                resource.overflow() / (float) resource.max(),
                resource.value(), resource.max(),
                resource.overflow() > 0 ? resource.overflow() : null);
    }

    public void renderText(GuiGraphics context) {
        if (!showText()) return;
        Font textRenderer = Minecraft.getInstance().font;
        int barWidth = iconPosition.equals(IconPosition.OFF) ? renderWidth : renderWidth - ICON_SIZE - 1;
        int barX     = iconPosition.equals(IconPosition.LEFT) ? renderX + ICON_SIZE + 2 : renderX;

        MutableComponent text = Component.literal(this.value.toString())
                .withStyle(style -> style.withColor((textColor == null ? colors[0] : textColor).getRGB()));

        if (hasMax() && showMax && max != null) {
            text.append("/").append(max.toString());
        }
        if (hasOverflow() && showOverflow && overflow != null) {
            MutableComponent lit = Component.literal(" + ")
                    .withStyle(style -> style.withColor(colors[1].getRGB()));
            lit.append(overflow.toString());
            text.append(lit);
        }

        int textWidth = textRenderer.width(text);
        int x = switch (textPosition) {
            case RIGHT      -> barX + barWidth - textWidth;
            case CENTER     -> this.renderX + (renderWidth - textWidth) / 2;
            case BAR_CENTER -> barX + (barWidth - textWidth) / 2;
            default         -> barX;
        };
        int y = this.renderY - 3;

        int color = transparency((textColor == null ? colors[0] : textColor).getRGB());

        // [NEOFORGE] Replaces Fabric's HudHelper.drawOutlinedText()
        drawOutlinedText(context, text, x, y, color);
    }

    /**
     * [NEOFORGE] Replaces Fabric's HudHelper.drawOutlinedText().
     *
     * Draws text with a 1-pixel black outline via 4 offset passes then the foreground pass.
     * Fabric's HudHelper did the same thing internally.
     */
    public static void drawOutlinedText(GuiGraphics context, Component text, int x, int y, int color) {
        Font font = Minecraft.getInstance().font;
        int outline = 0xFF000000;
        context.drawString(font, text, x - 1, y,     outline, false);
        context.drawString(font, text, x + 1, y,     outline, false);
        context.drawString(font, text, x,     y - 1, outline, false);
        context.drawString(font, text, x,     y + 1, outline, false);
        context.drawString(font, text, x,     y,     color,   false);
    }

    public void renderCursor(GuiGraphics context, int mouseX, int mouseY, float delta) {
        int tx = renderX, ty = renderY;
        boolean tg = inMouse;
        renderX = mouseX; renderY = mouseY; inMouse = false;
        render(context, mouseX, mouseY, delta);
        renderX = tx; renderY = ty; inMouse = tg;
    }

    // ── Layout / GUI boilerplate ──────────────────────────────────────────────

    @Override public void setX(int x)      { this.renderX = x; }
    @Override public void setY(int y)      { this.renderY = y; }
    @Override public int getX()            { return renderX; }
    @Override public int getY()            { return renderY; }
    @Override public int getWidth()        { return renderWidth; }
    public void setWidth(int width)        { this.renderWidth = width; }
    @Override public int getHeight()       { return 9; }
    @Override public ScreenRectangle getRectangle() { return LayoutElement.super.getRectangle(); }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= renderX && mouseX <= renderX + getWidth()
            && mouseY >= renderY && mouseY <= renderY + getHeight();
    }

    @Override public void visitWidgets(Consumer<AbstractWidget> consumer) {}
    @Override public void setFocused(boolean focused) {}
    @Override public boolean isFocused() { return false; }
    @Override public NarrationPriority narrationPriority() { return NarrationPriority.NONE; }
    @Override public void updateNarration(NarrationElementOutput builder) {}

    /**
     * [NEOFORGE] Fabric: mouseClicked(MouseButtonEvent click, boolean doubled)
     *            NeoForge/vanilla: mouseClicked(double mouseX, double mouseY, int button)
     *
     * MouseButtonEvent is a Fabric-specific record that wraps mouse input. NeoForge follows
     * the vanilla GuiEventListener signature which uses raw primitives.
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        if (onClick != null) onClick.onClick(this, mouseX, mouseY, button);
        return true;
    }

    public void setOnClick(@Nullable OnClick onClick) { this.onClick = onClick; }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("name", getName())
                .append("gridX", gridX).append("gridY", gridY)
                .append("size", size)
                .append("x", renderX).append("y", renderY)
                .append("width", renderWidth)
                .append("anchor", anchor)
                .toString();
    }

    public IconPosition getIconPosition()               { return iconPosition; }
    public void setIconPosition(IconPosition p)         { this.iconPosition = p; }
    public boolean showText()                           { return textPosition != TextPosition.OFF; }
    public TextPosition getTextPosition()               { return textPosition; }
    public void setTextPosition(TextPosition p)         { this.textPosition = p; }

    // ── Enums ─────────────────────────────────────────────────────────────────

    public enum IconPosition implements StringRepresentable {
        LEFT, RIGHT, OFF;
        @Override public String getSerializedName() { return name(); }
        @Override public String toString() {
            return I18n.get("shiny_sb.bars.config.commonPosition." + name());
        }
    }

    public enum TextPosition implements StringRepresentable {
        LEFT, CENTER, BAR_CENTER, RIGHT, OFF;
        @Override public String getSerializedName() { return name(); }
        @Override public String toString() {
            if (this == CENTER || this == BAR_CENTER)
                return I18n.get("shiny_sb.bars.config.textPosition." + name());
            return I18n.get("shiny_sb.bars.config.commonPosition." + name());
        }
    }

    /**
     * [NEOFORGE] Fabric's OnClick used MouseButtonEvent.
     *            NeoForge uses raw (double mouseX, double mouseY, int button).
     */
    @FunctionalInterface
    public interface OnClick {
        void onClick(StatusBar statusBar, double mouseX, double mouseY, int button);
    }

    // ── JSON serialization ────────────────────────────────────────────────────

    public void loadFromJson(JsonObject object) {
        if (object.has("colors")) {
            JsonArray colors1 = object.get("colors").getAsJsonArray();
            if (colors1.size() < 2 && hasOverflow())
                throw new IllegalStateException("Missing second color of bar that has overflow");
            Color[] newColors = new Color[colors1.size()];
            for (int i = 0; i < colors1.size(); i++)
                newColors[i] = new Color(Integer.parseInt(colors1.get(i).getAsString(), 16));
            this.colors = newColors;
        }
        if (object.has("text_color"))
            this.textColor = new Color(Integer.parseInt(object.get("text_color").getAsString(), 16));

        String maybeAnchor = object.get("anchor").getAsString().trim();
        this.anchor = maybeAnchor.equals("null") ? null : net.shinysquare.shsbm.content.fancybars.BarPositioner.BarAnchor.valueOf(maybeAnchor);

        if (!object.has("enabled")) enabled = anchor != null;
        else enabled = object.get("enabled").getAsBoolean();

        if (anchor != null) {
            this.size  = object.get("size").getAsInt();
            this.gridX = object.get("x").getAsInt();
            this.gridY = object.get("y").getAsInt();
        } else {
            this.width = object.get("size").getAsFloat();
            this.x     = object.get("x").getAsFloat();
            this.y     = object.get("y").getAsFloat();
        }
        if (object.has("icon_position"))
            this.iconPosition = IconPosition.valueOf(object.get("icon_position").getAsString().trim());
        if (object.has("show_text"))
            this.textPosition = object.get("show_text").getAsBoolean() ? TextPosition.BAR_CENTER : TextPosition.OFF;
        if (object.has("text_position"))
            this.textPosition = TextPosition.valueOf(object.get("text_position").getAsString().trim());
        if (object.has("show_max"))     this.showMax     = object.get("show_max").getAsBoolean();
        if (object.has("show_overflow")) this.showOverflow = object.get("show_overflow").getAsBoolean();
    }

    public JsonObject toJson() {
        JsonObject object = new JsonObject();
        JsonArray colors1 = new JsonArray();
        for (Color color : colors) colors1.add(Integer.toHexString(color.getRGB()).substring(2));
        object.add("colors", colors1);
        if (textColor != null)
            object.addProperty("text_color", Integer.toHexString(textColor.getRGB()).substring(2));
        object.addProperty("anchor", anchor != null ? anchor.toString() : "null");
        if (anchor != null) {
            object.addProperty("x", gridX); object.addProperty("y", gridY);
            object.addProperty("size", size);
        } else {
            object.addProperty("size", width); object.addProperty("x", x); object.addProperty("y", y);
        }
        object.addProperty("icon_position", iconPosition.getSerializedName());
        object.addProperty("text_position", textPosition.getSerializedName());
        object.addProperty("show_max", showMax);
        object.addProperty("show_overflow", showOverflow);
        object.addProperty("enabled", enabled);
        return object;
    }

    // ── Subclasses ────────────────────────────────────────────────────────────

    public static class ManaStatusBar extends StatusBar {
        public ManaStatusBar(net.shinysquare.shsbm.content.fancybars.StatusBarType type) { super(type); }

        @Override
        protected void drawBarFill(GuiGraphics context, int barX, int barWidth) {
            if (hasOverflow() && overflowFill > 0) {
                if (overflowFill > fill
                        && ShsbmConfigManager.get().uiAndVisuals.bars.intelligenceDisplay
                            == UIAndVisualsConfig.IntelligenceDisplay.IN_FRONT) {
                    renderNineSliceColored(context, BAR_FILL, barX + 1, getY() + 2,
                            (int) ((barWidth - 2) * Math.min(overflowFill, 1)), 5, transparency(getColors()[1].getRGB()));
                    renderNineSliceColored(context, BAR_FILL, barX + 1, getY() + 2,
                            (int) ((barWidth - 2) * fill), 5, transparency(getColors()[0].getRGB()));
                } else {
                    renderNineSliceColored(context, BAR_FILL, barX + 1, getY() + 2,
                            (int) ((barWidth - 2) * fill), 5, transparency(getColors()[0].getRGB()));
                    renderNineSliceColored(context, BAR_FILL, barX + 1, getY() + 2,
                            (int) ((barWidth - 2) * Math.min(overflowFill, 1)), 5, transparency(getColors()[1].getRGB()));
                }
            } else {
                renderNineSliceColored(context, BAR_FILL, barX + 1, getY() + 2,
                        (int) ((barWidth - 2) * fill), 5, transparency(getColors()[0].getRGB()));
            }
        }

        @Override
        public void updateValues(float fill, float overflowFill, Object text,
                                 @Nullable Object max, @Nullable Object overflow) {
            super.updateValues(fill, overflowFill,
                    StatusBarTracker.isManaEstimated() ? "~" + text : text, max, overflow);
        }
    }

    public static class ExperienceStatusBar extends StatusBar {
        private static final ResourceLocation CLOCK_ICON =
                ResourceLocation.fromNamespaceAndPath(ShinyHypixelSBRemake.MOD_ID, "bars/icons/rift_time");

        public ExperienceStatusBar(net.shinysquare.shsbm.content.fancybars.StatusBarType type) { super(type); }

        @Override
        protected ResourceLocation getIcon() {
            return Utils.isInTheRift() ? CLOCK_ICON : super.getIcon();
        }

        @Override
        public void updateValues(float fill, float overflowFill, Object text,
                                 @Nullable Object max, @Nullable Object overflow) {
            if (Utils.isInTheRift() && text instanceof Integer time) {
                text = time < 60 ? time + "s" : String.format("%dm%02ds", time / 60, time % 60);
            }
            super.updateValues(fill, overflowFill, text, max, overflow);
        }
    }
}
