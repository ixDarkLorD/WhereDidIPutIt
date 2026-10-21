package net.ixdarklord.wheredidiputit.config;

import net.ixdarklord.coolcatcore.api.config.type.EnumType;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** When a stored item counts as the one being searched for. */
public enum MatchMode implements EnumType.Displayable {
    /** The same item with the same components (enchantments, name, damage...). */
    EXACT,
    /** The same item, whatever its components. */
    IGNORE_DATA,
    /** Any item sharing one of the searched item's tags, e.g. any log for an oak log. */
    ITEM_TAG,
    /**
     * Items alike in what they do: sharing an enchantment with an enchanted item or book, holding the same potion as a
     * potion or tipped arrow, otherwise the same item. Last, so the earlier modes keep their place in saved configs.
     */
    SIMILAR;

    @Override
    public Component displayName() {
        return Component.translatable("config.wheredidiputit.match_mode." + this.name().toLowerCase(Locale.ROOT));
    }
}
