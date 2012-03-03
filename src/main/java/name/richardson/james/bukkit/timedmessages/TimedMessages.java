/*******************************************************************************
 * Copyright (c) 2011 James Richardson.
 * 
 * TimedMessages.java is part of TimedMessages.
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

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;

import name.richardson.james.bukkit.timedmessages.management.ReloadCommand;
import name.richardson.james.bukkit.timedmessages.management.StartCommand;
import name.richardson.james.bukkit.timedmessages.management.StatusCommand;
import name.richardson.james.bukkit.timedmessages.management.StopCommand;
import name.richardson.james.bukkit.timedmessages.random.RandomMessage;
import name.richardson.james.bukkit.timedmessages.rotation.RotatingMessage;
import name.richardson.james.bukkit.utilities.command.CommandManager;
import name.richardson.james.bukkit.utilities.formatters.TimeFormatter;
import name.richardson.james.bukkit.utilities.internals.Logger;
import name.richardson.james.bukkit.utilities.plugin.SimplePlugin;

public class TimedMessages extends SimplePlugin {

  public static final long START_DELAY = 30;

  private final Set<Message> timers = new HashSet<Message>();

  private List<ConfigurationSection> messages;
  private TimedMessagesConfiguration configuration;
  private boolean timersStarted = false;

  public String getFormattedTimerStartMessage(final long delay) {
    final Object[] arguments = { this.getTimerCount(), delay };
    final double[] limits = { 0, 1, 2 };
    final String[] formats = { this.getMessage("no-timers"), this.getMessage("one-timer"), this.getMessage("many-timers") };
    return this.getChoiceFormattedMessage("timers-started", arguments, formats, limits);
  }

  public int getTimerCount() {
    return this.timers.size();
  }

  public boolean isTimersStarted() {
    return this.timersStarted;
  }

  public void loadMessagesConfiguration() throws IOException {
    final MessagesConfiguration configuration = new MessagesConfiguration(this);
    this.messages = configuration.getConfigurationSections();
  }

  @Override
  public void onDisable() {
    this.stopTimers();
    this.logger.info(this.getSimpleFormattedMessage("plugin-disabled", this.getDescription().getName()));
  }

  @Override
  public void onEnable() {
    this.logger.setPrefix("[TimedMessages] ");

    try {
      this.loadConfiguration();
      this.setResourceBundle();
      this.loadMessagesConfiguration();
      this.startTimers(START_DELAY);
      this.setRootPermission();
      this.registerCommands();
    } catch (final IOException exception) {
      this.logger.severe(this.getMessage("unable-to-read-configuration"));
      this.setEnabled(false);
    } finally {
      if (!this.isEnabled()) {
        this.logger.severe(this.getMessage("panic"));
        return;
      }
    }

    this.logger.info(this.getSimpleFormattedMessage("plugin-enabled", this.getDescription().getFullName()));
  }

  public void startTimers(long startDelay) {
    if (this.timersStarted) {
      this.stopTimers();
    }
    this.timersStarted = true;
    final long startDelayInSeconds = startDelay;
    startDelay = startDelay * 20;
    for (final ConfigurationSection section : this.messages) {
      final Long milliseconds = TimeFormatter.parseTime(section.getString("delay", "5m"));
      final List<String> messages = section.getStringList("messages");
      final String permission = section.getString("permission");
      final String mode = section.getString("mode", "rotation");
      final String worldName = section.getString("world");
      Message task;
      if (mode.equalsIgnoreCase("rotation")) {
        task = new RotatingMessage(this.getServer(), milliseconds, messages, permission, worldName);
      } else {
        task = new RandomMessage(this.getServer(), milliseconds, messages, permission, worldName);
      }
      this.getServer().getScheduler().scheduleSyncRepeatingTask(this, task, startDelay, task.getTicks());
      this.timers.add(task);
    }
    this.logger.info(this.getFormattedTimerStartMessage(startDelayInSeconds));
  }

  public void stopTimers() {
    this.timersStarted = false;
    this.timers.clear();
    this.getServer().getScheduler().cancelTasks(this);
  }

  private void loadConfiguration() throws IOException {
    this.configuration = new TimedMessagesConfiguration(this);
    if (this.configuration.getDebugging()) {
      Logger.setDebugging(this, true);
    }
  }

  private void registerCommands() {
    final CommandManager commandManager = new CommandManager(this);
    this.getCommand("tm").setExecutor(commandManager);
    commandManager.addCommand(new ReloadCommand(this));
    commandManager.addCommand(new StartCommand(this));
    commandManager.addCommand(new StatusCommand(this));
    commandManager.addCommand(new StopCommand(this));
  }

}
