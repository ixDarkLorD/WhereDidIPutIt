package net.ixdarklord.wheredidiputit.find;

import com.mojang.blaze3d.platform.InputConstants;
import net.ixdarklord.coolcatcore.api.client.registry.KeyMappingRegistry;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.coolcatcore.api.network.Network;
import net.ixdarklord.wheredidiputit.client.render.ContainerHighlight;
import net.ixdarklord.wheredidiputit.config.WDIPIClientConfig;
import net.ixdarklord.wheredidiputit.memory.ContainerMemory;
import net.ixdarklord.wheredidiputit.memory.ContainerRecord;
import net.ixdarklord.wheredidiputit.memory.DimensionMemory;
import net.ixdarklord.wheredidiputit.memory.StoredItem;
import net.ixdarklord.wheredidiputit.network.QuickStackPayload;
import net.ixdarklord.wheredidiputit.network.QuickStackResultPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Quick stack, on the client (author, 2026-10-28): the key asks the server ({@code live/QuickStackServer}) to put the
 * inventory's items into the nearby containers already holding them, then points out where they went (no path line:
 * they're right here) and adds what moved to those containers' memory. Works while playing and in any container
 * screen, the player's inventory included. Needs the mod on the server: the client can't move items itself.
 */
public final class QuickStack {
    /** Unbound by default, as the mod's other added keys. */
    public static final KeyMapping KEY = new KeyMapping("key.wheredidiputit.quick_stack", InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), FindKey.CATEGORY);
    private static final long REPEAT_MILLIS = 250L;

    private static int nextId;
    private static int waitingFor = -1;
    private static long lastPress;

    private QuickStack() {
    }

    public static void init() {
        KeyMappingRegistry.register(KEY);
        ClientTickEvents.END.register(minecraft -> {
            while (KEY.consumeClick()) {
                if (minecraft.gui.screen() == null) request(minecraft);
            }
        });
    }

    /** The key pressed in a container screen (from {@code AbstractContainerScreenMixin}); true when it was used up. */
    public static boolean onKeyPressed(KeyEvent event) {
        if (!KEY.matches(event)) return false;
        request(Minecraft.getInstance());
        return true;
    }

    private static void request(Minecraft minecraft) {
        if (minecraft.player == null || !WDIPIClientConfig.ENABLED.get()) return;
        long now = Util.getMillis();
        if (now - lastPress < REPEAT_MILLIS) return;
        lastPress = now;
        if (!LiveSearch.serverHasMod()) {
            FindKey.tell(minecraft, Component.translatable("gui.wheredidiputit.quick_stack.no_server"), false);
            return;
        }
        waitingFor = ++nextId;
        Network.sendToServer(new QuickStackPayload(waitingFor));
    }

    public static void onResult(QuickStackResultPayload result) {
        if (result.requestId() != waitingFor) return;
        waitingFor = -1;
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) return;
        switch (result.status()) {
            case DISABLED -> FindKey.tell(minecraft, Component.translatable("gui.wheredidiputit.quick_stack.disabled"), false);
            case COOLDOWN -> { }
            case OK -> {
                if (result.moved().isEmpty()) {
                    FindKey.tell(minecraft, Component.translatable("gui.wheredidiputit.quick_stack.nothing"), false);
                    return;
                }
                remember(level, result.moved());
                show(minecraft, level, result.moved());
            }
        }
    }

    // Points out each container that took something, with the item it took most of and how many items it took.
    private static void show(Minecraft minecraft, ClientLevel level, List<QuickStackResultPayload.Moved> moved) {
        Map<BlockPos, List<QuickStackResultPayload.Moved>> byContainer = new LinkedHashMap<>();
        int total = 0;
        for (QuickStackResultPayload.Moved move : moved) {
            byContainer.computeIfAbsent(move.pos(), k -> new ArrayList<>()).add(move);
            total += move.count();
        }
        List<ContainerHighlight.Found> found = new ArrayList<>();
        byContainer.forEach((pos, moves) -> {
            QuickStackResultPayload.Moved most = moves.stream().max(Comparator.comparingInt(QuickStackResultPayload.Moved::count)).orElseThrow();
            found.add(new ContainerHighlight.Found(pos, most.item(), moves.stream().mapToInt(QuickStackResultPayload.Moved::count).sum(), ContainerHighlight.LIVE));
        });
        ContainerHighlight.show(level, found, false);
        FindKey.tell(minecraft, Component.translatable("gui.wheredidiputit.quick_stack.done", TooltipLines.number(total), TooltipLines.containers(byContainer.size())), true);
    }

    // What moved is now in those containers: their remembered contents gain it, so finds and the browser are right
    // before they are opened again. Containers not remembered stay unknown: only part of what they hold is known.
    private static void remember(ClientLevel level, List<QuickStackResultPayload.Moved> moved) {
        DimensionMemory memory = ContainerMemory.dimension(level.dimension());
        if (memory == null) return;
        for (QuickStackResultPayload.Moved move : moved) {
            ContainerRecord record = memory.get(move.pos());
            if (record == null || move.item().isEmpty()) continue;
            List<StoredItem> items = new ArrayList<>(record.items());
            boolean added = false;
            for (int i = 0; i < items.size() && !added; i++) {
                StoredItem item = items.get(i);
                if (ItemStack.isSameItemSameComponents(item.stack(), move.item())) {
                    items.set(i, new StoredItem(item.stack(), item.count() + move.count()));
                    added = true;
                }
            }
            if (!added) items.add(new StoredItem(move.item(), move.count()));
            memory.put(new ContainerRecord(record.pos(), record.block(), record.label(), record.seenAt(), List.copyOf(items), record.network(),
                    record.ignored(), record.slots(), record.entity()));
        }
    }
}
