package at.blvckbytes.playtime_rewards.command.main;

import me.blvckbytes.syllables_matcher.EnumMatcher;
import me.blvckbytes.syllables_matcher.MatchableEnum;

public enum CommandAction implements MatchableEnum {
  RELOAD,
  ADD_PLAY_TIME,
  SUBTRACT_PLAY_TIME,
  ADD_AFK_TIME,
  SUBTRACT_AFK_TIME,
  MIGRATE_REWARDS_LITE,
  EXPORT_USERDATA
  ;

  public static final EnumMatcher<CommandAction> matcher = new EnumMatcher<>(values());
}
