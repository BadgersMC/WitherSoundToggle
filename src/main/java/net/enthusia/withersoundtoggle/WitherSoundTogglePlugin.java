package net.enthusia.withersoundtoggle;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEffect;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WitherSoundTogglePlugin extends JavaPlugin implements CommandExecutor, TabCompleter {
    // Minecraft's global Wither-spawn level event, rather than a normal sound packet.
    private static final int WITHER_SPAWN_EVENT = 1023;
    private final Set<UUID> mutedPlayers = ConcurrentHashMap.newKeySet();
    private PacketListenerCommon packetListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        for (String value : getConfig().getStringList("muted-players")) {
            try {
                mutedPlayers.add(UUID.fromString(value));
            } catch (IllegalArgumentException exception) {
                getLogger().warning("Ignoring invalid UUID in muted-players: " + value);
            }
        }

        PluginCommand command = getCommand("withersounds");
        if (command == null) {
            getLogger().severe("Command registration failed; disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        command.setExecutor(this);
        command.setTabCompleter(this);

        // This packet generates the sound on the client. A stopSound packet
        // after the fact cannot reliably silence it.
        packetListener = PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract(PacketListenerPriority.HIGHEST) {
            @Override
            public void onPacketSend(PacketSendEvent event) {
                if (event.getPacketType() != PacketType.Play.Server.EFFECT
                        || !mutedPlayers.contains(event.getUser().getUUID())) {
                    return;
                }

                if (new WrapperPlayServerEffect(event).getType() == WITHER_SPAWN_EVENT) {
                    event.setCancelled(true);
                }
            }
        });

        getLogger().info("Ready. Players can use /withersounds off to mute Wither spawns.");
    }

    @Override
    public void onDisable() {
        saveMutedPlayers();
        if (packetListener != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(packetListener);
            packetListener = null;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is for players only.");
            return true;
        }

        if (args.length > 1) {
            player.sendMessage("§eUsage: /withersounds [on|off|toggle|status]");
            return true;
        }

        String action = args.length == 0 ? "toggle" : args[0].toLowerCase(java.util.Locale.ROOT);
        UUID uuid = player.getUniqueId();
        boolean muted = mutedPlayers.contains(uuid);

        switch (action) {
            case "on" -> muted = false;
            case "off" -> muted = true;
            case "toggle" -> muted = !muted;
            case "status" -> {
                player.sendMessage("§7Wither spawn sounds: " + (muted ? "§cOFF" : "§aON"));
                return true;
            }
            default -> {
                player.sendMessage("§eUsage: /withersounds [on|off|toggle|status]");
                return true;
            }
        }

        boolean changed = muted ? mutedPlayers.add(uuid) : mutedPlayers.remove(uuid);
        if (changed) {
            saveMutedPlayers();
        }
        player.sendMessage("§7Wither spawn sounds: " + (muted ? "§cOFF" : "§aON"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender,
                                      Command command,
                                      String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        List<String> matches = new ArrayList<>();
        for (String option : Arrays.asList("on", "off", "toggle", "status")) {
            if (option.startsWith(args[0].toLowerCase(java.util.Locale.ROOT))) {
                matches.add(option);
            }
        }
        return matches;
    }

    private void saveMutedPlayers() {
        List<String> ids = mutedPlayers.stream().map(UUID::toString).sorted().toList();
        getConfig().set("muted-players", ids);
        saveConfig();
    }
}
