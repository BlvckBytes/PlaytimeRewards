package at.blvckbytes.playtime_rewards;

import at.blvckbytes.playtime_rewards.store.*;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlaytimePlaceholderExpansion extends PlaceholderExpansion {

  private final Plugin plugin;
  private final UserDataStore userDataStore;

  public PlaytimePlaceholderExpansion(
    Plugin plugin,
    UserDataStore userDataStore
  ) {
    this.plugin = plugin;
    this.userDataStore = userDataStore;
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
        return String.valueOf(userData.getTotalTimeTicks(timeType));

      return tryAccessTopTime(args, TopListType.TOTAL, timeType);
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
      return String.valueOf(userData.getCalendarBucketTimeTicks(bucketType, timeType));

    return tryAccessTopTime(args, bucketType.getTopListType(), timeType);
  }

  private @Nullable String tryAccessTopTime(String[] args, TopListType topListType, TimeType timeType) {
    if (!(args.length == 5 || args.length == 6))
      return null;

    var direction = switch (args[2]) {
      case "desc" -> TopListDirection.DESCENDING;
      case "asc" -> TopListDirection.ASCENDING;
      default -> null;
    };

    if (direction == null)
      return null;

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
        return "0";

      var calendarBucket = topListType.getCalendarBucket();

      if (calendarBucket == null)
        return String.valueOf(targetUser.getTotalTimeTicks(timeType));

      return String.valueOf(targetUser.getCalendarBucketTimeTicks(calendarBucket, timeType));
    }

    if (!args[5].equals("name"))
      return null;

    if (targetUser == null)
      return "undefined";

    return targetUser.getLastKnownName();
  }
}
