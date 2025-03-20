package dev.brighten.antivpn.bukkit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.api.APIPlayer;
import dev.brighten.antivpn.api.VPNExecutor;
import dev.brighten.antivpn.message.VpnString;
import dev.brighten.antivpn.web.objects.VPNResponse;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@SuppressWarnings("unchecked")
public class BukkitListener extends VPNExecutor implements Listener {
    private final Cache<UUID, VPNResponse> responseCache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(2000)
            .build();

    @Override
    public void registerListeners() {
        BukkitPlugin.pluginInstance.getServer().getPluginManager()
                .registerEvents(this, BukkitPlugin.pluginInstance);
    }

    @Override
    public void shutdown() {

    }

    @Override
    public void log(Level level, String log, Object... objects) {
        Bukkit.getLogger().log(level, String.format(log, objects));
    }

    @Override
    public void log(String log, Object... objects) {
        log(Level.INFO, String.format(log, objects));
    }

    @Override
    public void logException(String message, Exception ex) {
        Bukkit.getLogger().log(Level.SEVERE, message, ex);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(final PlayerJoinEvent event) {
        AntiVPN.getInstance().getPlayerExecutor().getPlayer(event.getPlayer().getUniqueId())
                .ifPresent(player -> AntiVPN.getInstance().getDatabase().alertsState(player.getUuid(), enabled -> {
                    if(enabled) {
                        player.setAlertsEnabled(true);
                        player.sendMessage(AntiVPN.getInstance().getMessageHandler()
                                .getString("command-alerts-toggled")
                                .getFormattedMessage(new VpnString.Var<>("state", true)));
                    }
                }));

        String address;

        if(event.getPlayer().getAddress() != null) {
            address = event.getPlayer().getAddress().getAddress().getHostAddress();
        } else {
            log(Level.WARNING, "Player %s address is null! This is a bug and should be reported!", event.getPlayer().getName());
            return;
        }

        if(event.getPlayer().hasPermission("antivpn.bypass") //Has bypass permission
                || AntiVPN.getInstance().getExecutor().isWhitelisted(event.getPlayer().getUniqueId()) //Is exempt
                //Or has a name that starts with a certain prefix. This is for Bedrock exempting.
                || AntiVPN.getInstance().getExecutor().isWhitelisted(address)
                || AntiVPN.getInstance().getVpnConfig().getPrefixWhitelists().stream()
                .anyMatch(prefix -> event.getPlayer().getName().startsWith(prefix))) return;

        if(responseCache.asMap().containsKey(event.getPlayer().getUniqueId())) {
            VPNResponse cached = responseCache.getIfPresent(event.getPlayer().getUniqueId());

            if (cached != null && cached.isProxy()) {
                proxyKick(event.getPlayer(), cached);
                return;
            }
        }

        final Player player = event.getPlayer();
        checkIp(address,
                AntiVPN.getInstance().getVpnConfig().cachedResults(), result -> {
                    if(result.isSuccess()) {
                        //We need to run on main thread or kicking and running commands will cause errors
                        //If the player is whitelisted, we don't want to kick them
                        if(AntiVPN.getInstance().getExecutor().isWhitelisted(event.getPlayer().getUniqueId())) {
                            log("UUID is whitelisted: %s", event.getPlayer().getUniqueId().toString());
                            return;
                        }

                        //If the IP is whitelisted, we don't want to kick them
                        if (AntiVPN.getInstance().getExecutor().isWhitelisted(address)) {
                            log("IP is whitelisted: %s",
                                    address);
                            return;
                        }

                        // If the countryList() size is zero, no need to check.
                        // Running country check first
                        if(!AntiVPN.getInstance().getVpnConfig().countryList().isEmpty()
                                // This bit of code will decide whether to kick the player
                                // If contains the code, and set to whitelist, it will not kick as they are equal
                                // and vise versa. However, if the contains does not match the state, it will kick.
                                && AntiVPN.getInstance().getVpnConfig().countryList()
                                .contains(result.getCountryCode())
                                != AntiVPN.getInstance().getVpnConfig().whitelistCountries()) {
                            countryKick(player, result);
                        } else if(result.isProxy()) {
                            proxyKick(player, result);
                        }
                    } else {
                        log(Level.WARNING,
                                "The API query was not a success! " +
                                        "You may need to upgrade your license on https://funkemunky.cc/shop");
                    }
                    AntiVPN.getInstance().checked++;
                });
    }

    private void countryKick(Player player, VPNResponse result) {
        final String kickReason = AntiVPN.getInstance().getVpnConfig()
                .countryVanillaKickReason();
        //Using our built-in kicking system if no commands are configured
        if(AntiVPN.getInstance().getVpnConfig().countryKickCommands().isEmpty()) {
            // Kicking our player
            new BukkitRunnable() {
                public void run() {
                    player.kickPlayer(ChatColor
                            .translateAlternateColorCodes('&',
                                    kickReason
                                            .replace("%player%", player.getName())
                                            .replace("%country%", result.getCountryName())
                                            .replace("%code%", result.getCountryCode())));
                }
            }.runTask(BukkitPlugin.pluginInstance);
        } else {
            final String playerName = player.getName();

            BukkitPlugin.pluginInstance.getPlayerCommandRunner()
                    .addAction(player.getUniqueId(), () -> {
                        for (String cmd : AntiVPN.getInstance().getVpnConfig().countryKickCommands()) {
                            final String formattedCommand = ChatColor.translateAlternateColorCodes('&',
                                    cmd.replace("%player%", playerName)
                                            .replace("%country%", result.getCountryName())
                                            .replace("%code%", result.getCountryCode()));

                            // Runs our command from console
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedCommand);
                        }
                    });
        }
    }

    private void proxyKick(Player player, VPNResponse result) {
        log(Level.INFO, player.getName()
                + " joined on a VPN/Proxy (" + result.getMethod() + ")");

        //Ensuring the user wishes to alert to staff
        if(AntiVPN.getInstance().getVpnConfig().alertToStaff())
            AntiVPN.getInstance().getPlayerExecutor().getOnlinePlayers().stream()
                    .filter(APIPlayer::isAlertsEnabled)
                    .forEach(pl -> pl.sendMessage(AntiVPN.getInstance().getVpnConfig().alertMessage()
                            .replace("%player%", player.getName())
                            .replace("%reason%", result.getMethod())
                            .replace("%country%", result.getCountryName())
                            .replace("%city%", result.getCity())));

        if(AntiVPN.getInstance().getVpnConfig().kickPlayersOnDetect()) {
            new BukkitRunnable() {
                public void run() {
                    player.kickPlayer(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                            AntiVPN.getInstance().getVpnConfig().getKickString()));
                }
            }.runTask(BukkitPlugin.pluginInstance);
        } else {
            //In case the user wants to run their own commands instead of using the built-in kicking
            if(AntiVPN.getInstance().getVpnConfig().runCommands()) {
                String playerName = player.getName();
                BukkitPlugin.pluginInstance.getPlayerCommandRunner()
                        .addAction(player.getUniqueId(), () -> {
                            for (String command : AntiVPN.getInstance().getVpnConfig().commands()) {
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                        ChatColor.translateAlternateColorCodes('&',
                                                command.replace("%player%",
                                                        playerName)));
                            }
                        });
            }
            AntiVPN.getInstance().detections++;
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        AntiVPN.getInstance().getPlayerExecutor().unloadPlayer(event.getPlayer().getUniqueId());
    }
}
