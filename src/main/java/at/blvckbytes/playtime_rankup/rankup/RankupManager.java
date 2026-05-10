package at.blvckbytes.playtime_rankup.rankup;

import at.blvckbytes.cm_mapper.ConfigKeeper;
import at.blvckbytes.cm_mapper.ReloadPriority;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.playtime_rankup.config.MainSection;
import at.blvckbytes.playtime_rankup.config.RankSection;
import at.blvckbytes.playtime_rankup.store.TimeType;
import at.blvckbytes.playtime_rankup.store.UserDataStore;
import net.ess3.api.IEssentials;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class RankupManager implements Listener {

  private static final QueryOptions NON_CONTEXTUAL_QUERY = QueryOptions.nonContextual();

  private final UserDataStore userDataStore;
  private final LuckPerms luckPerms;
  private final IEssentials essentials;
  private final ConfigKeeper<MainSection> config;
  private final Plugin plugin;

  private final List<PlayerAndMeta> currentlyOnline;

  private int relativeTime;

  public RankupManager(
    UserDataStore userDataStore,
    LuckPerms luckPerms,
    IEssentials essentials,
    ConfigKeeper<MainSection> config,
    Plugin plugin
  ) {
    this.userDataStore = userDataStore;
    this.luckPerms = luckPerms;
    this.essentials = essentials;
    this.config = config;
    this.plugin = plugin;

    this.currentlyOnline = new ArrayList<>();

    config.registerReloadListener(this::warnAboutMissingGroups, ReloadPriority.HIGHEST);
    warnAboutMissingGroups();

    var afkPlayersBuffer = new ArrayList<PlayerAndMeta>();
    var activePlayersBuffer = new ArrayList<PlayerAndMeta>();

    Bukkit.getScheduler().runTaskTimer(plugin, () -> {
      ++relativeTime;

      var intervalTicks = config.rootSection.incrementIntervalTicks;

      if (relativeTime % intervalTicks != 0)
        return;

      afkPlayersBuffer.clear();
      activePlayersBuffer.clear();

      for (var playerAndUser : currentlyOnline) {
        if (playerAndUser.essentialsUser.isAfk()) {
          afkPlayersBuffer.add(playerAndUser);
          continue;
        }

        activePlayersBuffer.add(playerAndUser);
      }

      if (!activePlayersBuffer.isEmpty()) {
        userDataStore.batchIncrementTimeFor(TimeType.PLAY_TIME, activePlayersBuffer, intervalTicks);
        checkRankupFor(activePlayersBuffer);
      }

      if (!afkPlayersBuffer.isEmpty())
        userDataStore.batchIncrementTimeFor(TimeType.AFK_TIME, afkPlayersBuffer, intervalTicks);
    }, 0L, 0L);
  }

  private void warnAboutMissingGroups() {
    for (var rankEntry : config.rootSection.ranks.entrySet()) {
      var rankName = rankEntry.getKey();
      var group = luckPerms.getGroupManager().getGroup(rankName);

      if (group == null) {
        plugin.getLogger().warning("Could not find group " + rankEntry + " on LuckPerms; check the config-entry for typos!");
        continue;
      }

      rankEntry.getValue().group = group;
    }
  }

  private void checkRankupFor(Iterable<PlayerAndMeta> players) {
    for (var player : players) {
      var playTime = player.userData.getGlobalTimeTicks(TimeType.PLAY_TIME);

      for (var rank : config.rootSection.rankList) {
        if (rank.group == null)
          continue;

        if (playTime < rank._requiredPlayTimeTicks)
          continue;

        if (!player.claimedRanks.add(rank.group.getName()))
          continue;

        var inheritedGroups = player.luckPermsUser.getInheritedGroups(NON_CONTEXTUAL_QUERY);

        if (inheritedGroups.stream().anyMatch(it -> it.getName().equals(rank.group.getName())))
          continue;

        var node = InheritanceNode.builder(rank.group).build();
        var result = player.luckPermsUser.data().add(node);

        // Let's still act as if they received the parent-group, as to not re-try every second.
        // After next re-join, we'll retry again anyway, so don't needlessly spam.
        if (result != DataMutateResult.SUCCESS) {
          plugin.getLogger().severe("Could not add parent-group " + rank.group.getName() + " to " + player.getPlayerId() + " (" + player.getPlayerName() + ")");
          continue;
        }

        luckPerms.getUserManager().saveUser(player.luckPermsUser);

        onClaimedReward(player, rank);
      }
    }
  }

  private void onClaimedReward(PlayerAndMeta player, RankSection rank) {
    assert rank.group != null;

    plugin.getLogger().info("Added parent-group " + rank.group.getName() + " to " + player.getPlayerId() + " (" + player.getPlayerName() + ")");

    var environment = new InterpretationEnvironment()
      .withVariable("player", player.getPlayerName())
      .withVariable("group", rank.group.getDisplayName())
      .withVariable("required_time", rank._requiredPlayTimeTicks);

    rank.notification.self.sendTo(List.of(player.player), environment);
    rank.notification.broadcast.sendTo(Bukkit.getOnlinePlayers(), environment);
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    var player = event.getPlayer();

    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      var userData = userDataStore.access(player);

      userData.updateLastKnownName(player.getName());

      var essentialsUser = essentials.getUser(player);

      if (essentialsUser == null) {
        plugin.getLogger().severe("Could not access essentials-user for " + player.getUniqueId() + " (" + player.getName() + ")");
        return;
      }

      var luckPermsUser = luckPerms.getUserManager().getUser(player.getUniqueId());

      if (luckPermsUser == null) {
        plugin.getLogger().severe("Could not access luckperms-user for " + player.getUniqueId() + " (" + player.getName() + ")");
        return;
      }

      currentlyOnline.add(new PlayerAndMeta(player, essentialsUser, luckPermsUser, userData));
    }, 1);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    currentlyOnline.removeIf(it -> it.player == event.getPlayer());
  }
}
