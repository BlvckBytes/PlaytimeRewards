package at.blvckbytes.playtime_rankup.rewards_display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.playtime_rankup.config.MainSection;
import at.blvckbytes.playtime_rankup.store.UserData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.plugin.Plugin;

public class RewardsDisplayHandler extends DisplayHandler<RewardsDisplay, UserData> {

  private long relativeTime;

  public RewardsDisplayHandler(ConfigKeeper<MainSection> config, Plugin plugin) {
    super(config, plugin);

    var redrawPeriod = config.rootSection.incrementIntervalTicks / 2;

    Bukkit.getScheduler().runTaskTimer(plugin, () -> {
      ++relativeTime;

      if (redrawPeriod == 0 || relativeTime % redrawPeriod == 0)
        forEachDisplay(RewardsDisplay::renderItems);
    }, 0, 0);
  }

  @Override
  public RewardsDisplay instantiateDisplay(Player player, UserData displayData) {
    return new RewardsDisplay(player, displayData, config, plugin);
  }

  @Override
  protected void handleClick(Player player, RewardsDisplay display, ClickType clickType, int slot) {}
}
