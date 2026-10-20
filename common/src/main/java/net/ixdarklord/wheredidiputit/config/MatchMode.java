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
    ITEM_TAG;

    @Override
    public Component displayName() {
        return Component.translatable("config.wheredidiputit.match_mode." + this.name().toLowerCase(Locale.ROOT));
    }
}
