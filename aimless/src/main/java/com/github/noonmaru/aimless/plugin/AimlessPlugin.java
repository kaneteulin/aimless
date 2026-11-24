/*
 * Aimless for Paper 26.3
 *
 * Original work: Aimless 0.1.2 (AimlessPlugin.kt) by Noonmaru (Nemo)
 *   https://github.com/noonmaru/aimless - GPL-3.0
 *
 * Modified 2026-11-20 by kaneteulin:
 *   - Rewritten from Kotlin to Java for Paper 26.3 (Kotlin / ProtocolLib dependencies removed)
 *   - Restarter leaves a restart marker so server.ps1 restarts the server
 *   - Game rule locatorBar disabled (no nearby player indicators)
 *   - Paper Anti-Xray settings written on enable (one automatic restart when they change)
 *   - World border enlarged from 16384 to 32768
 *   - Works without server.ps1: restarts itself (new server window on Windows) and
 *     downloads updates from GitHub Releases into Paper's update folder
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

    @Override
    public void onEnable() {
        // 이전 실행에서 남은 재시작 표시 제거
        new File(Bukkit.getWorldContainer(), ServerRestart.RESTART_MARKER).delete();

        for (World world : Bukkit.getWorlds()) {
            // 26.x 에서 게임 규칙 상수가 GameRules 로 옮겨짐 (ANNOUNCE_ADVANCEMENTS -> SHOW_ADVANCEMENT_MESSAGES)
            world.setGameRule(GameRules.SHOW_ADVANCEMENT_MESSAGES, false);
            world.setGameRule(GameRules.SHOW_DEATH_MESSAGES, true);
            world.setGameRule(GameRules.SEND_COMMAND_FEEDBACK, false);
            world.setGameRule(GameRules.LOG_ADMIN_COMMANDS, false);
            world.setGameRule(GameRules.REDUCED_DEBUG_INFO, true);
            // 바닐라 기본값 (모두 자면 밤이 넘어감). 이전 버전에서 저장된 101 을 되돌림
            world.setGameRule(GameRules.PLAYERS_SLEEPING_PERCENTAGE, 100);
            // 경험치 바 위의 위치 표시 바(근처 플레이어 방향) 끄기
            world.setGameRule(GameRules.LOCATOR_BAR, false);
        }

        World world = Bukkit.getWorlds().getFirst();
        WorldBorder border = world.getWorldBorder();
        border.setCenter(new Location(world, 0.0, 0.0, 0.0));
        border.setSize(32768.0); // 원본: 16384 (2배로 확장)
        world.setSpawnLocation(world.getHighestBlockAt(0, 0).getLocation());

        PlayerList.loadSkins(new File(getDataFolder(), "skins.yml"));

        getServer().getPluginManager().registerEvents(new EventListener(this), this);
        getServer().getScheduler().runTaskTimer(this, PlayerList.INSTANCE, 0L, 1L);
        getServer().getScheduler().runTaskTimer(this, new Restarter(this), 20L * 60L, 20L * 60L);

        // server.ps1 없이 실행한 경우: 플러그인이 직접 GitHub Releases 에서 업데이트를 받는다
        if (!ServerRestart.isManagedByScript()) {
            String version = getPluginMeta().getVersion();
            String jarName = getFile().getName();
            getServer().getScheduler().runTaskAsynchronously(this, () -> SelfUpdater.check(version, jarName, getLogger()));
        }

        // Anti-Xray 설정이 새로 바뀌었으면 적용을 위해 바로 한 번 재시작 (처음 한 번만)
        if (AntiXray.apply(getLogger())) {
            if (ServerRestart.canRestart()) {
                getLogger().info("Anti-Xray 설정 적용을 위해 서버를 재시작합니다.");
                getServer().getScheduler().runTask(this, () -> ServerRestart.restart(getLogger()));
            } else {
                getLogger().info("Anti-Xray 설정은 다음 서버 시작부터 적용됩니다.");
            }
        }
    }

    static final class Restarter implements Runnable {
        private final AimlessPlugin plugin;
        private final long time = System.currentTimeMillis();
        private boolean restarting = false;

        Restarter(AimlessPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public void run() {
            long elapsedTime = System.currentTimeMillis() - time;

            long restartTime = 1000L * 60L * 60L * 2L;

            // 다시 켤 방법이 없는 환경이면 재시작하지 않음 (서버가 꺼진 채로 남지 않도록)
            if (restarting || !ServerRestart.canRestart()) {
                return;
            }

            if (elapsedTime >= restartTime) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage(Component.text("서버가 재시작됩니다."));
                }
                restarting = ServerRestart.restart(plugin.getLogger());
            } else if (elapsedTime >= restartTime - 60000L) {
                Bukkit.broadcast(Component.text("1분 뒤 서버가 재시작됩니다."));
            }
        }
    }
}
