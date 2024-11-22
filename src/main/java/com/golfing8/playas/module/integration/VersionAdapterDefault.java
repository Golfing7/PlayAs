package com.golfing8.playas.module.integration;

import com.golfing8.kcommon.nms.reflection.FieldHandle;
import com.golfing8.kcommon.nms.reflection.FieldHandles;
import com.golfing8.playas.PlayAsPlugin;
import com.google.common.base.Charsets;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.jetbrains.annotations.Contract;
import org.spigotmc.SpigotConfig;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implements the VersionAdapter functionality for MC 1.8.8
 */
public class VersionAdapterDefault implements VersionAdapter {
    private static final FieldHandle<?> PLAYERLIST_HANDLE = FieldHandles.getHandle("b", PacketPlayOutPlayerInfo.class);

    @Override
    public CompletableFuture<Player> logInAsPlayer(Player currentPlayer, UUID id, String username) {
        EntityPlayer nmsPlayer = ((CraftPlayer) currentPlayer).getHandle();
        PlayerList playerList = MinecraftServer.getServer().getPlayerList();

        GameProfile cloneProfile = new GameProfile(nmsPlayer.getProfile().getId(), nmsPlayer.getProfile().getName());
        cloneProfile.getProperties().putAll(nmsPlayer.getProfile().getProperties());

        PacketPlayOutPlayerInfo despawn = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER, nmsPlayer);
        nmsPlayer.playerConnection.sendPacket(despawn);
        // Disconnect the active player
        nmsPlayer.q();
        playerList.disconnect(nmsPlayer);

        CompletableFuture<Player> future = new CompletableFuture<>();
        CompletableFuture<EntityPlayer> replacement = loginNewPlayer(nmsPlayer, id, username);
        replacement.whenComplete((result, t) -> {
            Bukkit.getScheduler().runTask(PlayAsPlugin.getInstance(), () -> {
                playerList.a(nmsPlayer.playerConnection.networkManager, result);

                // Send a respawn packet to the player.
                updatePlayerProfile(result.getBukkitEntity(), currentPlayer.getUniqueId());

                future.complete(result.getBukkitEntity());
            });
        });
        return future;
    }

    @Override
    public void updatePlayerProfile(Player player, UUID originalUUID) {
        EntityPlayer nmsPlayer = ((CraftPlayer) player).getHandle();

        // Prepare the despawn packet.
        PacketPlayOutPlayerInfo despawn = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER);
        List<PacketPlayOutPlayerInfo.PlayerInfoData> despawnInfo = (List<PacketPlayOutPlayerInfo.PlayerInfoData>) PLAYERLIST_HANDLE.get(despawn);
        despawnInfo.add(despawn.new PlayerInfoData(new GameProfile(originalUUID, null), 0, WorldSettings.EnumGamemode.NOT_SET, nmsPlayer.getPlayerListName()));

        // Send a respawn packet to the player.
        PacketPlayOutPlayerInfo respawn = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER);
        List<PacketPlayOutPlayerInfo.PlayerInfoData> respawnInfo = (List<PacketPlayOutPlayerInfo.PlayerInfoData>) PLAYERLIST_HANDLE.get(respawn);
        GameProfile spoofedProfile = new GameProfile(originalUUID, nmsPlayer.getProfile().getName());
        spoofedProfile.getProperties().putAll(nmsPlayer.getProfile().getProperties());
        respawnInfo.add(respawn.new PlayerInfoData(spoofedProfile, 0, nmsPlayer.playerInteractManager.getGameMode(), nmsPlayer.getPlayerListName()));
        nmsPlayer.playerConnection.sendPacket(respawn);

        // After a bit, remove the spoofed profile.
        Bukkit.getScheduler().runTaskLater(PlayAsPlugin.getInstance(), () -> {
            nmsPlayer.playerConnection.sendPacket(despawn);
        }, 1);
        MinecraftServer.getServer().getPlayerList().moveToWorld(nmsPlayer, nmsPlayer.dimension, false, nmsPlayer.getBukkitEntity().getLocation(), false);
    }

    /**
     * Builds the given entity player from the UUID or the username, whichever is not null.
     *
     * @param original the original player
     * @param playerUUID the player's UUID
     * @param username the username of the player.
     * @return logs in the new player
     */
    @Contract("_, null, null -> fail")
    private CompletableFuture<EntityPlayer> loginNewPlayer(EntityPlayer original, UUID playerUUID, String username) {
        if (playerUUID == null && username == null)
            throw new IllegalArgumentException("One argument must be null.");

        CompletableFuture<EntityPlayer> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            GameProfile gameProfile;
            UUID fakeUUID = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(Charsets.UTF_8));
            if (playerUUID != null) {
                if (MinecraftServer.getServer().getOnlineMode() || SpigotConfig.bungee) {
                    gameProfile = MinecraftServer.getServer().aD().fillProfileProperties(new GameProfile(playerUUID, username), true);
                } else {
                    gameProfile = new GameProfile(fakeUUID, username);
                }
            } else {
                if (MinecraftServer.getServer().getOnlineMode() || SpigotConfig.bungee) {
                    gameProfile = MinecraftServer.getServer().aD().fillProfileProperties(MinecraftServer.getServer().getUserCache().getProfile(username), true);
                } else {
                    gameProfile = new GameProfile(fakeUUID, username);
                }
            }

            GameProfile clone = new GameProfile(original.getUniqueID(), gameProfile.getName());
            clone.getProperties().putAll(gameProfile.getProperties());

            AsyncPlayerPreLoginEvent preLoginEvent = new AsyncPlayerPreLoginEvent(
                    gameProfile.getName(),
                    ((java.net.InetSocketAddress) original.playerConnection.networkManager.getSocketAddress()).getAddress(),
                    gameProfile.getId()
            );
            preLoginEvent.callEvent();

            Bukkit.getScheduler().runTask(PlayAsPlugin.getInstance(), () -> {
                EntityPlayer newPlayer = MinecraftServer.getServer().getPlayerList().attemptLogin(
                        new LoginListener(MinecraftServer.getServer(), original.playerConnection.networkManager),
                        gameProfile,
                        original.playerConnection.networkManager.getSocketAddress().toString()
                );
                future.complete(newPlayer);
            });
        });
        return future;
    }
}
