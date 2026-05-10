package at.blvckbytes.playtime_rankup.config;

import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import net.luckperms.api.model.group.Group;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;

public class RankSection extends ConfigSection {

  public String requiredPlayTime = "";
  public @CSIgnore long _requiredPlayTimeTicks;

  public @CSAlways NotificationSection notification;
  public @CSAlways IconSection icon;

  public @CSIgnore @Nullable Group group;

  public RankSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  @Override
  public void afterParsing(List<Field> fields) throws Exception {
    super.afterParsing(fields);

    for (var timePart : requiredPlayTime.split(" ")) {
      var length = timePart.length();

      if (length == 0)
        continue;

      var unitChar = Character.toLowerCase(timePart.charAt(length - 1));

      var multiplier = switch (unitChar) {
        case 'h' -> 20 * 60 * 60;
        case 'd' -> 20 * 60 * 60 * 24;
        default -> throw new MappingError("Unsupported unit: " + unitChar);
      };

      var valueString = timePart.substring(0, length - 1);

      long value;

      try {
        value = Long.parseLong(valueString);
      } catch (Throwable e) {
        throw new MappingError("Malformed time-value: " + valueString);
      }

      if (value <= 0)
        throw new MappingError("A time-value cannot be less than or equal to zero");

      _requiredPlayTimeTicks += value * multiplier;
    }

    if (_requiredPlayTimeTicks <= 0)
      throw new IllegalStateException("The total time of \"requiredPlayTime\" must be greater than zero");
  }
}
