package net.dirtcraft.discord.discordlink.Utility.Compatability.Permission.LuckPerms;

import me.lucko.luckperms.api.*;
import me.lucko.luckperms.api.caching.CachedData;
import me.lucko.luckperms.api.caching.MetaData;
import me.lucko.luckperms.api.context.ContextSet;
import net.dirtcraft.discord.discordlink.API.MessageSource;
import net.dirtcraft.discord.discordlink.Utility.Compatability.Platform.PlatformPlayer;
import net.dirtcraft.discord.discordlink.Utility.Compatability.Platform.PlatformUser;
import net.dirtcraft.discord.discordlink.Utility.Pair;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static net.dirtcraft.discord.discordlink.Storage.Permission.PROMOTE_PERMISSION_GROUP_PREFIX;

public class Api4 extends LuckPermissions {
    @NonNull
    private LuckPermsApi api = me.lucko.luckperms.LuckPerms.getApi();
    private ContextSet contexts = api.getContextManager().getStaticContexts().getContexts();

    @Override
    public void printUserGroups(MessageSource source, PlatformUser player) {
        me.lucko.luckperms.api.manager.UserManager userManager = api.getUserManager();
        StringBuilder local = new StringBuilder("__**Local**__\n");
        StringBuilder remote = new StringBuilder("__**Other Servers**__\n");
        userManager.loadUser(player.getUUID()).join().getPermissions().stream()
                .filter(n -> n.getPermission().startsWith("group."))
                .filter(n -> !n.getPermission().endsWith(".default"))
                .forEach(n-> sortToString(n, local, remote, 6));

        if (local.toString().equalsIgnoreCase("__**Local**__\n")) remote.append("None found.\n");
        if (remote.toString().equalsIgnoreCase("__**Other Servers**__\n")) remote.append("None found.");
        else remote.deleteCharAt(remote.length()-1);

        source.sendCommandResponse(player.getName() + "'s Kits:", String.join("\n", local, remote));
    }

    @Override
    public void printUserKits(MessageSource source, PlatformUser player) {
        me.lucko.luckperms.api.manager.UserManager userManager = api.getUserManager();
        StringBuilder local = new StringBuilder("__**Local**__\n");
        StringBuilder remote = new StringBuilder("__**Other Servers**__\n");
        userManager.loadUser(player.getUUID()).join().getPermissions().stream()
                .filter(n -> n.getPermission().startsWith("nucleus.kits."))
                .forEach(n-> sortToString(n, local, remote, 13));

        if (local.toString().equalsIgnoreCase("__**Local**__\n")) remote.append("None found.\n");
        if (remote.toString().equalsIgnoreCase("__**Other Servers**__\n")) remote.append("None found.");
        else remote.deleteCharAt(remote.length()-1);

        source.sendCommandResponse(player.getName() + "'s Kits:", String.join("\n", local, remote));
    }

    public void sortToString(Node n, StringBuilder local, StringBuilder remote, int trim){
        if (n.isServerSpecific() && n.getFullContexts().isSatisfiedBy(contexts)) {
            local.append(n.getPermission().substring(trim))
                    .append("\n");
        } else if (n.appliesGlobally()) {
            local.append(n.getPermission().substring(trim))
                    .append(" *[global]*\n");
        } else {
            remote.append(n.getPermission().substring(trim))
                    .append(" *[")
                    .append(n.getFullContexts().getAnyValue("server").orElse("unknown"))
                    .append("]*\n");
        }
    }

    public Optional<RankUpdate> modifyRank(@Nullable PlatformPlayer source, @Nullable UUID targetUUID, @Nullable String trackName, boolean promote){
        Optional<me.lucko.luckperms.api.User> target = Optional.ofNullable(targetUUID)
                .map(api.getUserManager()::loadUser)
                .map(CompletableFuture::join);
        Optional<Track> track = Optional.ofNullable(trackName)
                .map(api.getTrackManager()::getTrack);

        if (source == null || !target.isPresent() || !track.isPresent()) return Optional.empty();
        else if (promote) return promoteTarget(source, target.get(), track.get());
        else return demoteTarget(source, target.get(), track.get());
    }

    @Override
    public Map<String, String> getUserGroupPrefixMap(PlatformUser user) {
        @NonNull Set<Group> groups = api.getGroupManager().getLoadedGroups();
        return groups.stream()
                .filter(g->user.hasPermission(g.getName()))
                .map(g->new Pair<>(g.getDisplayName(), g.getCachedData().calculateMeta(Contexts.of(contexts, Contexts.global().getSettings())).getPrefix()))
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    }

    @Override
    public Optional<String> getGroupPrefix(String name) {
        Group group = api.getGroupManager().getGroup(name);
        return Optional.ofNullable(group)
                .map(Group::getCachedData)
                .map(cd->cd.getMetaData(Contexts.of(contexts, Contexts.global().getSettings())))
                .map(MetaData::getPrefix);
    }

    @Override
    public boolean isInGroup(PlatformUser user, String group) {
        return user.hasPermission(group);
    }

    @Override
    public boolean groupHasPermission(String group, String perm) {
        Group g = api.getGroupManager().getGroup(group);
        return Optional.ofNullable(g)
                .map(Group::getCachedData)
                .map(cd->cd.getPermissionData(Contexts.of(contexts, Contexts.global().getSettings())))
                .map(pd->pd.getPermissionValue(perm))
                .map(Tristate::asBoolean)
                .orElse(false);
    }

    private Optional<RankUpdate> demoteTarget(PlatformPlayer source, me.lucko.luckperms.api.User targetUser, Track track) {
        try {
            List<String> groups = track.getGroups();
            SortedSet<? extends Node> targetNodes = targetUser.getPermissions();
            String previousGroup = "default";
            Node previousNode = null;
            for (int i = groups.size(); i > 0; ) {
                final String group = groups.get(--i);
                if (group.equalsIgnoreCase("default")) continue;
                final Node node = api.buildNode("group." + group).setServer(getServerContext()).build();
                if (targetNodes.contains(node)) {
                    previousGroup = group;
                    previousNode = node;
                } else if (previousNode != null && hasPermission(source, previousGroup)) {
                    setRank(targetUser, node, previousNode);
                    return Optional.of(new RankUpdate(targetUser.getUuid(), group, previousGroup));
                }
            }
            if (hasPermission(source, previousGroup)) {
                setRank(targetUser, null, previousNode);
                return Optional.of(new RankUpdate(targetUser.getUuid(), null, previousGroup));
            } else return Optional.empty();
        } catch (Throwable e){
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private Optional<RankUpdate> promoteTarget(PlatformPlayer source, me.lucko.luckperms.api.User targetUser, Track track) {
        List<String> groups = track.getGroups();
        SortedSet<? extends Node> targetNodes = targetUser.getPermissions();
        String previousGroup = "default";
        Node previousNode = null;
        for (int i = groups.size(); i > 0; ) {
            final String group = groups.get(--i);
            if (group.equalsIgnoreCase("default")) continue;
            final Node node = api.buildNode("group." + group).setServer(getServerContext()).build();
            if (!targetNodes.contains(node)) {
                previousGroup = group;
                previousNode = node;
            } else if (previousNode != null && hasPermission(source, previousGroup)) {
                setRank(targetUser, previousNode, node);
                return Optional.of(new RankUpdate(targetUser.getUuid(), previousGroup, group));
            } else return Optional.empty();
        }
        if (hasPermission(source, previousGroup)) {
            setRank(targetUser, previousNode, null);
            return Optional.of(new RankUpdate(targetUser.getUuid(), previousGroup, null));
        } else return Optional.empty();
    }

    private void setRank(me.lucko.luckperms.api.User target, Node add, Node remove){
        if (remove != null) target.unsetPermission(remove);
        if (add != null) target.setPermission(add);
        api.getUserManager().saveUser(target);
    }

    private boolean hasPermission(PlatformPlayer source, String group){
        return source.hasPermission(PROMOTE_PERMISSION_GROUP_PREFIX + group);
    }

    public String getServerContext(){
        return contexts.getAnyValue("server").orElse("global");
    }

    public Optional<String> getPrefix(UUID uuid){
        return Optional.ofNullable(api.getUserManager().getUser(uuid))
                .map(u->u.getCachedData().getMetaData(Contexts.of(contexts, Contexts.global().getSettings())))
                .map(MetaData::getPrefix);
    }

    @Override
    public boolean hasPermission(UUID uuid, String permission) {
        User user = api.getUserManager().getUser(uuid);
        return user != null && user.getCachedData()
                .getPermissionData(Contexts.of(contexts, Contexts.global().getSettings()))
                .getPermissionValue(permission)
                .asBoolean();
    }
}