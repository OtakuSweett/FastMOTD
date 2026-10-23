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

package net.elytrium.fastmotd.paper;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import com.destroystokyo.paper.event.server.PaperServerListPingEvent.ListedPlayerInfo;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Server;
import org.bukkit.util.CachedServerIcon;

public class PaperMOTDGenerator {

  private final Server server;
  private final ComponentSerializer<Component, Component, String> serializer;
  private final String versionName;
  private final List<Component> descriptions = new ArrayList<>();
  private final List<CachedServerIcon> favicons = new ArrayList<>();
  private final List<ListedPlayerInfo> samplePlayers = new ArrayList<>();

  private volatile int cachedOnline = 0;
  private volatile int cachedMax = 0;

  public PaperMOTDGenerator(Server server, Path dataFolder,
                            ComponentSerializer<Component, Component, String> serializer,
                            String versionName,
                            List<String> rawDescriptions,
                            List<String> rawFavicons,
                            List<String> rawInformation) {
    this.server = server;
    this.serializer = serializer;
    this.versionName = versionName;

    for (String desc : rawDescriptions) {
      this.descriptions.add(this.serializer.deserialize(desc.replace("{NL}", "\n")));
    }

    if (this.descriptions.isEmpty()) {
      this.descriptions.add(Component.empty());
    }

    for (String iconName : rawFavicons) {
      if (iconName == null || iconName.isBlank()) {
        continue;
      }
      File iconFile = dataFolder.resolve(iconName).toFile();
      if (iconFile.exists()) {
        try {
          this.favicons.add(this.server.loadServerIcon(iconFile));
        } catch (Exception e) {
          this.server.getLogger().warning("Could not load favicon: " + iconFile.getAbsolutePath());
        }
      }
    }

    int idx = 0;
    for (String line : rawInformation) {
      String legacyText = LegacyComponentSerializer.legacySection().serialize(this.serializer.deserialize(line));
      this.samplePlayers.add(new ListedPlayerInfo(legacyText, new UUID(0L, idx++)));
    }
  }

  public void update(int max, int online) {
    this.cachedMax = max;
    this.cachedOnline = online;
  }

  public void apply(PaperServerListPingEvent event, boolean showCustomVersion) {
    event.setNumPlayers(this.cachedOnline);
    event.setMaxPlayers(this.cachedMax);

    if (!this.descriptions.isEmpty()) {
      int randomIdx = ThreadLocalRandom.current().nextInt(this.descriptions.size());
      event.motd(this.descriptions.get(randomIdx));
    }

    if (!this.favicons.isEmpty()) {
      int randomIdx = ThreadLocalRandom.current().nextInt(this.favicons.size());
      event.setServerIcon(this.favicons.get(randomIdx));
    }

    if (!this.samplePlayers.isEmpty()) {
      List<ListedPlayerInfo> targetList = event.getListedPlayers();
      targetList.clear();
      targetList.addAll(this.samplePlayers);
    }

    if (showCustomVersion && this.versionName != null && !this.versionName.isEmpty()) {
      event.setVersion(this.versionName);
    }
  }
}
