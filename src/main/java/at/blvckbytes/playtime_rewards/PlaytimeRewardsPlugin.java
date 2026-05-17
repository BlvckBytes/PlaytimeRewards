package at.blvckbytes.playtime_rewards;

import at.blvckbytes.cm_mapper.ConfigHandler;
import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.ReloadPriority;
import at.blvckbytes.cm_mapper.section.command.CommandUpdater;
import at.blvckbytes.playtime_rewards.command.*;
import at.blvckbytes.playtime_rewards.command.main.MainCommandSection;
import at.blvckbytes.playtime_rewards.command.playtime.PlaytimeCommand;
import at.blvckbytes.playtime_rewards.command.playtime.PlaytimeCommandSection;
import at.blvckbytes.playtime_rewards.command.rewards.RewardsCommand;
import at.blvckbytes.playtime_rewards.command.rewards.RewardsCommandSection;
import at.blvckbytes.playtime_rewards.command.top_times.AfkTopCommand;
import at.blvckbytes.playtime_rewards.command.top_times.AfkTopCommandSection;
import at.blvckbytes.playtime_rewards.command.top_times.PlayTopCommand;
import at.blvckbytes.playtime_rewards.command.main.MainCommand;
import at.blvckbytes.playtime_rewards.command.top_times.PlayTopCommandSection;
import at.blvckbytes.playtime_rewards.config.MainSection;
import at.blvckbytes.playtime_rewards.placeholder.PlaytimePlaceholderExpansion;
import at.blvckbytes.playtime_rewards.rewards.RewardsManager;
import at.blvckbytes.playtime_rewards.rewards_display.RewardsDisplayHandler;
import at.blvckbytes.playtime_rewards.store.CalendarInfoProvider;
import at.blvckbytes.playtime_rewards.store.UserDataStore;
import net.ess3.api.IEssentials;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.logging.Level;

public class PlaytimeRewardsPlugin extends JavaPlugin {

  private @Nullable UserDataStore userDataStore;
  private @Nullable RewardsDisplayHandler rewardsDisplayHandler;

  @Override
  public void onEnable() {
    var logger = getLogger();

    try {
      var configHandler = new ConfigHandler(this, "config");
      var config = new ConfigKeeper<>(configHandler, "config.yml", MainSection.class);

      var luckPermsProvider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);

      if (luckPermsProvider == null)
        throw new IllegalStateException("Expected the luckperms-provider to be present");

      var luckPerms = luckPermsProvider.getProvider();

      var essentials = (IEssentials) Bukkit.getPluginManager().getPlugin("Essentials");

      if (essentials == null)
        throw new IllegalStateException("Expected Essentials to be loaded");

      var calendarInfoProvider = new CalendarInfoProvider(config, this);

      userDataStore = new UserDataStore(calendarInfoProvider, config, this);

      if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI"))
        new PlaytimePlaceholderExpansion(this, userDataStore, config).register();

      var rewardsManager = new RewardsManager(userDataStore, luckPerms, essentials, config, this);
      getServer().getPluginManager().registerEvents(rewardsManager, this);

      var offlinePlayerRegistry = new OfflinePlayerRegistry(userDataStore);
      getServer().getPluginManager().registerEvents(offlinePlayerRegistry, this);

      rewardsDisplayHandler = new RewardsDisplayHandler(config, this);
      getServer().getPluginManager().registerEvents(rewardsDisplayHandler, this);

      var playtimeCommand = Objects.requireNonNull(getCommand(PlaytimeCommandSection.INITIAL_NAME));
      setExecutorAndCompleter(playtimeCommand, new PlaytimeCommand(userDataStore, offlinePlayerRegistry, config));

      var playTopCommand = Objects.requireNonNull(getCommand(PlayTopCommandSection.INITIAL_NAME));
      setExecutorAndCompleter(playTopCommand, new PlayTopCommand(userDataStore, config));

      var afkTopCommand = Objects.requireNonNull(getCommand(AfkTopCommandSection.INITIAL_NAME));
      setExecutorAndCompleter(afkTopCommand, new AfkTopCommand(userDataStore, config));

      var rewardsCommand = Objects.requireNonNull(getCommand(RewardsCommandSection.INITIAL_NAME));
      setExecutorAndCompleter(rewardsCommand, new RewardsCommand(userDataStore, rewardsDisplayHandler, offlinePlayerRegistry, config));

      var mainCommand = Objects.requireNonNull(getCommand(MainCommandSection.INITIAL_NAME));
      setExecutorAndCompleter(mainCommand, new MainCommand(offlinePlayerRegistry, userDataStore, calendarInfoProvider, essentials, config, this));

      var commandUpdater = new CommandUpdater(this);

      Runnable updateCommands = () -> {
        config.rootSection.commands.playtime.apply(playtimeCommand, commandUpdater);
        config.rootSection.commands.playTop.apply(playTopCommand, commandUpdater);
        config.rootSection.commands.afkTop.apply(afkTopCommand, commandUpdater);
        config.rootSection.commands.rewards.apply(rewardsCommand, commandUpdater);
        config.rootSection.commands.main.apply(mainCommand, commandUpdater);
      };

      updateCommands.run();
      commandUpdater.trySyncCommands();

      config.registerReloadListener(updateCommands);
      config.registerReloadListener(commandUpdater::trySyncCommands, ReloadPriority.LOWEST);
    } catch (Throwable e) {
      logger.log(Level.SEVERE, "An error occurred while trying to enable the plugin; disabling!", e);
      Bukkit.getPluginManager().disablePlugin(this);
    }
  }

  @Override
  public void onDisable() {
    if (userDataStore != null) {
      catchAll(userDataStore::onDisable);
      userDataStore = null;
    }

    if (rewardsDisplayHandler != null) {
      catchAll(rewardsDisplayHandler::onDisable);
      rewardsDisplayHandler = null;
    }
  }

  private void catchAll(Runnable runnable) {
    try {
      runnable.run();
    } catch (Throwable e) {
      getLogger().log(Level.SEVERE, "An internal error occurred", e);
    }
  }

  private void setExecutorAndCompleter(PluginCommand command, CommandExecutor executor) {
    command.setExecutor(executor);

    if (executor instanceof TabCompleter tabCompleter)
      command.setTabCompleter(tabCompleter);
  }
}
