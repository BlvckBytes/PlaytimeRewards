package at.blvckbytes.playtime_rankup.duration_syntax;

public class DurationSyntax {

   private static final long TICKS_IN_SECOND = 20;
   private static final long TICKS_IN_MINUTE = TICKS_IN_SECOND * 60;
   private static final long TICKS_IN_HOUR   = TICKS_IN_MINUTE * 60;
   private static final long TICKS_IN_DAY    = TICKS_IN_HOUR * 24;
   private static final long TICKS_IN_WEEK   = TICKS_IN_DAY * 7;

  public static long parseSyntaxIntoTicks(String input) throws DurationException {
    var totalValue = 0L;

    for (var timePart : input.split(" ")) {
      var length = timePart.length();

      if (length == 0)
        continue;

      var unitChar = Character.toLowerCase(timePart.charAt(length - 1));

      var multiplier = switch (unitChar) {
        case 's' -> TICKS_IN_SECOND;
        case 'm' -> TICKS_IN_MINUTE;
        case 'h' -> TICKS_IN_HOUR;
        case 'd' -> TICKS_IN_DAY;
        case 'w' -> TICKS_IN_WEEK;
        default -> throw new DurationException(input, DurationError.UNSUPPORTED_UNIT, unitChar);
      };

      var valueString = timePart.substring(0, length - 1);

      long currentValue;

      try {
        currentValue = Long.parseLong(valueString);
      } catch (Throwable e) {
        throw new DurationException(input, DurationError.MALFORMED_NUMBER, valueString);
      }

      if (currentValue <= 0)
        throw new DurationException(input, DurationError.NEGATIVE_NUMBER, valueString);

      totalValue += currentValue * multiplier;
    }

    return totalValue;
  }
}
