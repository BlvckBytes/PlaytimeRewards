package at.blvckbytes.playtime_rewards.command.main;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.section.command.CommandSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class MainCommandSection extends CommandSection {

  public static final String INITIAL_NAME = "playtimerewards";

  public ComponentMarkup commandUsage;
  public ComponentMarkup reloadedSuccessfully;
  public ComponentMarkup errorWhileReloading;
  public ComponentMarkup timeModificationUsage;
  public ComponentMarkup unsupportedDurationUnit;
  public ComponentMarkup malformedDurationNumber;
  public ComponentMarkup negativeDurationNumber;
  public ComponentMarkup zeroDuration;
  public ComponentMarkup timeModifiedSuccessfully;
  public ComponentMarkup exportUsage;
  public ComponentMarkup exportTimeFormat;
  public ComponentMarkup exportWriteError;
  public ComponentMarkup exportSuccess;

  public MainCommandSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(INITIAL_NAME, baseEnvironment, interpreterLogger);
  }
}
