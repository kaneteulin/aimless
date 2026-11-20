/*
 * Aimless for Paper 26.3
 *
 * Original work: Aimless 0.1.2 (PlayerList.kt) by Noonmaru (Nemo)
 *   https://github.com/noonmaru/aimless - GPL-3.0
 *
 * Modified 2026-11-20 by kaneteulin:
 *   - Rewritten from Kotlin to Java
 *   - Builds the 26.3 player info update packet directly (ProtocolLib dependency removed;
 *     the packet format changed in 1.19.3). Game mode and latency of every entry are
 *     overwritten like the original ADD_PLAYER did.
 *   - Every entry is sent as unlisted, so the Tab list shows nobody (no player faces)
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, version 3.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License (LICENSE.md) for more details.
 */
package com.github.noonmaru.aimless.plugin;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.world.level.GameType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * 접속한 적 있는 모든 플레이어(오프라인 포함)의 정보를 보내되 Tab 목록에서는 모두 숨긴다.
 * (원본은 모두 Tab 목록에 표시해 누가 접속 중인지 알 수 없게 했다)
 */
public final class PlayerList implements Runnable {

    public static final PlayerList INSTANCE = new PlayerList();

    private static boolean update = false;

    private PlayerList() {
    }

    public static void update() {
        update = true;
    }

    @Override
    public void run() {
        if (update) {
            update = false;
            updatePlayerList();
        }
    }

    private static void updatePlayerList() {
        List<ClientboundPlayerInfoUpdatePacket.Entry> list = new ArrayList<>();

        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            String name = offlinePlayer.getName();
            if (name == null) {
                continue;
            }

            GameProfile profile = offlinePlayer instanceof Player player
                    ? ((CraftPlayer) player).getHandle().getGameProfile()
                    : new GameProfile(offlinePlayer.getUniqueId(), name);

            list.add(new ClientboundPlayerInfoUpdatePacket.Entry(
                    offlinePlayer.getUniqueId(),
                    profile,
                    false,              // listed: Tab 목록에 아무도 표시하지 않음 (얼굴 숨김, 원본은 true)
                    0,                  // latency
                    GameType.SURVIVAL,  // 원본: NOT_SET

                    Component.literal(name),
                    true,               // show hat
                    0,                  // list order
                    null                // chat session
            ));
        }

        // 원본(1.16)의 ADD_PLAYER 는 접속 중인 플레이어 항목까지 통째로 교체해 게임 모드(NOT_SET)와 핑(0)을 덮어썼다.
        // 26.3 에서는 이미 있는 항목에 ADD_PLAYER 가 무시되므로, 게임 모드/핑 갱신을 함께 보내 원본과 같게 맞춘다.
        // (NOT_SET 은 26.3 에 없으며, 클라이언트는 알 수 없는 게임 모드를 서바이벌로 처리하므로 SURVIVAL 과 같다)
        ClientboundPlayerInfoUpdatePacket packet = new ClientboundPlayerInfoUpdatePacket(
                EnumSet.of(
                        ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
                        ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE,
                        ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY,
                        ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED,
                        ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME
                ),
                list
        );

        for (Player player : Bukkit.getOnlinePlayers()) {
            ((CraftPlayer) player).getHandle().connection.send(packet);
        }
    }
}
