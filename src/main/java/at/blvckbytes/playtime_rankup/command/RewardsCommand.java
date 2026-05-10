package at.blvckbytes.playtime_rankup.command;

import at.blvckbytes.playtime_rankup.rewards_display.RewardsDisplayHandler;
import at.blvckbytes.playtime_rankup.store.UserDataStore;
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

  public RewardsCommand(
    UserDataStore userDataStore,
    RewardsDisplayHandler displayHandler,
    OfflinePlayerRegistry offlinePlayerRegistry
  ) {
    this.userDataStore = userDataStore;
    this.displayHandler = displayHandler;
    this.offlinePlayerRegistry = offlinePlayerRegistry;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage("§cThis command is only available to players");
      return true;
    }

    OfflinePlayer target;

    if (args.length == 0)
      target = player;

    else {
      target = offlinePlayerRegistry.getPlayerByName(args[1]);

      if (target == null) {
        sender.sendMessage("§cThe player " + args[0] + " hasn't played on this server yet!");
        return true;
      }
    }

    if (target != player && !player.hasPermission("playtimerankup.rewards.other")) {
      sender.sendMessage("§cYou cannot view the rewards of other players");
      return true;
    }

    displayHandler.show(player, userDataStore.access(target));
    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (sender instanceof Player player && player.hasPermission("playtimerankup.rewards.other") && args.length == 1) {
      var typedNameLower = args[0].toLowerCase();

      return offlinePlayerRegistry.streamKnownNames()
        .filter(it -> it.toLowerCase().startsWith(typedNameLower))
        .limit(15)
        .toList();
    }

    return List.of();
  }
}
