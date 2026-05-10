package at.blvckbytes.playtime_rankup.config;

import at.blvckbytes.cm_mapper.mapper.MappingError;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.CSIgnore;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import at.blvckbytes.playtime_rankup.duration_syntax.DurationException;
import at.blvckbytes.playtime_rankup.duration_syntax.DurationSyntax;
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

    try {
      _requiredPlayTimeTicks = DurationSyntax.parseSyntaxIntoTicks(requiredPlayTime);
    } catch (DurationException e) {
      switch (e.error) {
        case UNSUPPORTED_UNIT -> throw new MappingError("Property \"requiredPlayTime\": Unsupported unit: " + e.errorToken);
        case MALFORMED_NUMBER -> throw new MappingError("Property \"requiredPlayTime\": Malformed number: " + e.errorToken);
        case NEGATIVE_NUMBER  -> throw new MappingError("Property \"requiredPlayTime\": Negative number: " + e.errorToken);
      }
    }

    if (_requiredPlayTimeTicks <= 0)
      throw new IllegalStateException("The total time of \"requiredPlayTime\" must be greater than zero");
  }
}
