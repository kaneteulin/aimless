/*
 * Aimless for Paper 26.3
 *
 * Original work: Aimless 0.1.2 (AimlessPlugin.kt) by Noonmaru (Nemo)
 *   https://github.com/noonmaru/aimless - GPL-3.0
 *
 * Modified 2026-11-20 by kaneteulin:
 *   - Rewritten from Kotlin to Java for Paper 26.3 (Kotlin / ProtocolLib dependencies removed)
 *   - Restarter leaves a restart marker so server.ps1 restarts the server
 *     (a manual "stop" fully shuts the server down)
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, version 3.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License (LICENSE.md) for more details.
 */
package com.github.noonmaru.aimless.plugin;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * @author Nemo (original), kaneteulin (Paper 26.3 port)
 */
public final class AimlessPlugin extends JavaPlugin {

    /**
     * server.ps1 이 이 파일을 보면 서버를 바로 다시 시작한다 (서버 폴더 기준).
     */
    public static final String RESTART_MARKER = "aimless-restart.flag";

    @Override
    public void onEnable() {
        // 이전 실행에서 남은 재시작 표시 제거
        new File(Bukkit.getWorldContainer(), RESTART_MARKER).delete();

        for (World world : Bukkit.getWorlds()) {
            // 26.x 에서 게임 규칙 상수가 GameRules 로 옮겨짐 (ANNOUNCE_ADVANCEMENTS -> SHOW_ADVANCEMENT_MESSAGES)
            world.setGameRule(GameRules.SHOW_ADVANCEMENT_MESSAGES, false);
            world.setGameRule(GameRules.SHOW_DEATH_MESSAGES, true);
            world.setGameRule(GameRules.SEND_COMMAND_FEEDBACK, false);
            world.setGameRule(GameRules.LOG_ADMIN_COMMANDS, false);
            world.setGameRule(GameRules.REDUCED_DEBUG_INFO, true);
        }

        World world = Bukkit.getWorlds().getFirst();
        WorldBorder border = world.getWorldBorder();
        border.setCenter(new Location(world, 0.0, 0.0, 0.0));
        border.setSize(16384.0);
        world.setSpawnLocation(world.getHighestBlockAt(0, 0).getLocation());

        getServer().getPluginManager().registerEvents(new EventListener(this), this);
        getServer().getScheduler().runTaskTimer(this, PlayerList.INSTANCE, 0L, 1L);
        getServer().getScheduler().runTaskTimer(this, new Restarter(this), 20L * 60L, 20L * 60L);
    }

    static final class Restarter implements Runnable {
        private final AimlessPlugin plugin;
        private final long time = System.currentTimeMillis();

        Restarter(AimlessPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public void run() {
            long elapsedTime = System.currentTimeMillis() - time;

            long restartTime = 1000L * 60L * 60L * 2L;

            if (elapsedTime >= restartTime) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage(Component.text("서버가 재시작됩니다."));
                }
                markRestart();
                Bukkit.shutdown();
            } else if (elapsedTime >= restartTime - 60000L) {
                Bukkit.broadcast(Component.text("1분 뒤 서버가 재시작됩니다."));
            }
        }

        private void markRestart() {
            try {
                new File(Bukkit.getWorldContainer(), RESTART_MARKER).createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create restart marker", e);
            }
        }
    }
}
