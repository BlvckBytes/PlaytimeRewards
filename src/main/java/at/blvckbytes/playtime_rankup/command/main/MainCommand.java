package at.blvckbytes.playtime_rankup.command.main;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.playtime_rankup.config.MainSection;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.logging.Level;

public class MainCommand implements CommandExecutor, TabCompleter {

  private final ConfigKeeper<MainSection> config;
  private final Plugin plugin;

  public MainCommand(ConfigKeeper<MainSection> config, Plugin plugin) {
    this.config = config;
    this.plugin = plugin;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!sender.hasPermission("playtimerankup.admin-command"))
      return true;

    if (args.length == 0) {
      sender.sendMessage("§cUsage: /" + label + " <Action>");
      return true;
    }

    var normalizedAction = CommandAction.matcher.matchFirst(args[0]);

    if (normalizedAction == null) {
      sender.sendMessage("§cUsage: /" + label + " <Action>");
      return true;
    }

    if (normalizedAction.constant == CommandAction.RELOAD) {
      try {
        config.reload();
        sender.sendMessage("§aReloaded successfully");
      } catch (Exception e) {
        sender.sendMessage("§cAn error occurred while trying to reload");
        plugin.getLogger().log(Level.SEVERE, "An error occurred while trying to reload the config", e);
      }
      return true;
    }

    throw new IllegalStateException("Unaccounted-for action: " + normalizedAction.constant);
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!sender.hasPermission("playtimerankup.admin-command"))
      return List.of();

    if (args.length == 1)
      return CommandAction.matcher.createCompletions(args[0]);

    return List.of();
  }
}
