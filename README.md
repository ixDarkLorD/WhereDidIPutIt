<p align="center">
  <img src="https://raw.githubusercontent.com/ixDarkLorD/WhereDidIPutIt/26.1.2/.github/assets/banner.png" alt="Where Did I Put It?" width="100%">
</p>

<p align="center"><b>Hover an item, press a key, and the nearest container holding it lights up.</b></p>

<p align="center">
  <a href="https://modrinth.com/mod/where-did-i-put-it">Modrinth</a> ·
  <a href="https://github.com/ixDarkLorD/WhereDidIPutIt/issues">Issues</a> ·
  <a href="https://discord.gg/ZapGEPqm2V">Discord</a>
</p>

<p align="center">
  <img src="https://raw.githubusercontent.com/ixDarkLorD/ModResources/main/badges/fabric.svg" alt="Fabric" height="26">
  <img src="https://raw.githubusercontent.com/ixDarkLorD/ModResources/main/badges/neoforge.svg" alt="NeoForge" height="26">
  <img src="https://raw.githubusercontent.com/ixDarkLorD/ModResources/main/badges/forge.svg" alt="Forge" height="26">
  <img src="https://raw.githubusercontent.com/ixDarkLorD/ModResources/main/badges/client-side.svg" alt="Client side" height="26">
</p>

<hr>

## ✨ About

Where Did I Put It? remembers what's in every chest, barrel and shulker box you open, so finding something is one key
press instead of opening twenty chests. It works **client side on any server**; with the mod on the server too,
searches look in the real containers around you, even ones you never opened.

## 🧩 Features

- **🔍 Find anything.** **F** on an item (in your inventory, a chest, JEI or REI) points out the nearest container that
  has it; **G** points out all of them. The container glows through walls with a gold outline and a see-through ghost
  of itself, the item floats over it with its count, and an arrow points the way while it's off screen.
- **🧭 Led there.** A glowing line runs from your feet straight to the container, and a compass at the top of the
  screen keeps pointing until you open it.
- **🗂️ Search and browse.** A search screen (**Y**) over everything you've stored, by name, `#tag` or `@mod`, and a
  container browser (**K**) of every container you've opened, with what it held. Hold Shift on an item for where it is.
- **🧠 More finding.** Optional keys for similar items (same enchantment or potion), room for what you hold, the
  ingredients a recipe is missing, and a peek into a container from across the room.
- **🌐 Live search.** With the mod on the server, searches ask the server, which decides how far and how often, skips
  anything you couldn't open, and only ever says where an item is and how many. Teams can share what they've seen.
- **📦 Storage mods.** Applied Energistics 2, Refined Storage, Simple Storage Network and Tom's Simple Storage terminals
  are remembered with their whole network, and the find key works in their grids.

## 📦 Download

| Modrinth |
|---|
| [where-did-i-put-it](https://modrinth.com/mod/where-did-i-put-it) |

It needs [CoolCatLib](https://modrinth.com/mod/coolcatlib), and [Fabric API](https://modrinth.com/mod/fabric-api) on
Fabric. JEI, REI and the storage mods are optional.

## 🧭 Versions

Each Minecraft version lives on its own branch.

| Minecraft | Loaders | Branch | Status | Latest |
|---|---|---|:-:|---|
| **26.3** | Fabric, NeoForge | [`26.3`](https://github.com/ixDarkLorD/WhereDidIPutIt/tree/26.3) | ✅ Supported | 26.3-1 |
| **26.2** | Fabric, NeoForge | [`26.2`](https://github.com/ixDarkLorD/WhereDidIPutIt/tree/26.2) | ✅ Supported | 26.2-1 |
| **26.1 – 26.1.2** | Fabric, NeoForge | [`26.1.2`](https://github.com/ixDarkLorD/WhereDidIPutIt/tree/26.1.2) | ✅ Supported | 26.1.2-1 |
| **1.21.1** | Fabric, NeoForge, Forge | [`1.21-1.21.1`](https://github.com/ixDarkLorD/WhereDidIPutIt/tree/1.21-1.21.1) | ✅ Supported | 1.21.1-1 |
| **1.20.1** | Fabric, Forge | [`1.20.1`](https://github.com/ixDarkLorD/WhereDidIPutIt/tree/1.20.1) | ✅ Supported | 1.20.1-1 |

## 🛠️ Building

Check out the branch for your Minecraft version, then run:

```bash
./gradlew build                  # the jars, in each loader's build/libs
./gradlew :fabric:runClient      # a game with the mod (or :neoforge: / :forge:)
```

It needs the Java its Minecraft version runs on (25 for 26.x, 21 for 1.21.1, 17 for 1.20.1). CoolCatLib comes from the
ModResources Maven, and the build scripts and dependency versions from
[ModResources](https://github.com/ixDarkLorD/ModResources); each release is published to its Maven automatically.

## 🐞 Issues and ideas

Found a bug or have an idea? Open an [issue](https://github.com/ixDarkLorD/WhereDidIPutIt/issues), or drop by the
[Discord](https://discord.gg/ZapGEPqm2V).

## 📄 License

Where Did I Put It? is under the [Mozilla Public License 2.0](LICENSE). You can use it in modpacks, make videos and
reviews of it, and reuse or change its code, as long as files you change stay under the same license.
