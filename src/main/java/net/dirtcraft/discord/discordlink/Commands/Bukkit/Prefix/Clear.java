package net.dirtcraft.discord.discordlink.Commands.Bukkit.Prefix;

import net.dirtcraft.discord.discordlink.Commands.Bukkit.ThermosSubCommand;
import net.dirtcraft.discord.discordlink.Commands.Sources.ConsoleSource;
import net.dirtcraft.discord.discordlink.Storage.Permission;
import net.dirtcraft.discord.discordlink.Utility.Compatability.Permission.PermissionUtils;
import net.dirtcraft.discord.discordlink.Utility.Compatability.Platform.PlatformUser;
import net.dirtcraft.discord.discordlink.Utility.Compatability.Platform.PlatformUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

public class Clear extends ThermosSubCommand {
    public Clear(){
        super(Permission.PREFIX_CLEAR);
    }

    @Override
    public boolean onCommand(CommandSender commandSender, List<String> strings) {
        final Optional<PlatformUser> target = !strings.isEmpty() ? PlatformUtils.getPlayerOffline(strings.get(0)): PlatformUtils.getPlayerOffline(commandSender); //args.<User>getOne("Target").orElseThrow(()->new CommandException(Text.of("§cYou must specify a target.")));
        if (!target.isPresent()) {
            commandSender.sendMessage("You must specify a valid player.");
        } else if ((!(commandSender instanceof Player) || !target.map(PlatformUser::getUUID).get().equals(((Player) commandSender).getUniqueId())) && !commandSender.hasPermission(Permission.PREFIX_OTHERS)){
            commandSender.sendMessage("You do not have permission to set other players prefixes.");
        } else {
            PermissionUtils.INSTANCE.clearPlayerPrefix(getSource(commandSender), target.get());
        }
        return true;
    }

    private ConsoleSource getSource(CommandSender source){
        return new ConsoleSource(){
            @Override
            public void sendMessage(String message) {
                source.sendMessage(message);
            }
        };
    }
}