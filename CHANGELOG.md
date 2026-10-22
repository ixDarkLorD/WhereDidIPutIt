# Changelog
This file is for listing all the changes to this project

## v1.21.1-1 Release | 2026-10-22
### 🛠️ Changes
- Ported to Minecraft 1.21.1 (Forge, Fabric and NeoForge), with every feature of v26.1.2-1.
- Forge is new: everything works there except the storage mods, none of which has a Forge build for 1.21.1, and REI, which has none either (JEI works).
- Recipe viewers and storage mods use their 1.21.1 builds: JEI, REI (Fabric and NeoForge), Applied Energistics 2 and Simple Storage Network (NeoForge), Refined Storage and Tom's Simple Storage (Fabric and NeoForge).

## v26.1.2-1 Release | Unreleased
### ✨ New Features
- Counts in the search screen, container browser and contents preview shrink to fit their slot when they have three digits or more (128, 1.2k).
- The mod has a logo in the mods list.
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
- **Live search**: on a server with the mod (and in singleplayer), the find keys ask the server, which looks in the real containers round you, so results are always up to date, even for containers you never opened. The server decides how far and how often, skips containers you couldn't open (locked, protected, unlooted) and only says where the item is and how much, never what else a container holds. Without it, or if the server says no, searches use what you remember.
- On NeoForge, the mod no longer stops you joining servers that don't have it, even servers without CoolCatLib (needs the matching CoolCatLib).
- Ghosts are drawn by the game's own renderers with your camera and then made see-through as a whole, so they look like the real block from every side and up close: no inside or back faces show through, and nothing warps. They're unshaded and full bright, so they're easy to see through walls.
- **Search screen** (Y): find any remembered item by name, `#tag` or `@mod`, without holding or hovering it. Left click shows the nearest container, right click all of them.
- **Container browser** (K): every remembered container here, nearest first, searchable by name or contents, with what each held, and buttons to show it, skip it in searches, or forget it. It shrinks to fit small windows.
- The search screen and the container browser have a cleaner look in the highlight color: a header, a search well, inset slots, scrollbars, badges and flat buttons. The browser can sort by nearest, most recent or name, and shows a glimpse of each container's items in its row.
- **Skipping containers**: leave a container (a trash chest, a farm's output) out of searches with the browser or a key (unbound by default) while it's open. It's still remembered.
- The item marker says how long ago a remembered container was last seen ("2m ago"), as what it holds may have changed since. It can be turned off.
- **Tooltips** are tidier: holding Shift shows a section of its own, "Stored: 412 in 2 containers", the nearest container and where it was last seen, with the same look in the sorting hint and the search screen. Only items seen in a container, or found by a recent search (even in containers never opened), get anything at all, and the mod's lines always come last in the tooltip.
- A container you break is forgotten, and stops being highlighted, the moment the server confirms it.
- **Find similar** (key, unbound by default): every container with something like the hovered item: the same enchantment, the same potion, or the same item. Also a match mode ("Similar").
- **Find room** (key, unbound by default): the nearest container with room for the hovered item, one already holding some first. The marker shows how many more fit.
- **Find missing ingredients** (key, unbound by default): for an item you have the recipe for, the containers holding what your inventory lacks to craft it.
- **Path line**: after finding one container, a glowing line runs from your feet straight to it, up or down and through walls, with chevrons drifting the way to go, until you open it. It follows you smoothly as you move and fades out a few blocks ahead. It stops after a minute away from the keyboard.
- **Compass**: after the highlight, a small compass at the top of the screen keeps pointing to the container found, with its distance, until you open it.
- A soft chime sounds from the direction of each container found.
- **Contents preview** (hold a key, unbound by default): the slots of the highlighted container nearest your crosshair, as last seen, with the item found framed.
- **Sorting hints**: with a container open, an item's tooltip says when a container nearby holds more of it.
- **Entity containers**: chest boats, minecarts with a chest or hopper, and donkeys, mules and llamas with a chest are remembered too, and followed as they move.
- **Export and import**: `/wdipi_client export` saves what is remembered in this world to a folder; `/wdipi_client import <name>` merges one in, keeping whichever record of each container is newer.
- **Operator command**: `/wdipi find <item> [radius]` lists the containers holding an item around you, each a click away from a teleport.
- **Team memory** (server setting, off by default): players on the same team share what they see in containers.
- **Live search into storage networks**: on a server with the mod, finds now look into Applied Energistics 2, Refined Storage, Simple Storage Network and Tom's Simple Storage networks through their nearby terminals and controllers. It can be turned off on the server.
