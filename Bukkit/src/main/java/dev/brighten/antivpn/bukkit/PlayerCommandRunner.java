package dev.brighten.antivpn.bukkit;

import dev.brighten.antivpn.utils.MiscUtils;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PlayerCommandRunner {
    private final ScheduledExecutorService executorService;
    private final Queue<PlayerAction> playerActions = new ArrayBlockingQueue<>(10000);

    public PlayerCommandRunner() {
        executorService = Executors.newSingleThreadScheduledExecutor(
                MiscUtils.createThreadFactory("AntiVPN:PlayerCommandRunner")
        );
    }

    void start() {
        executorService.scheduleAtFixedRate(() -> {
            long currentTime = System.currentTimeMillis();
            while(!playerActions.isEmpty()) {
                PlayerAction action = playerActions.peek();

                if(action == null) continue;

                if(currentTime - action.start > 2000L || Bukkit.getPlayer(action.getUuid()) != null) {
                    new BukkitRunnable() {
                        public void run() {
                            action.getAction().run();
                        }
                    }.runTask(BukkitBootstrap.pluginInstance);

                    playerActions.poll();
                }
            }
        }, 1000, 100, TimeUnit.MILLISECONDS);
    }

    void stop() {
        executorService.shutdown();
        playerActions.clear();
    }

    void addAction(UUID uuid, Runnable action) {
        playerActions.add(new PlayerAction(uuid, System.currentTimeMillis(), action));
    }

    @Data
    static class PlayerAction {
        private final UUID uuid;
        private final long start;
        private final Runnable action;
    }
}
