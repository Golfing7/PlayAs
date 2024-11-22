package com.golfing8.playas.module.cmd;

import com.golfing8.kcommon.command.Cmd;
import com.golfing8.kcommon.command.CommandContext;
import com.golfing8.kcommon.command.MCommand;
import com.golfing8.kcommon.command.argument.CommandArguments;
import com.golfing8.kcommon.command.requirement.RequirementPlayer;
import com.golfing8.kcommon.config.lang.LangConf;
import com.golfing8.kcommon.config.lang.Message;
import com.golfing8.playas.module.PlayAsModule;
import com.golfing8.playas.module.integration.VersionAdapterDefault;

/**
 * Lets a spoofing player stop spoofing.
 */
@Cmd(
        name = "quitspoofing",
        description = "Logs you back in to your original account"
)
public class QuitSpoofingCMD extends MCommand<PlayAsModule> {
    @LangConf
    private Message stoppedSpoofingMsg = new Message("&cYou stopped spoofing &e{PLAYER}&c.");

    @LangConf
    private Message notSpoofingAnyoneMsg = new Message("&cYou aren't spoofing anyone.");

    @Override
    protected void onRegister() {
        addRequirement(RequirementPlayer.getInstance());
    }

    @Override
    protected void execute(CommandContext context) {
        if (!getModule().isSpoofed(context.getPlayer())) {
            notSpoofingAnyoneMsg.send(context.getPlayer());
            return;
        }

        stoppedSpoofingMsg.send(context.getSender(), "PLAYER", context.getSender().getName());
        getModule().stopSpoofing(context.getPlayer());
    }
}
