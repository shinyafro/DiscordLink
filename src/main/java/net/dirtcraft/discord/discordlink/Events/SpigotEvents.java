package net.dirtcraft.discord.discordlink.Events;

import net.dirtcraft.discord.discordlink.API.GameChat;
import net.dirtcraft.discord.discordlink.Compatability.PlatformPlayer;
import net.dirtcraft.discord.discordlink.Compatability.PlatformUtils;
import net.dirtcraft.discord.discordlink.Storage.Permissions;
import net.dirtcraft.discord.discordlink.Storage.PluginConfiguration;
import net.dirtcraft.discord.discordlink.DiscordLink;
import net.dirtcraft.discord.discordlink.Utility.Utility;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class SpigotEvents implements Listener {
    public SpigotEvents(Chat chat){
        this.vault = chat;
    }
    private final Chat vault;
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChat(AsyncPlayerChatEvent event){
         if (event.isCancelled()) return;
        String prefix = Utility.sanitiseMinecraftText(vault.getPlayerPrefix(event.getPlayer()));
        String nickName = Utility.sanitiseMinecraftText(event.getPlayer().getDisplayName());
        String message = Utility.sanitiseMinecraftText(event.getMessage());
        GameChat.sendPlayerMessage(prefix, nickName, message);
        if (event.getPlayer().hasPermission(Permissions.COLOUR_CHAT)) {
            String s = event.getMessage();
            s = Utility.formatColourCodes(s);
            event.setMessage(s);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        PlatformPlayer player = PlatformUtils.getPlayer(event.getPlayer());
        if (player.isVanished()) return;
        if (player.hasPlayedBefore()) {
            String prefix = Utility.removeColourCodes(vault.getPlayerPrefix(event.getPlayer()));
            GameChat.sendMessage(PluginConfiguration.Format.playerJoin
                    .replace("{username}", player.getName())
                    .replace("{prefix}", prefix)
            );
        } else {
            MessageEmbed embed = Utility
                    .embedBuilder()
                    .setDescription(PluginConfiguration.Format.newPlayerJoin
                            .replace("{username}", player.getName()))
                    .build();
            GameChat.sendMessage(embed);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDisconnect(PlayerQuitEvent event) {
        PlatformPlayer player = PlatformUtils.getPlayer(event.getPlayer());
        if (player.isVanished()) return;
        String prefix = Utility.removeColourCodes(vault.getPlayerPrefix(event.getPlayer()));
        GameChat.sendMessage(PluginConfiguration.Format.playerDisconnect
                .replace("{username}", player.getName())
                .replace("{prefix}", prefix)
        );
    }

}
