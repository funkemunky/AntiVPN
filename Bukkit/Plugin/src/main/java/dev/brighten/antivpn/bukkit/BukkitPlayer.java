/*
 * Copyright 2026 Dawson Hessler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.brighten.antivpn.bukkit;

import dev.brighten.antivpn.api.APIPlayer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class BukkitPlayer extends APIPlayer {

    private final Player player;
    public BukkitPlayer(Player player) {
        super(player.getUniqueId(), player.getName(), player.getAddress() != null ? player.getAddress().getAddress() : null);

        this.player = player;
    }

    @Override
    public void sendMessage(String message) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    @Override
    public void kickPlayer(String reason) {
        new BukkitRunnable() {
            public void run() {
                player.kickPlayer(ChatColor.translateAlternateColorCodes('&', reason));
            }
        }.runTask(BukkitPlugin.pluginInstance.getPlugin());
    }

    @Override
    public boolean hasPermission(String permission) {
        return player.hasPermission(permission);
    }
}
