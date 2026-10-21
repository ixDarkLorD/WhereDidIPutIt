package net.ixdarklord.wheredidiputit.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

/**
 * A button in the screens' flat look ({@link ScreenStyle}): plain, filled with the accent ({@link Kind#PRIMARY}), or
 * turning red under the mouse for what can't be undone ({@link Kind#DANGER}).
 */
public final class FlatButton extends AbstractButton {
    private static final int PLAIN = 0xFF262D37;
    private static final int PLAIN_HOVER = 0xFF323B47;
    private static final int DISABLED = 0xFF1A1F27;

    public enum Kind {
        PLAIN,
        PRIMARY,
        DANGER
    }

    private final Kind kind;
    private final Runnable action;

    public FlatButton(int x, int y, int width, int height, Component message, Kind kind, Runnable action) {
        super(x, y, width, height, message);
        this.kind = kind;
        this.action = action;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        this.action.run();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        boolean hovered = this.isHoveredOrFocused() && this.active;
        int fill;
        int text = ScreenStyle.TEXT;
        if (!this.active) {
            fill = DISABLED;
            text = ScreenStyle.FAINT;
        } else if (this.kind == Kind.PRIMARY) {
            fill = hovered ? ScreenStyle.tint(0xFFFFFFFF, 0.75F) : ScreenStyle.accent();
            text = 0xFF14181F;
        } else if (this.kind == Kind.DANGER && hovered) {
            fill = ScreenStyle.DANGER;
        } else {
            fill = hovered ? PLAIN_HOVER : PLAIN;
        }
        ScreenStyle.rounded(graphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), ARGB.multiplyAlpha(fill, this.alpha));
        Font font = Minecraft.getInstance().font;
        int width = font.width(this.getMessage());
        graphics.text(font, this.getMessage(), this.getX() + (this.getWidth() - width) / 2, this.getY() + (this.getHeight() - 8) / 2, text, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
