package net.ixdarklord.wheredidiputit.mixin.client;

import net.ixdarklord.wheredidiputit.find.FindKey;
import net.ixdarklord.wheredidiputit.find.HoveredStacks;
import net.ixdarklord.wheredidiputit.find.IgnoreKey;
import net.ixdarklord.wheredidiputit.find.ScreenNotice;
import net.ixdarklord.wheredidiputit.memory.StorageTerminals;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Runs the find key on the hovered slot. At the head of the base class's {@code keyPressed}, so screens with text
 * fields (creative search, anvil, recipe book) have already taken the key when one of them is typed in, and before
 * the game's own offhand swap, which also sits on F; the {@link IgnoreKey} too. Also draws the {@link ScreenNotice}
 * above the panel.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin extends Screen {
    @Shadow
    protected @Nullable Slot hoveredSlot;
    @Shadow
    protected int leftPos;
    @Shadow
    protected int topPos;
    @Shadow
    protected int imageWidth;

    private AbstractContainerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void wheredidiputit$onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        // A text field that takes typing gets the letter, as it would without the mod (some screens type into one
        // through this method, such as storage terminals with a focused search box).
        if (this.wheredidiputit$typing()) return;
        if (IgnoreKey.onKeyPressed(keyCode, scanCode, (AbstractContainerScreen<?>) (Object) this)) {
            cir.setReturnValue(true);
            return;
        }
        // The slot first, then a storage terminal's own entries, then whatever a recipe viewer has under the mouse.
        ItemStack hovered = this.hoveredSlot != null && this.hoveredSlot.hasItem() ? this.hoveredSlot.getItem() : ItemStack.EMPTY;
        if (hovered.isEmpty()) hovered = StorageTerminals.hovered((AbstractContainerScreen<?>) (Object) this);
        if (hovered.isEmpty()) hovered = HoveredStacks.fromViewers(this);
        if (FindKey.onKeyPressed(keyCode, scanCode, hovered)) cir.setReturnValue(true);
    }

    @Unique
    private boolean wheredidiputit$typing() {
        for (GuiEventListener child : this.children()) {
            if (child instanceof EditBox box && box.canConsumeInput()) return true;
        }
        return false;
    }

    // At the end of the base class's render, over the panel and its tooltip.
    @Inject(method = "render", at = @At("TAIL"))
    private void wheredidiputit$renderNotice(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ScreenNotice.render(graphics, this.leftPos + this.imageWidth / 2, this.topPos);
    }
}
