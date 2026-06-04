package at.blvckbytes.playtime_rewards;

import at.blvckbytes.playtime_rewards.store.CalendarBucket;
import at.blvckbytes.playtime_rewards.store.TimeType;
import at.blvckbytes.playtime_rewards.store.TopListDirection;
import at.blvckbytes.playtime_rewards.store.TopListType;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public interface PlaytimeRewardsAPI {

  long getTotalTimeTicks(OfflinePlayer player, TimeType timeType);

  long getCalendarBucketTimeTicks(OfflinePlayer player, CalendarBucket calendarBucket, TimeType timeType);

  int getTopListNumber(OfflinePlayer player, TopListType topListType, TopListDirection topListDirection, TimeType timeType);

  long getRemainingTimeUntilNextRank(Player player);

}
