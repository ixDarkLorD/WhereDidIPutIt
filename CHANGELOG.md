# Changelog
This file is for listing all the changes to this project

## v26.1.2-1 Release | Unreleased
### ✨ New Features
- Project set up for Fabric and NeoForge (Minecraft 26.1 to 26.1.2), built on CoolCatLib.
- Client and server configs.
- **Container memory**: every chest, barrel, shulker box, hopper, dropper, dispenser or modded container you open is remembered when you close it: where it is, what block it is, what it held (including what's inside shulker boxes and bundles in it) and when. A double chest counts as one. Breaking or replacing a container forgets it, and containers can be forgotten after a number of days. Memory is kept per world or server and per dimension in the game folder, never shared between servers.
- **Find key** (F by default): hover an item in any inventory or container screen and press it. The screen closes and the nearest remembered container holding the item glows through walls for a few seconds, and an arrow points to it while it's off-screen. If nothing has it, the screen stays open and says so. Typing in search boxes is unaffected, and outside screens F still swaps your offhand.
- **Recipe viewers**: the find key also works on items in JEI's and REI's lists, bookmarks and favorites, and in their recipe screens, when they're installed. Neither is required.
- **Storage mods**: Applied Energistics 2, Refined Storage, Simple Storage Network and Tom's Simple Storage terminals are remembered with everything their network holds, and the find key works on the items in their grids. A search points to the terminal you used. All optional.
- Containers are named by their screen title, so a chest renamed in an anvil shows its name.
- **Last-seen tooltip**: hold Shift (rebindable) on an item to see where it was last seen: "Last seen in: Barrel at 6, -60, -7 (2 containers)". Items you've stored somewhere get a hint to hold it.
- The highlight's outline has rounded corners and an even, smooth glow, through walls too.
- **Find all**: G (rebindable) points out every container within range that holds the hovered item; F points out the nearest.
- **Ghost and item marker**: a highlighted container behind walls shows as a see-through ghost of itself, and the item you searched for floats over it with its amount in the corner, as in a slot. They replace the action bar text.
- Ghosts are pixel-sharp, and chests (single and double, normal, trapped and copper) show as themselves.
- **Match modes**: the item tag mode matches items of the same kind (any log for an oak log) but leaves out tags about what items are used for, so a diamond no longer turns up iron. The tags it leaves out are a setting. The item shown over a container is the one found there.
