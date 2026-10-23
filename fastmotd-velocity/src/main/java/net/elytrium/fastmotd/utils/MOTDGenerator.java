/*
 * Copyright (C) 2022 - 2025 Elytrium
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.elytrium.fastmotd.utils;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.Favicon;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.elytrium.fastmotd.FastMOTD;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class MOTDGenerator {

  private final FastMOTD plugin;
  private final ComponentSerializer<Component, Component, String> serializer;
  private final String versionName;
  private final List<String> rawDescriptions;
  private final List<String> rawFavicons;
  private final List<String> rawInformation;

  private final List<Component> descriptions = new ArrayList<>();
  private final List<Favicon> favicons = new ArrayList<>();
  private final List<ServerPing.SamplePlayer> samplePlayers = new ArrayList<>();

  private volatile int cachedOnline = 0;
  private volatile int cachedMax = 0;

  public MOTDGenerator(FastMOTD plugin, ComponentSerializer<Component, Component, String> serializer,
                       String versionName, List<String> descriptions, List<String> favicons, List<String> information) {
    this.plugin = plugin;
    this.serializer = serializer;
    this.versionName = versionName;
    this.rawDescriptions = descriptions;
    this.rawFavicons = favicons;
    this.rawInformation = information;
  }

  public void generate() {
    this.descriptions.clear();
    for (String desc : this.rawDescriptions) {
      this.descriptions.add(this.serializer.deserialize(desc.replace("{NL}", "\n")));
    }
    if (this.descriptions.isEmpty()) {
      this.descriptions.add(Component.empty());
    }

    this.favicons.clear();
    Path configDir = this.plugin.getConfigPath().getParent();
    for (String iconPath : this.rawFavicons) {
      if (iconPath == null || iconPath.isBlank()) {
        continue;
      }
      Path resolved = configDir.resolve(iconPath);
      try {
        if (resolved.toFile().exists()) {
          this.favicons.add(Favicon.create(resolved));
        }
      } catch (IOException e) {
        this.plugin.getLogger().warn("Failed to load favicon: " + resolved, e);
      }
    }

    this.samplePlayers.clear();
    int idx = 0;
    for (String info : this.rawInformation) {
      String legacy = LegacyComponentSerializer.legacySection().serialize(this.serializer.deserialize(info));
      this.samplePlayers.add(new ServerPing.SamplePlayer(legacy, new UUID(0L, idx++)));
    }
  }

  public void update(int max, int online) {
    this.cachedMax = max;
    this.cachedOnline = online;
  }

  public ServerPing apply(ServerPing basePing, ProtocolVersion clientVersion, boolean showVersion) {
    ServerPing.Builder builder = basePing.asBuilder();

    builder.onlinePlayers(this.cachedOnline);
    builder.maximumPlayers(this.cachedMax);

    if (!this.descriptions.isEmpty()) {
      int randomIdx = ThreadLocalRandom.current().nextInt(this.descriptions.size());
      builder.description(this.descriptions.get(randomIdx));
    }

    if (!this.favicons.isEmpty()) {
      int randomIdx = ThreadLocalRandom.current().nextInt(this.favicons.size());
      builder.favicon(this.favicons.get(randomIdx));
    }

    if (!this.samplePlayers.isEmpty()) {
      builder.samplePlayers(this.samplePlayers);
    }

    if (showVersion && this.versionName != null && !this.versionName.isEmpty()) {
      builder.version(new ServerPing.Version(clientVersion.getProtocol(), this.versionName));
    }

    return builder.build();
  }

  public void dispose() {
    this.descriptions.clear();
    this.favicons.clear();
    this.samplePlayers.clear();
  }
}
