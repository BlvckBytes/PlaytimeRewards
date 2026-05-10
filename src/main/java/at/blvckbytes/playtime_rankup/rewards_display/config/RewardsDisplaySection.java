package at.blvckbytes.playtime_rankup.rewards_display.config;

import at.blvckbytes.cm_mapper.section.gui.GuiSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class RewardsDisplaySection extends GuiSection<RewardsDisplayItemsSection> {

  public RewardsDisplaySection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(RewardsDisplayItemsSection.class, baseEnvironment, interpreterLogger);
  }
}
