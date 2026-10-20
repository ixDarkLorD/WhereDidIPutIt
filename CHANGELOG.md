# Changelog
This file is for listing all the changes to this project

## v26.1.2-1 Release | Unreleased
### ✨ New Features
- Project set up for Fabric and NeoForge (Minecraft 26.1 to 26.1.2), built on CoolCatLib.
- Client and server configs.
- **Container memory**: every chest, barrel, shulker box, hopper, dropper, dispenser or modded container you open is remembered when you close it: where it is, what block it is, what it held (including what's inside shulker boxes and bundles in it) and when. A double chest counts as one. Breaking or replacing a container forgets it, and containers can be forgotten after a number of days. Memory is kept per world or server and per dimension in the game folder, never shared between servers.
