package at.blvckbytes.playtime_rankup;

import at.blvckbytes.cm_mapper.ConfigHandler;
import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.playtime_rankup.command.*;
import at.blvckbytes.playtime_rankup.command.main.MainCommand;
import at.blvckbytes.playtime_rankup.config.MainSection;
import at.blvckbytes.playtime_rankup.rankup.RankupManager;
import at.blvckbytes.playtime_rankup.rewards_display.RewardsDisplayHandler;
import at.blvckbytes.playtime_rankup.store.CalendarInfoProvider;
import at.blvckbytes.playtime_rankup.store.UserDataStore;
import net.ess3.api.IEssentials;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.logging.Level;

public class PlaytimeRankupPlugin extends JavaPlugin {

  // TODO: Put all messages into the config

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

      userDataStore = new UserDataStore(new CalendarInfoProvider(config, this), config, this);

      if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI"))
        new PlaytimePlaceholderExpansion(this, userDataStore).register();

      var rankupManager = new RankupManager(userDataStore, luckPerms, essentials, config, this);
      getServer().getPluginManager().registerEvents(rankupManager, this);

      var offlinePlayerRegistry = new OfflinePlayerRegistry();
      getServer().getPluginManager().registerEvents(offlinePlayerRegistry, this);

      rewardsDisplayHandler = new RewardsDisplayHandler(config, this);
      getServer().getPluginManager().registerEvents(rewardsDisplayHandler, this);

      Objects.requireNonNull(getCommand("playtime")).setExecutor(new PlaytimeCommand(userDataStore, offlinePlayerRegistry));
      Objects.requireNonNull(getCommand("playtop")).setExecutor(new PlayTopCommand(userDataStore));
      Objects.requireNonNull(getCommand("afktop")).setExecutor(new AfkTopCommand(userDataStore));
      Objects.requireNonNull(getCommand("rewards")).setExecutor(new RewardsCommand(userDataStore, rewardsDisplayHandler, offlinePlayerRegistry));
      Objects.requireNonNull(getCommand("playtimerankup")).setExecutor(new MainCommand(config, this));
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
}
