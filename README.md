<p align="center">
  <img src="https://github.com/NomNuggetNom/mcctf/assets/1479568/cdd5190e-2f1d-4b79-a27e-97f182e49e55" />
</p>
<p align="center"><b>MCCTF - The classic MPVP gamemode reborn</b></p>

# Getting Started

## Basics

The JAR file can be found in the releases tab. This is a standalone plugin that can be run on a vanilla server. Here is a quick summary of this plugin:

- Runs on Minecraft 1.8.8
- Supports around 700 CTF maps
- Includes all classes up to Wraith
- No modes (ZC, DM, etc) besides plain CTF

## Downloads

1. Download the [CTF map archive](https://discord.com/channels/@me/123296961511030784/1153080239711330314). This is a large file and might take some time, so get it started now.
2. PaperMC 1.8.8 (build 445) is recommended. Head over to their [build explorer](https://papermc.io/downloads/all) and click 1.8.8 on the left. This [direct link](https://api.papermc.io/v2/projects/paper/versions/1.8.8/builds/445/downloads/paper-1.8.8-445.jar) might work too.
3. Download a compatible Java version. I use [Temurin 17](https://adoptium.net/temurin/releases/). You need to be able to run `java` from your command prompt to start the server - you can follow the [installation instructions here](https://adoptium.net/installation/).
    - For advanced users, consider using [sdkman](https://sdkman.io/install) to install `17.0.3-tem`.
4. You'll also need a few more plugins:
    - [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/). Tested with v4.7.0.
    - [ViaVersion](https://www.spigotmc.org/resources/viaversion.19254/) for allowing newer clients - this is optional.

## Running

Before you install the plugins, run the server on its own. Move the downloaded PaperMC JAR to its own folder, as it will create a lot of folders and files. Then open a command prompt, `cd` to that directory, and run the following console command:

```bash
java -jar paper-1.8.8-455.jar
```

To stop the server, type `stop` into the console. Within the folder that you made, you should now have a folder called `plugins`. If so, you can proceed:

- Open that folder and place the mcctf JAR as well as all the other plugin JARs into that folder
- Create a new folder called `ctf`, then a folder called `maps` in that directory.
- Unzip the map archive into that maps folder. See below for an example.
- Start the server and hope everything works.

- Make sure to turn off spawn protection

### Example File Layout

```
documents/
├─ ctf_server/
│  ├─ plugins/
│  │  ├─ mcctf.jar
│  │  ├─ LuckPerms-Bukkit.jar
│  │  ├─ ProtocolLib.jar
│  │  ├─ ctf/
│  │  │  ├─ maps/
│  │  │  │  ├─ 60 - Lighthouse V1/
│  │  │  │  ├─ 65 - Kill Creek V2/
│  │  │  │  ├─ ...etc
│  ├─ paper-1.8.8-445.jar
```

## Known Issues

- Ninjas cannot be damaged by projectiles while invisible

# Configuration

The plugin will automatically create a config file for you, and it lives at `plugins/ctf/config.json`. This file provides many options for tweaking the plugin.

# Development

This plugin is designed to be easy to modify. My suggestion is to create a `server` folder alongside the `src` folder in the root directory of this project. For example:

```
documents/
├─ ctf_dev/
│  ├─ src/main/...
│  ├─ server/
│  │  ├─ plugins/
│  │  ├─ paper-1.8.8-445.jar
```

This project uses Gradle to build the plugin. You should be able to run the following command without installing any additional dependencies:

```
./gradlew shadowJar
```

This will compile the code and build a new plugin JAR. The JAR will automatically be placed in `/server/plugins`. Make sure you don't have multiple versions of the plugin in this folder.

# Credits

- ryguy1 and redslime for their help in modernizing
- Miskey, f1brown, and Ninsanity for help with balancing and testing
- wintergreen3 and Dave01, without whose testing and help, this project would have never been completed
