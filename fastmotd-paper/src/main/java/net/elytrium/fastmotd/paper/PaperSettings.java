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

import java.util.List;
import java.util.Map;
import net.elytrium.commons.kyori.serialization.Serializers;
import net.elytrium.fastmotd.FastMOTD.MaxCountType;
import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.annotations.Final;
import net.elytrium.serializer.language.object.YamlSerializable;

public class PaperSettings extends YamlSerializable {

  public static final PaperSettings IMP = new PaperSettings();

  @Final
  public String VERSION = "1.1.0";

  @Comment({
      @CommentValue("Available serializers:"),
      @CommentValue("LEGACY_AMPERSAND - \"&c&lExample &c&9Text\"."),
      @CommentValue("LEGACY_SECTION - \"§c§lExample §c§9Text\"."),
      @CommentValue("MINIMESSAGE - \"<bold><red>Example</red> <blue>Text</blue></bold>\"."),
      @CommentValue("GSON - Json text format.")
  })
  public Serializers SERIALIZER = Serializers.MINIMESSAGE;

  public MAIN MAIN = new MAIN();
  public MAINTENANCE MAINTENANCE = new MAINTENANCE();

  public static class MAIN {
    public boolean ENABLE_UPDATES = false;
    public String VERSION_NAME = "Paper 26.3";
    public List<String> DESCRIPTIONS = List.of("<gradient:#1e90ff:#00ffff>FastMOTD Native Paper 26.3</gradient>{NL}<gray>Ultra-fast server list ping!");
    public List<String> FAVICONS = List.of("server-icon.png");
    public List<String> INFORMATION = List.of("<yellow>Running native on Paper 26.3", "<aqua>Optimized MOTD engine");
    @Comment(@CommentValue("How frequently player count will be updated (in ms)"))
    public long UPDATE_RATE = 1000;
    public MaxCountType MAX_COUNT_TYPE = MaxCountType.ADD_SOME;
    public int MAX_COUNT = 1;
    public int FAKE_ONLINE_ADD_SINGLE = 0;
    public int FAKE_ONLINE_ADD_PERCENT = 0;
    public boolean LOG_PINGS = false;

    public VERSIONS VERSIONS = new VERSIONS();
    public Map<String, DOMAIN_MOTD_NODE> DOMAINS = Map.of();
  }

  public static class VERSIONS {
    public Map<String, List<String>> DESCRIPTIONS = Map.of();
    public Map<String, List<String>> FAVICONS = Map.of();
    public Map<String, List<String>> INFORMATION = Map.of();
  }

  public static class MAINTENANCE {
    public boolean MAINTENANCE_ENABLED = false;
    public boolean SHOULD_KICK_ON_JOIN = true;
    public String KICK_MESSAGE = "<red>Server is currently under maintenance!{NL}<gray>Please check back later.";
    public int OVERRIDE_ONLINE = -1;
    public int OVERRIDE_MAX_ONLINE = -1;
    public boolean SHOW_VERSION = true;
    public String VERSION_NAME = "<red>Maintenance";
    public List<String> DESCRIPTIONS = List.of("<red><bold>Server Maintenance</bold></red>{NL}<yellow>We will be back shortly!");
    public List<String> FAVICONS = List.of();
    public List<String> INFORMATION = List.of("<red>Currently closed for updates.");

    public COMMAND COMMAND = new COMMAND();
    public List<String> KICK_WHITELIST = List.of();
    public VERSIONS VERSIONS = new VERSIONS();
    public Map<String, DOMAIN_MOTD_NODE> DOMAINS = Map.of();
  }

  public static class COMMAND {
    public String USAGE = "<gold>FastMOTD Paper</gold> <gray>| Use /maintenance [on/off]";
  }

  public static class DOMAIN_MOTD_NODE {
    public List<String> DESCRIPTION = List.of("Default Paper MOTD");
    public List<String> FAVICON = List.of();
    public List<String> INFORMATION = List.of();
  }
}
