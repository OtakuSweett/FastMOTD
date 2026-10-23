<img src="https://elytrium.net/src/img/elytrium.webp" alt="Elytrium" align="right">

# FastMOTD

[![Join our Discord](https://img.shields.io/discord/775778822334709780.svg?logo=discord&label=Discord)](https://ely.su/discord)

Ultra-fast, native MOTD plugins for modern Minecraft infrastructure supporting **Paper 26.3** and **Velocity**.

## Modules

The project is structured into two optimized native submodules:

### 1. `fastmotd-paper`
- **Platform:** Native Paper plugin targeting **Paper 26.3** (`paper-plugin.yml`, `Java 25`).
- **Features:**
  - Modern `PaperServerListPingEvent` integration with Adventure Components (`net.kyori.adventure.text.Component`).
  - Zero legacy compatibility hacks (deprecated iterator and profile APIs removed).
  - High-performance caching for server descriptions, icons (`CachedServerIcon`), and custom sample player entries (`ListedPlayerInfo`).
  - Virtual host domain routing (`StatusClient.getVirtualHost()`).
  - Lightweight maintenance mode with `AsyncPlayerPreLoginEvent` rejection.

### 2. `fastmotd-velocity`
- **Platform:** Modern native Velocity proxy plugin.
- **Features:**
  - Direct `ProxyPingEvent` & `PreLoginEvent` event pipeline.
  - Zero legacy backward-compatibility wrappers (`CompatPingListener` and pre-1.16 legacy serialization removed).
  - High-performance caching for player counts, multi-description rotation, dynamic versions, and domain routing.

## Building

Requires **JDK 25** to compile:

```bash
./gradlew build
```

Built JAR artifacts will be located at:
- `fastmotd-paper/build/libs/fastmotd-paper-1.1.0.jar`
- `fastmotd-velocity/build/libs/fastmotd-velocity-1.1.0.jar`
