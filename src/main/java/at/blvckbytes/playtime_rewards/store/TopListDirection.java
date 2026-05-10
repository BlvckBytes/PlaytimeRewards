package at.blvckbytes.playtime_rewards.store;

import me.blvckbytes.syllables_matcher.EnumMatcher;
import me.blvckbytes.syllables_matcher.MatchableEnum;

import java.util.Arrays;
import java.util.List;

public enum TopListDirection implements MatchableEnum {

  DESCENDING(-1),
  ASCENDING(1),
  ;

  public static final List<TopListDirection> ALL_VALUES = Arrays.asList(values());
  public static final EnumMatcher<TopListDirection> matcher = new EnumMatcher<>(values());

  private final int numericMultiplier;

  TopListDirection(int numericMultiplier) {
    this.numericMultiplier = numericMultiplier;
  }

  public void sort(List<UserData> list, TopListType topListType, TimeType timeType) {
    list.sort((a, b) -> {

      // Let's round down to the previous second, as we do not get more specific than that whenever
      // displaying statistics; otherwise, sorting feels off, even if technically more correct.
      var secondsA = topListType.accessStatistic(a, timeType) / 20;
      var secondsB = topListType.accessStatistic(b, timeType) / 20;

      int result;

      if ((result = numericMultiplier * Long.compare(secondsA, secondsB)) != 0)
        return result;

      return a.getLastKnownName().compareTo(b.getLastKnownName());
    });
  }
}
