package com.golfing8.playas.module.cmd;

import com.golfing8.kcommon.command.Cmd;
import com.golfing8.kcommon.command.CommandContext;
import com.golfing8.kcommon.command.MCommand;
import com.golfing8.kcommon.command.argument.CommandArguments;
import com.golfing8.kcommon.command.requirement.RequirementPlayer;
import com.golfing8.kcommon.config.lang.LangConf;
import com.golfing8.kcommon.config.lang.Message;
import com.golfing8.kcommon.util.MS;
import com.golfing8.playas.module.PlayAsModule;
import com.golfing8.playas.module.integration.VersionAdapterDefault;
import org.spigotmc.SpigotConfig;

/**
 * Lets a spoofing player stop spoofing.
 */
@Cmd(
        name = "quitspoofing",
        description = "Logs you back in to your original account",
        permission = ""
)
public class QuitSpoofingCMD extends MCommand<PlayAsModule> {
    @LangConf
    private Message stoppedSpoofingMsg = new Message("&cYou stopped spoofing &e{PLAYER}&c.");

    @Override
    protected void onRegister() {
        addRequirement(RequirementPlayer.getInstance());
    }

    @Override
    protected void execute(CommandContext context) {
        if (!getModule().isSpoofed(context.getPlayer())) {
            MS.pass(context.getPlayer(), SpigotConfig.unknownCommandMessage);
            return;
        }

        stoppedSpoofingMsg.send(context.getSender(), "PLAYER", context.getSender().getName());
        getModule().stopSpoofing(context.getPlayer());
    }
}
