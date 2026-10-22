package net.ixdarklord.wheredidiputit.find;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.sdl.SDLMouse;

import java.nio.FloatBuffer;

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
        return switch (this.key.getType()) {
            // SDL's keyboard state, as the game reads held keys.
            case KEYBOARD -> InputConstants.isKeyDown(this.key.getValue());
            // SDL's pressed buttons, one bit each from the left button up.
            case MOUSE -> (SDLMouse.SDL_GetMouseState((FloatBuffer) null, null) & (1 << (this.key.getValue() - 1))) != 0;
        };
    }
}
