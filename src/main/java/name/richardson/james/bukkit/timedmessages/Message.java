/*******************************************************************************
 * Copyright (c) 2011 James Richardson.
 * 
 * Message.java is part of TimedMessages.
 * 
 * TimedMessages is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * 
 * TimedMessages is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * TimedMessages. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package name.richardson.james.bukkit.timedmessages;

import java.util.LinkedList;
import java.util.List;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;

import name.richardson.james.bukkit.utilities.formatters.ColourFormatter;
import name.richardson.james.bukkit.utilities.formatters.TimeFormatter;
import name.richardson.james.bukkit.utilities.internals.Logger;

public abstract class Message implements Runnable {

  protected final Logger logger = new Logger(this.getClass());
  protected final List<String> messages;

  private final Long ticks;
  private final String permission;
  private final Server server;
  private final String worldName;

  public Message(final Server server, final Long milliseconds, final List<String> messages, final String permission, final String worldName) {
    final long seconds = milliseconds / 1000;
    this.ticks = seconds * 20;
    this.messages = messages;
    this.permission = permission;
    this.server = server;
    this.worldName = worldName;
    this.logger.debug(String.format("Creating new message broadcasting every %s (%d ticks)", TimeFormatter.millisToLongDHMS(milliseconds), this.ticks));
  }

  public List<String> getMessages() {
    return this.messages;
  }

  public String getPermission() {
    return this.permission;
  }

  public Long getTicks() {
    return this.ticks;
  }

  public void run() {
    String message = this.getNextMessage();
    message = ColourFormatter.replace("&", message);
    final String[] parts = message.split("/n");
    final List<Player> players = new LinkedList<Player>();
    World world = null;

    if (this.worldName != null) {
      world = this.server.getWorld(this.worldName);
    }

    for (final Player player : this.server.getOnlinePlayers()) {
      // ignore the player if they are not in the world required
      if ((world != null) && (player.getLocation().getWorld() != world)) {
        continue;
      }
      // ignore the player if they do not have the correct permission
      if ((this.permission != null) && (!player.hasPermission(this.permission))) {
        continue;
      }
      players.add(player);
    }

    if (players.isEmpty()) {
      return;
    }

    this.logger.debug("Selecting players for broadcast: " + players.toString());
    for (final String part : parts) {
      this.logger.debug(String.format("Broadcasting message: '%s'", part, players.size()));
      for (final Player player : players) {
        player.sendMessage(part);
      }
    }

  }

  protected abstract String getNextMessage();

}
