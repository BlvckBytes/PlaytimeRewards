package at.blvckbytes.playtime_rewards;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.playtime_rewards.config.MainSection;
import at.blvckbytes.playtime_rewards.store.*;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlaytimePlaceholderExpansion extends PlaceholderExpansion {

  private final Plugin plugin;
  private final UserDataStore userDataStore;
  private final ConfigKeeper<MainSection> config;

  public PlaytimePlaceholderExpansion(
    Plugin plugin,
    UserDataStore userDataStore,
    ConfigKeeper<MainSection> config
  ) {
    this.plugin = plugin;
    this.userDataStore = userDataStore;
    this.config = config;
  }

  @Override
  public @NotNull String getIdentifier() {
    return "playtime";
  }

  @Override
  public @NotNull String getAuthor() {
    return String.join(", ", plugin.getPluginMeta().getAuthors());
  }

  @Override
  public @NotNull String getVersion() {
    return plugin.getPluginMeta().getVersion();
  }

  @Override
  public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
    var args = params.split("_");

    if (args.length < 2)
      return null;

    var userData = userDataStore.access(player);

    var timeType = switch (args[0]) {
      case "play" -> TimeType.PLAY_TIME;
      case "afk" -> TimeType.AFK_TIME;
      case null, default -> null;
    };

    if (timeType == null)
      return null;

    if (args[1].equals("total")) {
      if (args.length == 2)
        return formatTime(userData.getTotalTimeTicks(timeType));

      return tryAccessTopStatistic(userData, args, TopListType.TOTAL, timeType);
    }

    var bucketType = switch (args[1]) {
      case "day" -> CalendarBucket.DAY;
      case "week" -> CalendarBucket.WEEK;
      case "month" -> CalendarBucket.MONTH;
      case "year" -> CalendarBucket.YEAR;
      default -> null;
    };

    if (bucketType == null)
      return null;

    if (args.length == 2)
      return formatTime(userData.getCalendarBucketTimeTicks(bucketType, timeType));

    return tryAccessTopStatistic(userData, args, bucketType.getTopListType(), timeType);
  }

  private @Nullable String tryAccessTopStatistic(UserData userData, String[] args, TopListType topListType, TimeType timeType) {
    if (!(args.length == 4 || args.length == 5 || args.length == 6))
      return null;

    var direction = switch (args[2]) {
      case "desc" -> TopListDirection.DESCENDING;
      case "asc" -> TopListDirection.ASCENDING;
      default -> null;
    };

    if (direction == null)
      return null;

    if (args.length == 4) {
      if (args[3].equals("place"))
        return String.valueOf(userData.getTopListNumber(topListType, timeType, direction));

      return null;
    }

    if (!args[3].equals("top"))
      return null;

    int topPlace;

    try {
      topPlace = Integer.parseInt(args[4]);
    } catch (Throwable e) {
      return null;
    }

    var topList = userDataStore.getTopList(topListType, timeType, direction);
    var targetUser = topPlace <= 0 || topPlace > topList.size() ? null : topList.get(topPlace - 1);

    if (args.length == 5) {
      if (targetUser == null)
        return formatTime(0);

      var calendarBucket = topListType.getCalendarBucket();

      if (calendarBucket == null)
        return formatTime(targetUser.getTotalTimeTicks(timeType));

      return formatTime(targetUser.getCalendarBucketTimeTicks(calendarBucket, timeType));
    }

    if (!args[5].equals("name"))
      return null;

    if (targetUser == null)
      return "undefined";

    return targetUser.getLastKnownName();
  }

  private String formatTime(long input) {
    return config.rootSection.placeholderTime.asPlainString(
      new InterpretationEnvironment()
        .withVariable("time", input)
    );
  }
}
