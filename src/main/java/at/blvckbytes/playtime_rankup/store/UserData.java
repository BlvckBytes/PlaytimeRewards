package at.blvckbytes.playtime_rankup.store;

import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.StringReader;
import java.util.EnumMap;
import java.util.UUID;

public class UserData {

  public final UUID playerId;

  private String lastKnownName;

  private final TimeStatistics globalStatistics;

  private final TimeStatisticsAndKey[] statisticsByCalendarBucketOrdinal;

  private boolean dirty;

  private final EnumMap<TopListType, EnumMap<TimeType, Integer>> topListIndexByTimeTypeByListType;

  private UserData(
    UUID playerId,
    String lastKnownName,
    TimeStatistics globalStatistics,
    TimeStatisticsAndKey[] statisticsByCalendarBucketOrdinal
  ) {
    this.playerId = playerId;

    this.lastKnownName = lastKnownName;

    if (lastKnownName.isBlank())
      throw new IllegalStateException("Property \"lastKnownName\" is blank");

    this.globalStatistics = globalStatistics;

    this.statisticsByCalendarBucketOrdinal = statisticsByCalendarBucketOrdinal;

    if (statisticsByCalendarBucketOrdinal.length != CalendarBucket.ALL_VALUES.size())
      throw new IllegalStateException("Array \"statisticsByCalendarBucketOrdinal\" does not hold as many values as there are bucket-types");

    this.topListIndexByTimeTypeByListType = new EnumMap<>(TopListType.class);
  }

  public void setTopListNumber(TopListType topListType, TimeType timeType, int index) {
    topListIndexByTimeTypeByListType
      .computeIfAbsent(topListType, _ -> new EnumMap<>(TimeType.class))
      .put(timeType, index);
  }

  public int getTopListNumber(TopListType topListType, TimeType timeType) {
    var typeBucket = topListIndexByTimeTypeByListType.get(topListType);

    if (typeBucket == null)
      return -1;

    return typeBucket.getOrDefault(timeType, -1);
  }

  public long getGlobalTimeTicks(TimeType timeType) {
    return globalStatistics.getTime(timeType);
  }

  public long getCalendarBucketTimeTicks(CalendarBucket calendarBucket, TimeType timeType) {
    return statisticsByCalendarBucketOrdinal[calendarBucket.ordinal()].getTime(timeType);
  }

  public String getLastKnownName() {
    return lastKnownName;
  }

  public void updateLastKnownName(String name) {
    if (name.equals(lastKnownName))
      return;

    this.lastKnownName = name;
    this.dirty = true;
  }

  public void updateCalendarBucketKeys(CalendarInfoProvider calendarInfoProvider) {
    for (var calendarBucket : CalendarBucket.ALL_VALUES) {
      var bucketStatistics = statisticsByCalendarBucketOrdinal[calendarBucket.ordinal()];
      dirty |= bucketStatistics.updateKeyAndResetIfApplicable(calendarInfoProvider.getCalendarKey(calendarBucket));
    }
  }

  public void incrementTime(TimeType timeType, int value, CalendarInfoProvider calendarInfoProvider) {
    globalStatistics.incrementTime(timeType, value);

    for (var calendarBucket : CalendarBucket.ALL_VALUES) {
      var bucketStatistics = statisticsByCalendarBucketOrdinal[calendarBucket.ordinal()];

      bucketStatistics.updateKeyAndResetIfApplicable(calendarInfoProvider.getCalendarKey(calendarBucket));
      bucketStatistics.incrementTime(timeType, value);
    }

    dirty = true;
  }

  public void decrementTime(TimeType timeType, int value, CalendarInfoProvider calendarInfoProvider) {
    globalStatistics.decrementTime(timeType, value);

    for (var calendarBucket : CalendarBucket.ALL_VALUES) {
      var bucketStatistics = statisticsByCalendarBucketOrdinal[calendarBucket.ordinal()];

      bucketStatistics.updateKeyAndResetIfApplicable(calendarInfoProvider.getCalendarKey(calendarBucket));
      bucketStatistics.decrementTime(timeType, value);
    }

    dirty = true;
  }

  public boolean isDirty() {
    return dirty;
  }

  public void clearDirty() {
    this.dirty = false;
  }

  public String serialize() {
    var temporaryConfig = new YamlConfiguration();

    temporaryConfig.set("playTimeTicks", globalStatistics.getTime(TimeType.PLAY_TIME));
    temporaryConfig.set("afkTimeTicks", globalStatistics.getTime(TimeType.AFK_TIME));
    temporaryConfig.set("lastKnownName", lastKnownName);

    for (var calendarBucket : CalendarBucket.ALL_VALUES) {
      var bucketSection = temporaryConfig.createSection("calendarBuckets." + calendarBucket.name());
      var bucketStatistics = statisticsByCalendarBucketOrdinal[calendarBucket.ordinal()];

      bucketSection.set("playTimeTicks", bucketStatistics.getTime(TimeType.PLAY_TIME));
      bucketSection.set("afkTimeTicks", bucketStatistics.getTime(TimeType.AFK_TIME));
      bucketSection.set("key", bucketStatistics.key);
    }

    return temporaryConfig.saveToString();
  }

  public InterpretationEnvironment makeEnvironment() {
    var environment = new InterpretationEnvironment();

    environment
      .withVariable("player_name", lastKnownName)
      .withVariable("play_time", getGlobalTimeTicks(TimeType.PLAY_TIME))
      .withVariable("afk_time", getGlobalTimeTicks(TimeType.AFK_TIME));

    for (var timeType : TimeType.ALL_VALUES) {
      for (var topListType : TopListType.ALL_VALUES) {
        var timeIdentifier = (timeType.name() + "_" + topListType.name()).toLowerCase();

        var calendarBucket = topListType.getCalendarBucket();

        if (calendarBucket != null)
          environment.withVariable(timeIdentifier, getCalendarBucketTimeTicks(calendarBucket, timeType));

        var topNumberIdentifier = timeIdentifier + "_top_place";

        environment.withVariable(topNumberIdentifier, getTopListNumber(topListType, timeType));
      }
    }

    return environment;
  }

  public static UserData makeInitial(UUID playerId, String lastKnownName, CalendarInfoProvider calendarInfoProvider) {
    var statisticsByCalendarBucketOrdinal = new TimeStatisticsAndKey[CalendarBucket.ALL_VALUES.size()];

    for (var calendarBucket : CalendarBucket.ALL_VALUES)
      statisticsByCalendarBucketOrdinal[calendarBucket.ordinal()] = new TimeStatisticsAndKey(0, 0, calendarInfoProvider.getCalendarKey(calendarBucket));

    return new UserData(playerId, lastKnownName, new TimeStatistics(0, 0), statisticsByCalendarBucketOrdinal);
  }

  public static UserData deserialize(UUID playerId, String value) {
    var temporaryConfig = YamlConfiguration.loadConfiguration(new StringReader(value));

    if (!(temporaryConfig.get("playTimeTicks") instanceof Number playTimeTicksNumber))
      throw new IllegalStateException("Missing property \"playTimeTicks\"");

    if (!(temporaryConfig.get("afkTimeTicks") instanceof Number afkTimeTicksNumber))
      throw new IllegalStateException("Missing property \"afkTimeTicks\"");

    if (!(temporaryConfig.get("lastKnownName") instanceof String lastKnownName))
      throw new IllegalStateException("Missing property \"lastKnownName\"");

    var statisticsByCalendarBucketOrdinal = new TimeStatisticsAndKey[CalendarBucket.ALL_VALUES.size()];

    for (var calendarBucket : CalendarBucket.ALL_VALUES) {
      var sectionPath = "calendarBuckets." + calendarBucket.name();
      var bucketSection = temporaryConfig.getConfigurationSection(sectionPath);

      if (bucketSection == null)
        throw new IllegalStateException("Missing section \"" + sectionPath + "\"");

      if (!(bucketSection.get("playTimeTicks") instanceof Number bucketPlayTimeTicksNumber))
        throw new IllegalStateException("Missing property \"" + sectionPath + ".playTimeTicks\"");

      if (!(bucketSection.get("afkTimeTicks") instanceof Number bucketAfkTimeTicksNumber))
        throw new IllegalStateException("Missing property \"" + sectionPath + ".afkTimeTicks\"");

      if (!(bucketSection.get("key") instanceof Number key))
        throw new IllegalStateException("Missing property \"" + sectionPath + ".key\"");

      statisticsByCalendarBucketOrdinal[calendarBucket.ordinal()] = new TimeStatisticsAndKey(
        bucketPlayTimeTicksNumber.longValue(),
        bucketAfkTimeTicksNumber.longValue(),
        key.intValue()
      );
    }

    return new UserData(
      playerId,
      lastKnownName,
      new TimeStatistics(playTimeTicksNumber.longValue(), afkTimeTicksNumber.longValue()),
      statisticsByCalendarBucketOrdinal
    );
  }
}
