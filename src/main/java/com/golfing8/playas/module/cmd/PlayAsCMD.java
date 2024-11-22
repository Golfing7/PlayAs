package com.golfing8.playas.module.cmd;

import com.golfing8.kcommon.command.Cmd;
import com.golfing8.kcommon.command.CommandContext;
import com.golfing8.kcommon.command.MCommand;
import com.golfing8.kcommon.command.argument.CommandArguments;
import com.golfing8.kcommon.command.requirement.RequirementPlayer;
import com.golfing8.kcommon.config.lang.LangConf;
import com.golfing8.kcommon.config.lang.Message;
import com.golfing8.playas.module.PlayAsModule;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

/**
 * Lets the player play as another player.
 */
@Cmd(
        name = "playas",
        description = "Lets you log in as another player"
)
public class PlayAsCMD extends MCommand<PlayAsModule> {
    @LangConf
    private Message cantSpoofSelfMessage = new Message("&cYou can't spoof yourself.");
    @LangConf
    private Message nowPlayingAsMessage = new Message("&aYou are now playing as &e{PLAYER}&a.");

    @Override
    protected void onRegister() {
        addRequirement(RequirementPlayer.getInstance());
        addArgument("name", CommandArguments.ALPHANUMERIC_STRING);
    }

    @Override
    protected void execute(CommandContext context) {
        String name = context.next();
        if (name.equals(context.getPlayer().getName())) {
            cantSpoofSelfMessage.send(context.getPlayer());
            return;
        }
        CompletableFuture<Player> future = getModule().startSpoofing(context.getPlayer(), null, name);
        future.whenCompleteAsync((result, t) -> {
            nowPlayingAsMessage.send(result, "PLAYER", result.getName());
        });
    }
}
