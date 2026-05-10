package at.blvckbytes.playtime_rankup.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class OfflinePlayerRegistry implements Listener {

  private final List<String> knownNames;
  private final Map<String, UUID> idByNameLower;

  public OfflinePlayerRegistry() {
    this.knownNames = new ArrayList<>();
    this.idByNameLower = new HashMap<>();

    for (var offlinePlayer : Bukkit.getOfflinePlayers()) {
      var name = offlinePlayer.getName();

      if (name != null)
        addKnownName(name, offlinePlayer.getUniqueId());
    }
  }

  public Stream<String> streamKnownNames() {
    return knownNames.stream();
  }

  public @Nullable OfflinePlayer getPlayerByName(String name) {
    var playerId = idByNameLower.get(name.toLowerCase());

    if (playerId == null)
      return null;

    return Bukkit.getOfflinePlayer(playerId);
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    var player = event.getPlayer();

    addKnownName(player.getName(), player.getUniqueId());
    addKnownName(componentToText(player.displayName()), player.getUniqueId());
  }

  private void addKnownName(String name, UUID playerId) {
    var trimmedName = name.trim();

    if (trimmedName.isBlank())
      return;

    if (knownNames.stream().anyMatch(it -> it.equalsIgnoreCase(trimmedName)))
      return;

    knownNames.add(trimmedName);
    idByNameLower.put(trimmedName.toLowerCase(), playerId);
  }

  private String componentToText(Component component) {
    var result = new StringBuilder();

    forEachChildrenAndSelf(component, current -> {
      if (current instanceof TextComponent textComponent)
        result.append(textComponent.content());
    });

    return result.toString();
  }

  private void forEachChildrenAndSelf(Component component, Consumer<Component> handler) {
    handler.accept(component);

    for (var child : component.children())
      forEachChildrenAndSelf(child, handler);
  }
}
