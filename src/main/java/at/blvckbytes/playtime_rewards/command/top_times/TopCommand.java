package at.blvckbytes.playtime_rewards.command.top_times;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.playtime_rewards.config.MainSection;
import at.blvckbytes.playtime_rewards.store.TimeType;
import at.blvckbytes.playtime_rewards.store.TopListDirection;
import at.blvckbytes.playtime_rewards.store.TopListType;
import at.blvckbytes.playtime_rewards.store.UserDataStore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public abstract class TopCommand implements CommandExecutor, TabCompleter {

  private final UserDataStore userDataStore;
  protected final ConfigKeeper<MainSection> config;
  private final TimeType timeType;

  public TopCommand(
    UserDataStore userDataStore,
    ConfigKeeper<MainSection> config,
    TimeType timeType
  ) {
    this.userDataStore = userDataStore;
    this.config = config;
    this.timeType = timeType;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    var page = 1;

    if (args.length > 0) {
      try {
        page = Integer.parseInt(args[0]);

        if (page < 0)
          throw new IllegalStateException();
      } catch (Throwable e) {
        config.rootSection.commonMessages.topCommandInvalidPage.sendMessage(
          sender,
          new InterpretationEnvironment()
            .withVariable("input", args[0])
        );

        return true;
      }
    }

    var normalizedType = TopListType.matcher.getNormalizedConstant(TopListType.TOTAL);

    if (args.length > 1) {
      normalizedType = TopListType.matcher.matchFirst(args[1]);

      if (normalizedType == null) {
        printUsage(sender, label);
        return true;
      }
    }

    var normalizedDirection = TopListDirection.matcher.getNormalizedConstant(TopListDirection.DESCENDING);

    if (args.length > 2) {
      normalizedDirection = TopListDirection.matcher.matchFirst(args[2]);

      if (normalizedDirection == null) {
        printUsage(sender, label);
        return true;
      }
    }

    if (args.length > 3) {
      printUsage(sender, label);
      return true;
    }

    var topList = userDataStore.getTopList(normalizedType.constant, timeType, normalizedDirection.constant);

    if (topList.isEmpty()) {
      config.rootSection.commonMessages.topCommandEmptyTopList.sendMessage(sender);
      return true;
    }

    var pageSize = config.rootSection.topListCommandsPageSize;
    var numberOfPages = (topList.size() + pageSize - 1) / pageSize;

    if (page > numberOfPages) {
      config.rootSection.commonMessages.topCommandExceededPages.sendMessage(
        sender,
        new InterpretationEnvironment()
          .withVariable("page", page)
          .withVariable("number_of_pages", numberOfPages)
      );

      return true;
    }

    var pageEntries = new ArrayList<TopListPageEntry>();
    var firstIndex = (page - 1) * pageSize;

    for (var index = firstIndex; index < firstIndex + pageSize; ++index) {
      if (index >= topList.size())
        break;

      var userData = topList.get(index);

      pageEntries.add(new TopListPageEntry(
        index + 1,
        userData.getLastKnownName(),
        normalizedType.constant.accessStatistic(userData, timeType))
      );
    }

    config.rootSection.commonMessages.topScreen.sendMessage(
      sender,
      new InterpretationEnvironment()
        .withVariable("time_type", TimeType.matcher.getNormalizedName(timeType))
        .withVariable("top_type", normalizedType.getNormalizedName())
        .withVariable("top_direction", normalizedDirection.getNormalizedName())
        .withVariable("entries", pageEntries)
        .withVariable("current_page", page)
        .withVariable("number_of_pages", numberOfPages)
        .withVariable("page_size", pageSize)
    );

    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (args.length == 1) {
      var topList = userDataStore.getTopList(TopListType.TOTAL, timeType, TopListDirection.DESCENDING);
      var pageSize = config.rootSection.topListCommandsPageSize;
      var numberOfPages = (topList.size() + pageSize - 1) / pageSize;

      return IntStream.range(1, numberOfPages + 1)
        .mapToObj(String::valueOf)
        .filter(it -> it.startsWith(args[0]))
        .limit(15)
        .toList();
    }

    if (args.length == 2)
      return TopListType.matcher.createCompletions(args[1]);

    if (args.length == 3)
      return TopListDirection.matcher.createCompletions(args[2]);

    return List.of();
  }

  private void printUsage(CommandSender sender, String label) {
    config.rootSection.commonMessages.topCommandUsage.sendMessage(
      sender,
      new InterpretationEnvironment()
        .withVariable("label", label)
        .withVariable("types", TopListType.matcher.createCompletions(null))
        .withVariable("directions", TopListDirection.matcher.createCompletions(null))
    );
  }
}
