package net.ixdarklord.wheredidiputit.find;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * A key mapping read by whether its key is held right now. The game updates key mappings only while no screen is
 * open, so {@link #isDown()} stays false in an inventory, where a tooltip key is used.
 */
public class HeldKeyMapping extends KeyMapping {
    public HeldKeyMapping(String name, int keysym, String category) {
        super(name, keysym, category);
    }

    public boolean isHeld() {
        if (this.isUnbound()) return false;
        long window = Minecraft.getInstance().getWindow().getWindow();
        // 1.20.1's KeyMapping keeps its key to itself; its saved name says which it is.
        InputConstants.Key key = InputConstants.getKey(this.saveString());
        return switch (key.getType()) {
            // The same check as the game's own hasShiftDown().
            case KEYSYM -> InputConstants.isKeyDown(window, key.getValue());
            case MOUSE -> GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
            // A key bound by its scancode alone has no key code to ask about.
            case SCANCODE -> false;
        };
    }
}
