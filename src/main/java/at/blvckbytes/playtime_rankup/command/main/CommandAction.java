package at.blvckbytes.playtime_rankup.command.main;

import me.blvckbytes.syllables_matcher.EnumMatcher;
import me.blvckbytes.syllables_matcher.MatchableEnum;

public enum CommandAction implements MatchableEnum {
  RELOAD,
  ;

  public static final EnumMatcher<CommandAction> matcher = new EnumMatcher<>(values());
}
