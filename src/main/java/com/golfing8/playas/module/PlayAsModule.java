package com.golfing8.playas.module;

import com.golfing8.kcommon.module.Module;
import com.golfing8.kcommon.module.ModuleInfo;
import com.golfing8.playas.module.cmd.PlayAsCMD;
import com.golfing8.playas.module.cmd.QuitSpoofingCMD;
import com.golfing8.playas.module.integration.VersionAdapter;
import com.golfing8.playas.module.integration.VersionAdapterDefault;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lets admins log into the accounts of other players as if they were them.
 */
@ModuleInfo(
        name = "playas"
)
public class PlayAsModule extends Module {
    /**
     * Maps a currently spoofed player to the original UUID they had.
     */
    private Map<Player, UUID> spoofedPlayerMap;
    /** The current NMS handle of integration */
    private VersionAdapter adapter;

    @Override
    public void onEnable() {
        this.spoofedPlayerMap = new ConcurrentHashMap<>();
        this.adapter = new VersionAdapterDefault();

        addCommand(new PlayAsCMD());
        addCommand(new QuitSpoofingCMD());
    }

    @Override
    public void onDisable() {

    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onQuit(PlayerQuitEvent event) {
        spoofedPlayerMap.remove(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChangeWorld(PlayerChangedWorldEvent event) {
        if (!isSpoofed(event.getPlayer()))
            return;

        Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
            adapter.updatePlayerProfile(event.getPlayer(), getOriginalUUID(event.getPlayer()));
        }, 5);
    }

    /**
     * Lets the given player start spoofing the target.
     *
     * @param player the player.
     * @param targetUUID the target ID.
     * @param targetName the target name.
     * @return the loaded spoofed player.
     */
    public CompletableFuture<Player> startSpoofing(Player player, UUID targetUUID, String targetName) {
        UUID unspoofedUUID = this.spoofedPlayerMap.containsKey(player) ? this.spoofedPlayerMap.get(player) : player.getUniqueId();

        CompletableFuture<Player> playerCompletableFuture = this.adapter.logInAsPlayer(player, targetUUID, targetName);
        playerCompletableFuture.whenCompleteAsync((result, t) -> {
            if (this.spoofedPlayerMap.containsKey(player)) {
                if (this.spoofedPlayerMap.get(player).equals(player.getUniqueId())) {
                    this.spoofedPlayerMap.remove(player);
                    return; // At this point, the player is no longer spoofing anyone.
                }
                this.spoofedPlayerMap.remove(player); // If a spoofed player starts spoofing ANOTHER player, don't let it happen.
            }
            if (result != null) {
                this.spoofedPlayerMap.put(result.getPlayer(), unspoofedUUID);
            }
        });
        return playerCompletableFuture;
    }

    /**
     * Forces the given player to stop spoofing.
     *
     * @param spoofedPlayer the spoofed player.
     */
    public void stopSpoofing(Player spoofedPlayer) {
        if (!isSpoofed(spoofedPlayer))
            return;

        this.adapter.logInAsPlayer(spoofedPlayer, this.spoofedPlayerMap.get(spoofedPlayer), null).whenCompleteAsync((result, t) -> {
            this.spoofedPlayerMap.remove(spoofedPlayer);
        });
    }

    /**
     * Checks if the given player is being played by someone else.
     *
     * @param player the player.
     * @return if the player is being played by someone else.
     */
    public boolean isSpoofed(Player player) {
        return this.spoofedPlayerMap.containsKey(player);
    }

    /**
     * Gets the original UUID the spoofed player had.
     *
     * @param spoofedPlayer the spoofed player.
     * @return the original UUID of the player.
     */
    public UUID getOriginalUUID(Player spoofedPlayer) {
        return this.spoofedPlayerMap.get(spoofedPlayer);
    }
}
