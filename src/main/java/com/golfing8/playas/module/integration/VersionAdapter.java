package com.golfing8.playas.module.integration;

import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Provides functionality across different versions of minecraft.
 */
public interface VersionAdapter {
    /**
     * Logs in the given player as the other player.
     *
     * @param currentPlayer the current player.
     * @param id the id of the player.
     * @param username the username.
     * @return the logged in player.
     */
    CompletableFuture<Player> logInAsPlayer(Player currentPlayer, UUID id, String username);

    /**
     * Updates the given player's skin.
     *
     * @param player the player.
     * @param originalUUID the original UUID of the player.
     */
    void updatePlayerProfile(Player player, UUID originalUUID);
}
