package net.ixdarklord.wheredidiputit.find;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * A key mapping read by whether its key is held right now. The game updates key mappings only while no screen is
 * open, so {@link #isDown()} stays false in an inventory, where a tooltip key is used.
 */
public class HeldKeyMapping extends KeyMapping {
    public HeldKeyMapping(String name, int keysym, Category category) {
        super(name, keysym, category);
    }

    public boolean isHeld() {
        if (this.isUnbound()) return false;
        Window window = Minecraft.getInstance().getWindow();
        return switch (this.key.getType()) {
            // The same check as the game's own hasShiftDown().
            case KEYSYM -> InputConstants.isKeyDown(window, this.key.getValue());
            case MOUSE -> GLFW.glfwGetMouseButton(window.handle(), this.key.getValue()) == GLFW.GLFW_PRESS;
            // A key bound by its scancode alone has no key code to ask about.
            case SCANCODE -> false;
        };
    }
}
