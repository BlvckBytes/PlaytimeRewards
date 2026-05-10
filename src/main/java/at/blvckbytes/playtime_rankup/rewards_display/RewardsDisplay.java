package at.blvckbytes.playtime_rankup.rewards_display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.playtime_rankup.config.MainSection;
import at.blvckbytes.playtime_rankup.store.TimeType;
import at.blvckbytes.playtime_rankup.store.UserData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

public class RewardsDisplay extends Display<UserData> {

  public RewardsDisplay(
    Player player,
    UserData displayData,
    ConfigKeeper<MainSection> config,
    Plugin plugin
  ) {
    super(player, displayData, config, plugin);

    show();
  }

  @Override
  protected void renderItems() {
    var environment = makeEnvironment();

    config.rootSection.rewardsDisplay.items.filler.renderInto(inventory, environment);
    config.rootSection.rewardsDisplay.items.statistics.renderInto(inventory, environment);

    var playTime = displayData.getGlobalTimeTicks(TimeType.PLAY_TIME);

    for (var rank : config.rootSection.rankList) {
      var remainingTime = rank._requiredPlayTimeTicks - playTime;

      environment.withVariable("required_time", rank._requiredPlayTimeTicks);
      environment.withVariable("remaining_time", remainingTime);

      if (remainingTime < 0) {
        inventory.setItem(rank.icon.slot, rank.icon.claimed.build(environment));
        continue;
      }

      inventory.setItem(rank.icon.slot, rank.icon.pending.build(environment));
    }
  }

  @Override
  protected Inventory makeInventory() {
    return config.rootSection.rewardsDisplay.createInventory(makeEnvironment());
  }

  @Override
  public void onConfigReload() {
    show();
  }

  private InterpretationEnvironment makeEnvironment() {
    return displayData.makeEnvironment();
  }
}
