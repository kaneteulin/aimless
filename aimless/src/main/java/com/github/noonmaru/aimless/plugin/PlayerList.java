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
 *   - A joining player's offline entry is removed first so their real skin is applied
 *   - Offline entries keep the player's last seen skin (saved in plugins/Aimless/skins.yml)
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, version 3.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License (LICENSE.md) for more details.
 */
package com.github.noonmaru.aimless.plugin;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.world.level.GameType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * 접속한 적 있는 모든 플레이어(오프라인 포함)의 정보를 보내되 Tab 목록에서는 모두 숨긴다.
 * (원본은 모두 Tab 목록에 표시해 누가 접속 중인지 알 수 없게 했다)
 */
public final class PlayerList implements Runnable {

    public static final PlayerList INSTANCE = new PlayerList();

    private static final String TEXTURES = "textures";

    private static boolean update = false;

    private PlayerList() {
    }

    public static void update() {
        update = true;
    }

    /**
     * 접속한 플레이어의 항목을 다른 플레이어들에게서 지운다.
     * <p>
     * 오프라인일 때 보낸 항목(스킨 없음)이 남아 있으면, 26.3 클라이언트는 이미 있는 항목에 대한 ADD_PLAYER 를 무시하므로
     * 서버가 보내는 실제 스킨 정보가 적용되지 않아 기본 스킨으로 보인다.
     * PlayerJoinEvent 는 서버가 새 플레이어 정보를 다른 플레이어들에게 보내기 전에 호출되므로, 여기서 지우면
     * 바로 뒤에 실제 스킨이 담긴 항목이 새로 추가된다.
     */
    public static void forget(Player joined) {
        ClientboundPlayerInfoRemovePacket packet = new ClientboundPlayerInfoRemovePacket(List.of(joined.getUniqueId()));

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player != joined) {
                ((CraftPlayer) player).getHandle().connection.send(packet);
            }
        }
    }

    /**
     * 플레이어가 마지막으로 접속했을 때의 스킨(Mojang 서명 포함)을 기억해,
     * 오프라인일 때도 같은 얼굴로 보이게 한다 (얼굴로 접속 여부를 구분할 수 없게).
     */
    private static final Map<UUID, Property> skins = new HashMap<>();
    private static File skinsFile;

    public static void loadSkins(File file) {
        skinsFile = file;
        skins.clear();

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            ConfigurationSection section = yaml.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            String value = section.getString("value");
            if (value == null) {
                continue;
            }
            try {
                skins.put(UUID.fromString(key), new Property(TEXTURES, value, section.getString("signature")));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    /** 접속한 플레이어의 현재 스킨을 기억하고 파일에 저장한다. */
    public static void rememberSkin(Player player) {
        Collection<Property> textures = ((CraftPlayer) player).getHandle().getGameProfile().properties().get(TEXTURES);
        if (textures.isEmpty()) {
            return;
        }

        Property property = textures.iterator().next();
        if (property.equals(skins.put(player.getUniqueId(), property))) {
            return; // 바뀐 것 없음
        }

        YamlConfiguration yaml = new YamlConfiguration();
        skins.forEach((uuid, skin) -> {
            yaml.set(uuid + ".value", skin.value());
            yaml.set(uuid + ".signature", skin.signature());
        });
        try {
            yaml.save(skinsFile);
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.WARNING, "[Aimless] Failed to save " + skinsFile, e);
        }
    }

    private static GameProfile offlineProfile(UUID uuid, String name) {
        Property skin = skins.get(uuid);
        if (skin == null) {
            return new GameProfile(uuid, name); // 이 기능이 생기기 전에만 접속한 플레이어: 기본 스킨
        }
        return new GameProfile(uuid, name, new PropertyMap(ImmutableMultimap.of(TEXTURES, skin)));
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
                    : offlineProfile(offlinePlayer.getUniqueId(), name);

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
