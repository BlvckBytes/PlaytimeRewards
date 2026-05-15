package at.blvckbytes.playtime_rewards.command.main;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.playtime_rewards.command.OfflinePlayerRegistry;
import at.blvckbytes.playtime_rewards.config.MainSection;
import at.blvckbytes.playtime_rewards.duration_syntax.DurationException;
import at.blvckbytes.playtime_rewards.duration_syntax.DurationSyntax;
import at.blvckbytes.playtime_rewards.store.*;
import me.blvckbytes.syllables_matcher.NormalizedConstant;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.logging.Level;

public class MainCommand implements CommandExecutor, TabCompleter {

  private final OfflinePlayerRegistry offlinePlayerRegistry;
  private final UserDataStore userDataStore;
  private final CalendarInfoProvider calendarInfoProvider;
  private final ConfigKeeper<MainSection> config;
  private final Plugin plugin;

  public MainCommand(
    OfflinePlayerRegistry offlinePlayerRegistry,
    UserDataStore userDataStore,
    CalendarInfoProvider calendarInfoProvider,
    ConfigKeeper<MainSection> config,
    Plugin plugin
  ) {
    this.offlinePlayerRegistry = offlinePlayerRegistry;
    this.userDataStore = userDataStore;
    this.calendarInfoProvider = calendarInfoProvider;
    this.config = config;
    this.plugin = plugin;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!command.testPermission(sender))
      return true;

    NormalizedConstant<CommandAction> normalizedAction;

    if (args.length == 0 || (normalizedAction = CommandAction.matcher.matchFirst(args[0])) == null) {
      config.rootSection.commands.main.commandUsage.sendMessage(
        sender,
        new InterpretationEnvironment()
          .withVariable("label", label)
          .withVariable("actions", CommandAction.matcher.createCompletions(null))
      );

      return true;
    }

    switch (normalizedAction.constant) {
      case RELOAD -> {
        try {
          config.reload();
          config.rootSection.commands.main.reloadedSuccessfully.sendMessage(sender);
        } catch (Exception e) {
          config.rootSection.commands.main.errorWhileReloading.sendMessage(sender);
          plugin.getLogger().log(Level.SEVERE, "An error occurred while trying to reload the config", e);
        }
        return true;
      }

      case ADD_PLAY_TIME, SUBTRACT_PLAY_TIME, ADD_AFK_TIME, SUBTRACT_AFK_TIME -> {
        if (args.length < 3) {
          config.rootSection.commands.main.timeModificationUsage.sendMessage(
            sender,
            new InterpretationEnvironment()
              .withVariable("label", label)
              .withVariable("action", normalizedAction.getNormalizedName())
          );

          return true;
        }

        var target = offlinePlayerRegistry.getPlayerByName(args[1]);

        if (target == null) {
          config.rootSection.commonMessages.hasNotPlayedBefore.sendMessage(
            sender,
            new InterpretationEnvironment()
              .withVariable("name", args[1])
          );

          return true;
        }

        var amountString = String.join(" ", Arrays.asList(args).subList(2, args.length));

        long duration;

        try {
          duration = DurationSyntax.parseSyntaxIntoTicks(amountString);
        } catch (DurationException e) {
          var environment = new InterpretationEnvironment()
            .withVariable("input", amountString)
            .withVariable("error_token", e.errorToken)
            .withVariable("supported_units", DurationSyntax.SUPPORTED_UNIT_CHARS);

          switch (e.error) {
            case UNSUPPORTED_UNIT -> config.rootSection.commands.main.unsupportedDurationUnit.sendMessage(sender, environment);
            case MALFORMED_NUMBER -> config.rootSection.commands.main.malformedDurationNumber.sendMessage(sender, environment);
            case NEGATIVE_NUMBER -> config.rootSection.commands.main.negativeDurationNumber.sendMessage(sender, environment);
          }

          return true;
        }

        if (duration == 0) {
          config.rootSection.commands.main.zeroDuration.sendMessage(sender);
          return true;
        }

        var timeType = switch (normalizedAction.constant) {
          case ADD_PLAY_TIME, SUBTRACT_PLAY_TIME -> TimeType.PLAY_TIME;
          case ADD_AFK_TIME, SUBTRACT_AFK_TIME -> TimeType.AFK_TIME;
          default -> throw new IllegalStateException("Unaccounted-for action: " + normalizedAction.constant);
        };

        var userData = userDataStore.access(target);

        long priorValue;
        boolean didAdd;

        switch (normalizedAction.constant) {
          case ADD_PLAY_TIME, ADD_AFK_TIME -> {
            didAdd = true;
            priorValue = userData.getTotalTimeTicks(timeType);
            userData.incrementTime(timeType, duration, calendarInfoProvider);
          }

          case SUBTRACT_PLAY_TIME, SUBTRACT_AFK_TIME -> {
            didAdd = false;
            priorValue = userData.getTotalTimeTicks(timeType);
            userData.decrementTime(timeType, duration, calendarInfoProvider);
          }

          default -> throw new IllegalStateException("Unaccounted-for action: " + normalizedAction.constant);
        }

        config.rootSection.commands.main.timeModifiedSuccessfully.sendMessage(
          sender,
          new InterpretationEnvironment()
            .withVariable("player_name", userData.getLastKnownName())
            .withVariable("time_type", TimeType.matcher.getNormalizedName(timeType))
            .withVariable("prior_value", priorValue)
            .withVariable("new_value", userData.getTotalTimeTicks(timeType))
            .withVariable("delta_time", duration)
            .withVariable("did_add", didAdd)
        );

        return true;
      }

      // For now, this will remain a hackish one-off utility, seeing how specific it is.
      case MIGRATE_REWARDS_LITE -> {
        var userdataFolder = Paths.get(
          plugin.getDataFolder().getParentFile().getAbsolutePath(),
          "RewardsLite", "userdata"
        ).toFile();

        if (!userdataFolder.isDirectory()) {
          sender.sendMessage("§cThere's no userdata-folder at " + userdataFolder);
          return true;
        }

        var outputFolder = new File(plugin.getDataFolder(), "RewardsLiteMigration");

        if (!outputFolder.exists()) {
          if (!outputFolder.mkdirs()) {
            sender.sendMessage("§cCould not create output-folder at " + outputFolder);
            return true;
          }
        }

        else if (!outputFolder.isDirectory()) {
          sender.sendMessage("§cEncountered unexpected file at " + outputFolder);
          return true;
        }

        var userdataFiles = userdataFolder.listFiles();

        if (userdataFiles == null || userdataFiles.length == 0) {
          sender.sendMessage("§cThe userdata-folder at " + userdataFolder + " is empty");
          return true;
        }

        var migratedFileCount = 0;

        for (var userdataFile : userdataFiles) {
          if (!userdataFile.isFile())
            continue;

          UUID playerId;

          try {
            var fileName = userdataFile.getName();
            playerId = UUID.fromString(fileName.substring(0, fileName.indexOf('.')));
          } catch (Throwable e) {
            sender.sendMessage("§cEncountered malformed UUID at file-name of " + userdataFile + "; skipping");
            continue;
          }

          var userdataConfig = YamlConfiguration.loadConfiguration(userdataFile);

          var afkTime = userdataConfig.getInt("afkTime", -1);
          var playTime = userdataConfig.getInt("playtime", -1);
          var lastKnownName = userdataConfig.getString("lastKnownName");

          if (afkTime < 0 || playTime < 0 || lastKnownName == null) {
            sender.sendMessage("§cEncountered malformed data-file at " + userdataFile + "; skipping");
            continue;
          }

          var internalUserData = UserData.makeInitial(playerId, lastKnownName, calendarInfoProvider);

          if (playTime > 0)
            internalUserData.incrementTime(TimeType.PLAY_TIME, playTime, calendarInfoProvider);

          if (afkTime > 0)
            internalUserData.incrementTime(TimeType.AFK_TIME, afkTime, calendarInfoProvider);

          internalUserData.resetAllCalendarBuckets();

          var outputFile = new File(outputFolder, playerId + ".yml");

          try (var writer = new FileWriter(outputFile)) {
            writer.write(internalUserData.serialize());
          } catch (Throwable e) {
            sender.sendMessage("§cCould not write output to " + outputFile + ": " + e.getMessage());
            continue;
          }

          ++migratedFileCount;
        }

        sender.sendMessage("§aMigrated " + migratedFileCount + " userdata-files");
        return true;
      }

      case EXPORT_USERDATA -> {
        NormalizedConstant<TimeType> timeType;
        NormalizedConstant<TopListType> topType;
        NormalizedConstant<TopListDirection> direction;

        if (
          args.length != 4
            || (timeType = TimeType.matcher.matchFirst(args[1])) == null
            || (topType = TopListType.matcher.matchFirst(args[2])) == null
            || (direction = TopListDirection.matcher.matchFirst(args[3])) == null
        ) {
          config.rootSection.commands.main.exportUsage.sendMessage(
            sender,
            new InterpretationEnvironment()
              .withVariable("label", label)
              .withVariable("action", normalizedAction.getNormalizedName())
              .withVariable("time_types", TimeType.matcher.createCompletions(null))
              .withVariable("top_types", TopListType.matcher.createCompletions(null))
              .withVariable("directions", TopListDirection.matcher.createCompletions(null))
          );
          return true;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
          var userData = userDataStore.getAllUserData();

          direction.constant.sort(userData, topType.constant, timeType.constant);

          var currentDate = LocalDateTime.now(config.rootSection._timeZone);
          var dateStamp = DateTimeFormatter.ofPattern("ddMMyyyy-HHmmss").format(currentDate);
          var typeString = (timeType.getNormalizedName() + "-" + topType.getNormalizedName() + "-" + direction.getNormalizedName()).toLowerCase();

          var outputFile = new File(plugin.getDataFolder(), "export-" + dateStamp + "-" + typeString + ".csv");

          try (var writer = new FileWriter(outputFile)) {
            writer.write(makeCSVRow("name","uuid","time"));

            for (var dataEntry : userData) {
              var timeString = config.rootSection.commands.main.exportTimeFormat.asPlainString(
                new InterpretationEnvironment()
                  .withVariable("time", topType.constant.accessStatistic(dataEntry, timeType.constant))
              );

              writer.write('\n');
              writer.write(makeCSVRow(dataEntry.getLastKnownName(), dataEntry.playerId, timeString));
            }
          } catch (Throwable e) {
            plugin.getLogger().log(Level.SEVERE, "An error occurred while trying to write to " + outputFile, e);
            config.rootSection.commands.main.exportWriteError.sendMessage(sender);
            return;
          }

          config.rootSection.commands.main.exportSuccess.sendMessage(
            sender,
            new InterpretationEnvironment()
              .withVariable("file_path", plugin.getDataFolder().getParentFile().toPath().relativize(outputFile.toPath()).toString())
          );
        });

        return true;
      }
    }

    throw new IllegalStateException("Unaccounted-for action: " + normalizedAction.constant);
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!command.testPermission(sender))
      return List.of();

    if (args.length == 1)
      return CommandAction.matcher.createCompletions(args[0]);

    var normalizedAction = CommandAction.matcher.matchFirst(args[0]);

    if (normalizedAction == null)
      return List.of();

    if (normalizedAction.constant == CommandAction.EXPORT_USERDATA) {
      if (args.length == 2)
        return TimeType.matcher.createCompletions(args[1]);

      if (args.length == 3)
        return TopListType.matcher.createCompletions(args[2]);

      if (args.length == 4)
        return TopListDirection.matcher.createCompletions(args[3]);

      return List.of();
    }

    switch (normalizedAction.constant) {
      case ADD_PLAY_TIME, SUBTRACT_PLAY_TIME, ADD_AFK_TIME, SUBTRACT_AFK_TIME -> {
        if (args.length == 2) {
          var typedNameLower = args[1].toLowerCase();

          return offlinePlayerRegistry.streamKnownNames()
            .filter(it -> it.toLowerCase().startsWith(typedNameLower))
            .limit(15)
            .toList();
        }
      }
    }

    return List.of();
  }

  private String makeCSVRow(Object... fields) {
    var result = new StringJoiner(";");

    for (var field : fields)
      result.add(String.valueOf(field).replace(";", ""));

    return result.toString();
  }
}
