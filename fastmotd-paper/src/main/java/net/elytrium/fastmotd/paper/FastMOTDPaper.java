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
import com.destroystokyo.paper.network.StatusClient;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;
import net.elytrium.commons.utils.updates.UpdatesChecker;
import net.elytrium.fastmotd.FastMOTD.MaxCountType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import org.bstats.bukkit.Metrics;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

public final class FastMOTDPaper extends JavaPlugin implements Listener {

  private final List<PaperMOTDGenerator> motdGenerators = new ArrayList<>();
  private final List<PaperMOTDGenerator> maintenanceMOTDGenerators = new ArrayList<>();
  private final Int2IntMap protocolPointers = new Int2IntOpenHashMap();
  private final Int2IntMap maintenanceProtocolPointers = new Int2IntOpenHashMap();
  private final Map<String, PaperMOTDGenerator> domainMOTD = new HashMap<>();
  private final Map<String, PaperMOTDGenerator> domainMaintenanceMOTD = new HashMap<>();
  private final Set<InetAddress> kickWhitelist = new HashSet<>();

  private Component kickReason;
  private BukkitTask updaterTask;

  @Override
  public void onEnable() {
    this.reloadPlugin();
    this.getServer().getPluginManager().registerEvents(this, this);

    try {
      new Metrics(this, 15640);
    } catch (Exception e) {
      this.getLogger().fine("Could not start metrics: " + e.getMessage());
    }

    this.getLogger().info("FastMOTD Native Paper 26.3 initialized successfully.");
  }

  @Override
  public void onDisable() {
    if (this.updaterTask != null) {
      this.updaterTask.cancel();
    }
    HandlerList.unregisterAll((Listener) this);
  }

  public void reloadPlugin() {
    File configFile = new File(this.getDataFolder(), "config.yml");
    if (!configFile.exists()) {
      this.getDataFolder().mkdirs();
    }
    PaperSettings.IMP.reload(configFile.toPath());

    if (PaperSettings.IMP.MAIN.ENABLE_UPDATES) {
      this.getServer().getAsyncScheduler().runNow(this, task -> {
        try {
          if (!UpdatesChecker.checkVersionByURL("https://raw.githubusercontent.com/Elytrium/FastMOTD/master/VERSION", PaperSettings.IMP.VERSION)) {
            this.getLogger().warning("A new FastMOTD update is available: https://github.com/Elytrium/FastMOTD/releases/");
          }
        } catch (Exception e) {
          this.getLogger().fine("Update check error: " + e.getMessage());
        }
      });
    }

    ComponentSerializer<Component, Component, String> serializer = PaperSettings.IMP.SERIALIZER.getSerializer();
    if (serializer == null) {
      this.getLogger().severe("Incorrect serializer set: " + PaperSettings.IMP.SERIALIZER);
      return;
    }

    this.protocolPointers.clear();
    this.motdGenerators.clear();
    this.domainMOTD.clear();

    this.maintenanceProtocolPointers.clear();
    this.maintenanceMOTDGenerators.clear();
    this.domainMaintenanceMOTD.clear();

    if (this.updaterTask != null) {
      this.updaterTask.cancel();
    }

    this.kickReason = serializer.deserialize(PaperSettings.IMP.MAINTENANCE.KICK_MESSAGE.replace("{NL}", "\n"));

    this.kickWhitelist.clear();
    for (String host : PaperSettings.IMP.MAINTENANCE.KICK_WHITELIST) {
      try {
        this.kickWhitelist.add(InetAddress.getByName(host));
      } catch (UnknownHostException e) {
        this.getLogger().warning("Could not resolve maintenance whitelist host: " + host);
      }
    }

    Path dataPath = this.getDataFolder().toPath();

    this.generateMOTDGenerators(dataPath, serializer, PaperSettings.IMP.MAIN.VERSION_NAME,
        PaperSettings.IMP.MAIN.DESCRIPTIONS, PaperSettings.IMP.MAIN.FAVICONS,
        PaperSettings.IMP.MAIN.INFORMATION, this.motdGenerators, this.protocolPointers,
        PaperSettings.IMP.MAIN.VERSIONS.DESCRIPTIONS, PaperSettings.IMP.MAIN.VERSIONS.FAVICONS,
        PaperSettings.IMP.MAIN.VERSIONS.INFORMATION, PaperSettings.IMP.MAIN.DOMAINS, this.domainMOTD);

    this.generateMOTDGenerators(dataPath, serializer, PaperSettings.IMP.MAINTENANCE.VERSION_NAME,
        PaperSettings.IMP.MAINTENANCE.DESCRIPTIONS, PaperSettings.IMP.MAINTENANCE.FAVICONS,
        PaperSettings.IMP.MAINTENANCE.INFORMATION, this.maintenanceMOTDGenerators, this.maintenanceProtocolPointers,
        PaperSettings.IMP.MAINTENANCE.VERSIONS.DESCRIPTIONS, PaperSettings.IMP.MAINTENANCE.VERSIONS.FAVICONS,
        PaperSettings.IMP.MAINTENANCE.VERSIONS.INFORMATION, PaperSettings.IMP.MAINTENANCE.DOMAINS, this.domainMaintenanceMOTD);

    long periodTicks = Math.max(1, PaperSettings.IMP.MAIN.UPDATE_RATE / 50);
    this.updaterTask = this.getServer().getScheduler().runTaskTimer(this, this::updateMOTD, 0L, periodTicks);
  }

  private void generateMOTDGenerators(
      Path dataFolder,
      ComponentSerializer<Component, Component, String> serializer,
      String versionName, List<String> defaultDescriptions, List<String> defaultFavicons,
      List<String> defaultInformation, List<PaperMOTDGenerator> dest, Int2IntMap destPointers,
      Map<String, List<String>> descriptionVersions, Map<String, List<String>> faviconVersions,
      Map<String, List<String>> informationVersions, Map<String, PaperSettings.DOMAIN_MOTD_NODE> domainMotd,
      Map<String, PaperMOTDGenerator> domainDest) {

    descriptionVersions = Objects.requireNonNullElseGet(descriptionVersions, HashMap::new);
    faviconVersions = Objects.requireNonNullElseGet(faviconVersions, HashMap::new);
    informationVersions = Objects.requireNonNullElseGet(informationVersions, HashMap::new);
    List<String> nonNullDefaultDescriptions = Objects.requireNonNullElseGet(defaultDescriptions, Collections::emptyList);
    List<String> nonNullDefaultFavicons = Objects.requireNonNullElseGet(defaultFavicons, Collections::emptyList);
    List<String> nonNullDefaultInformation = Objects.requireNonNullElseGet(defaultInformation, Collections::emptyList);

    PaperMOTDGenerator defaultGenerator = new PaperMOTDGenerator(
        this.getServer(), dataFolder, serializer, versionName,
        nonNullDefaultDescriptions, nonNullDefaultFavicons, nonNullDefaultInformation
    );
    dest.add(defaultGenerator);

    Int2ObjectMap<List<String>> protocolDescriptions = new Int2ObjectOpenHashMap<>();
    Int2ObjectMap<List<String>> protocolIcons = new Int2ObjectOpenHashMap<>();
    Int2ObjectMap<List<String>> protocolInformation = new Int2ObjectOpenHashMap<>();

    this.sortByProtocolVersion(descriptionVersions, protocolDescriptions);
    this.sortByProtocolVersion(faviconVersions, protocolIcons);
    this.sortByProtocolVersion(informationVersions, protocolInformation);

    IntSet allProtocols = new IntOpenHashSet();
    allProtocols.addAll(protocolDescriptions.keySet());
    allProtocols.addAll(protocolIcons.keySet());
    allProtocols.addAll(protocolInformation.keySet());

    Map<List<String>, IntSet> protocolsByData = new HashMap<>();

    allProtocols.forEach(protocol -> {
      List<String> key = new ArrayList<>();
      key.addAll(protocolDescriptions.getOrDefault(protocol, nonNullDefaultDescriptions));
      key.addAll(protocolIcons.getOrDefault(protocol, nonNullDefaultFavicons));
      key.addAll(protocolInformation.getOrDefault(protocol, nonNullDefaultInformation));
      protocolsByData.computeIfAbsent(key, k -> new IntOpenHashSet()).add(protocol);
    });

    protocolsByData.values().forEach(identical -> {
      final int idx = dest.size();
      final int key = identical.iterator().nextInt();
      PaperMOTDGenerator generator = new PaperMOTDGenerator(
          this.getServer(), dataFolder, serializer, versionName,
          protocolDescriptions.getOrDefault(key, nonNullDefaultDescriptions),
          protocolIcons.getOrDefault(key, nonNullDefaultFavicons),
          protocolInformation.getOrDefault(key, nonNullDefaultInformation)
      );
      dest.add(generator);
      identical.forEach(p -> destPointers.put(p, idx));
    });

    domainMotd.forEach((domain, motdNode) -> {
      PaperMOTDGenerator generator = new PaperMOTDGenerator(
          this.getServer(), dataFolder, serializer, versionName,
          motdNode.DESCRIPTION, motdNode.FAVICON, motdNode.INFORMATION
      );
      domainDest.put(domain, generator);
    });
  }

  private void sortByProtocolVersion(Map<String, List<String>> src, Int2ObjectMap<List<String>> dest) {
    src.forEach((key, value) -> {
      IntStream range;
      if (key.contains("-")) {
        String[] parts = key.split("-");
        range = IntStream.rangeClosed(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
      } else {
        range = IntStream.of(Integer.parseInt(key));
      }
      range.forEach(protocol -> dest.computeIfAbsent(protocol, p -> new ArrayList<>()).addAll(value));
    });
  }

  private void updateMOTD() {
    int online = this.getOnline();
    int max = this.getMax(online);

    for (PaperMOTDGenerator generator : this.motdGenerators) {
      generator.update(max, online);
    }
    for (PaperMOTDGenerator generator : this.domainMOTD.values()) {
      generator.update(max, online);
    }

    if (PaperSettings.IMP.MAINTENANCE.OVERRIDE_MAX_ONLINE != -1) {
      max = PaperSettings.IMP.MAINTENANCE.OVERRIDE_MAX_ONLINE;
    }
    if (PaperSettings.IMP.MAINTENANCE.OVERRIDE_ONLINE != -1) {
      online = PaperSettings.IMP.MAINTENANCE.OVERRIDE_ONLINE;
    }

    for (PaperMOTDGenerator generator : this.maintenanceMOTDGenerators) {
      generator.update(max, online);
    }
    for (PaperMOTDGenerator generator : this.domainMaintenanceMOTD.values()) {
      generator.update(max, online);
    }
  }

  private int getOnline() {
    int online = this.getServer().getOnlinePlayers().size() + PaperSettings.IMP.MAIN.FAKE_ONLINE_ADD_SINGLE;
    return online * (PaperSettings.IMP.MAIN.FAKE_ONLINE_ADD_PERCENT + 100) / 100;
  }

  private int getMax(int online) {
    MaxCountType type = PaperSettings.IMP.MAIN.MAX_COUNT_TYPE;
    return switch (type) {
      case ADD_SOME -> online + PaperSettings.IMP.MAIN.MAX_COUNT;
      case VARIABLE -> PaperSettings.IMP.MAIN.MAX_COUNT;
    };
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onServerListPing(PaperServerListPingEvent event) {
    StatusClient client = event.getClient();
    int protocol = client.getProtocolVersion();

    String virtualHost = null;
    InetSocketAddress vh = client.getVirtualHost();
    if (vh != null) {
      virtualHost = vh.getHostString() + ":" + vh.getPort();
    }

    if (PaperSettings.IMP.MAIN.LOG_PINGS) {
      this.getLogger().info(event.getAddress() + " is pinging server with protocol " + protocol);
    }

    if (PaperSettings.IMP.MAINTENANCE.MAINTENANCE_ENABLED) {
      PaperMOTDGenerator generator = this.domainMaintenanceMOTD.get(virtualHost);
      if (generator == null) {
        int idx = this.maintenanceProtocolPointers.getOrDefault(protocol, 0);
        generator = this.maintenanceMOTDGenerators.get(idx);
      }
      generator.apply(event, PaperSettings.IMP.MAINTENANCE.SHOW_VERSION);
    } else {
      PaperMOTDGenerator generator = this.domainMOTD.get(virtualHost);
      if (generator == null) {
        int idx = this.protocolPointers.getOrDefault(protocol, 0);
        generator = this.motdGenerators.get(idx);
      }
      generator.apply(event, true);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
    if (!PaperSettings.IMP.MAINTENANCE.MAINTENANCE_ENABLED || !PaperSettings.IMP.MAINTENANCE.SHOULD_KICK_ON_JOIN) {
      return;
    }

    InetAddress ip = event.getAddress();
    if (this.kickWhitelist.contains(ip)) {
      return;
    }

    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, this.kickReason);
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
    ComponentSerializer<Component, Component, String> serializer = PaperSettings.IMP.SERIALIZER.getSerializer();
    if (serializer == null) {
      return true;
    }

    if (command.getName().equalsIgnoreCase("fastmotdreload")) {
      if (!sender.hasPermission("fastmotd.reload")) {
        sender.sendMessage(Component.text("No permission."));
        return true;
      }
      this.reloadPlugin();
      sender.sendMessage(Component.text("FastMOTD configuration reloaded."));
      return true;
    }

    if (command.getName().equalsIgnoreCase("maintenance")) {
      if (!sender.hasPermission("fastmotd.maintenance")) {
        sender.sendMessage(Component.text("No permission."));
        return true;
      }

      if (args.length == 0) {
        sender.sendMessage(serializer.deserialize(PaperSettings.IMP.MAINTENANCE.COMMAND.USAGE));
        return true;
      }

      if (args[0].equalsIgnoreCase("on")) {
        PaperSettings.IMP.MAINTENANCE.MAINTENANCE_ENABLED = true;
        PaperSettings.IMP.save();
        sender.sendMessage(Component.text("Maintenance mode enabled."));
        if (PaperSettings.IMP.MAINTENANCE.SHOULD_KICK_ON_JOIN) {
          for (Player player : this.getServer().getOnlinePlayers()) {
            if (!this.kickWhitelist.contains(player.getAddress().getAddress())) {
              player.kick(this.kickReason);
            }
          }
        }
        return true;
      }

      if (args[0].equalsIgnoreCase("off")) {
        PaperSettings.IMP.MAINTENANCE.MAINTENANCE_ENABLED = false;
        PaperSettings.IMP.save();
        sender.sendMessage(Component.text("Maintenance mode disabled."));
        return true;
      }

      sender.sendMessage(serializer.deserialize(PaperSettings.IMP.MAINTENANCE.COMMAND.USAGE));
      return true;
    }

    return false;
  }
}
