# Changelog
This file is for listing all the changes to this project

## v26.1.2-1 Release | Unreleased
### ✨ New Features
- Project set up for Fabric and NeoForge (Minecraft 26.1 to 26.1.2), built on CoolCatLib.
- Client and server configs.
- **Container memory**: every chest, barrel, shulker box, hopper, dropper, dispenser or modded container you open is remembered when you close it: where it is, what block it is, what it held (including what's inside shulker boxes and bundles in it) and when. A double chest counts as one. Breaking or replacing a container forgets it, and containers can be forgotten after a number of days. Memory is kept per world or server and per dimension in the game folder, never shared between servers.
- **Find key** (F by default): hover an item in any inventory or container screen and press it. The screen closes and the nearest remembered container holding the item glows through walls for a few seconds, an arrow points to it while it's off-screen, and the action bar says where it is: "Barrel, 14 blocks north-east, 32 seen 5 min ago". Press it again on the same item for the next-nearest container. If nothing has it, the screen stays open and says so. Typing in search boxes is unaffected, and outside screens F still swaps your offhand.
- **Recipe viewers**: the find key also works on items in JEI's and REI's lists, bookmarks and favorites, and in their recipe screens, when they're installed. Neither is required.
