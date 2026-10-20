package net.ixdarklord.wheredidiputit.mixin.client;

import net.ixdarklord.wheredidiputit.find.FindKey;
import net.ixdarklord.wheredidiputit.find.ScreenNotice;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Runs the find key on the hovered slot. At the head of the base class's {@code keyPressed}, so screens with text
 * fields (creative search, anvil, recipe book) have already taken the key when one of them is typed in, and before
 * the game's own offhand swap, which also sits on F. Also draws the {@link ScreenNotice} above the panel.
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
    private void wheredidiputit$onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        ItemStack hovered = this.hoveredSlot != null ? this.hoveredSlot.getItem() : ItemStack.EMPTY;
        if (FindKey.onKeyPressed(this, event, hovered)) cir.setReturnValue(true);
    }

    // extractContents, not extractRenderState: the recipe book screens draw their contents without the latter.
    @Inject(method = "extractContents", at = @At("TAIL"))
    private void wheredidiputit$extractNotice(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        ScreenNotice.extract(graphics, this.leftPos + this.imageWidth / 2, this.topPos);
    }
}
