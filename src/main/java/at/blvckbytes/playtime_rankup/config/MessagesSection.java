package at.blvckbytes.playtime_rankup.config;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.constructor.SlotType;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

public class MessagesSection extends ConfigSection {

  public @Nullable ComponentMarkup title;
  public @Nullable ComponentMarkup subtitle;
  public @Nullable ComponentMarkup chat;

  public MessagesSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }

  public void sendTo(Iterable<? extends Player> receivers, InterpretationEnvironment environment) {
    if (chat != null) {
      var components = chat.interpret(SlotType.CHAT, environment);

      for (var receiver : receivers)
        components.forEach(receiver::sendMessage);
    }

    if (title != null || subtitle != null) {
      var titleComponent = title == null ? null : title.interpret(SlotType.SINGLE_LINE_CHAT, environment).getFirst();
      var subtitleComponent = subtitle == null ? null : subtitle.interpret(SlotType.SINGLE_LINE_CHAT, environment).getFirst();

      for (var receiver : receivers) {
        if (titleComponent != null)
          receiver.sendTitlePart(TitlePart.TITLE, titleComponent);

        if (subtitleComponent != null)
          receiver.sendTitlePart(TitlePart.SUBTITLE, subtitleComponent);

        receiver.sendTitlePart(
          TitlePart.TIMES,
          Title.Times.times(
            Duration.ofMillis(500),
            Duration.ofMillis(2500),
            Duration.ofMillis(500)
          )
        );
      }
    }
  }
}
