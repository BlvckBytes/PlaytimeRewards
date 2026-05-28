package at.blvckbytes.playtime_rewards.command.rewards;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.playtime_rewards.command.OfflinePlayerRegistry;
import at.blvckbytes.playtime_rewards.config.MainSection;
import at.blvckbytes.playtime_rewards.rewards_display.RewardsDisplayHandler;
import at.blvckbytes.playtime_rewards.store.UserDataStore;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RewardsCommand implements CommandExecutor, TabCompleter {

  private final UserDataStore userDataStore;
  private final RewardsDisplayHandler displayHandler;
  private final OfflinePlayerRegistry offlinePlayerRegistry;
  private final ConfigKeeper<MainSection> config;

  public RewardsCommand(
    UserDataStore userDataStore,
    RewardsDisplayHandler displayHandler,
    OfflinePlayerRegistry offlinePlayerRegistry,
    ConfigKeeper<MainSection> config
  ) {
    this.userDataStore = userDataStore;
    this.displayHandler = displayHandler;
    this.offlinePlayerRegistry = offlinePlayerRegistry;
    this.config = config;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      config.rootSection.commonMessages.onlyAvailableToPlayers.sendMessage(sender);
      return true;
    }

    OfflinePlayer target;

    if (args.length == 0)
      target = player;

    else {
      target = offlinePlayerRegistry.getPlayerByName(args[0]);

      if (target == null) {
        config.rootSection.commonMessages.hasNotPlayedBefore.sendMessage(
          sender,
          new InterpretationEnvironment()
            .withVariable("name", args[0])
        );

        return true;
      }
    }

    displayHandler.show(player, userDataStore.access(target));
    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (sender instanceof Player && args.length == 1) {
      var typedNameLower = args[0].toLowerCase();

      return offlinePlayerRegistry.streamKnownNames()
        .filter(it -> it.toLowerCase().startsWith(typedNameLower))
        .limit(15)
        .toList();
    }

    return List.of();
  }
}
