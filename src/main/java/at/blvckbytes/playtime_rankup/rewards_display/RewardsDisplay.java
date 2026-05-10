package at.blvckbytes.playtime_rankup.rewards_display;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.playtime_rankup.config.MainSection;
import at.blvckbytes.playtime_rankup.store.TimeType;
import at.blvckbytes.playtime_rankup.store.TopListType;
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

      environment.withVariable("remaining_time", remainingTime);

      if (remainingTime < 0) {
        rank.icon.claimed.renderInto(inventory, environment);
        continue;
      }

      rank.icon.pending.renderInto(inventory, environment);
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
    var environment = new InterpretationEnvironment();

    environment
      .withVariable("player_name", displayData.getLastKnownName())
      .withVariable("play_time", displayData.getGlobalTimeTicks(TimeType.PLAY_TIME))
      .withVariable("afk_time", displayData.getGlobalTimeTicks(TimeType.AFK_TIME));

    for (var timeType : TimeType.ALL_VALUES) {
      for (var topListType : TopListType.ALL_VALUES) {
        var timeIdentifier = (timeType.name() + "_" + topListType.name()).toLowerCase();

        var calendarBucket = topListType.getCalendarBucket();

        if (calendarBucket != null)
          environment.withVariable(timeIdentifier, displayData.getCalendarBucketTimeTicks(calendarBucket, timeType));

        var topNumberIdentifier = timeIdentifier + "_top_place";

        environment.withVariable(topNumberIdentifier, displayData.getTopListNumber(topListType, timeType));
      }
    }

    return environment;
  }
}
