/*
 * Aimless for Paper 26.3
 *
 * Original work: Aimless 0.1.2 (EventListener.kt) by Noonmaru (Nemo)
 *   https://github.com/noonmaru/aimless - GPL-3.0
 *
 * Modified 2026-11-20 by kaneteulin:
 *   - Rewritten from Kotlin to Java, migrated to Paper 26.3 events / Adventure components
 *   - Sign/book filter now replaces any non-blank text (letters of every script, digits,
 *     symbols) with "?????" regardless of its length
 *   - Server list MOTD year changed to 2026
 *   - Emotes triggered from chat run on the main thread
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, version 3.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License (LICENSE.md) for more details.
 */
package com.github.noonmaru.aimless.plugin;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.event.player.PlayerServerFullCheckEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.server.TabCompleteEvent;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.util.NumberConversions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public final class EventListener implements Listener {

    private final AimlessPlugin plugin;

    EventListener(AimlessPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onServerFullCheck(PlayerServerFullCheckEvent event) {
        // 원본: PlayerLoginEvent 에서 KICK_FULL 이면 allow()
        event.allow(true);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.joinMessage(null);

        PlayerList.update();

        Player player = event.getPlayer();

        if (!player.hasPlayedBefore()) {
            player.teleport(getSpawnLocation(player.getName()));
        }

        player.setCompassTarget(Bukkit.getWorlds().getFirst().getSpawnLocation());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.quitMessage(null);

        PlayerList.update();
    }

    @EventHandler(ignoreCancelled = true)
    public void onTabComplete(TabCompleteEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        event.setCancelled(true);

        String message = event.getMessage();
        if (message.startsWith("/")) {
            message = message.substring(1);
        }

        Consumer<Location> emote = Emote.emoteBy(message);
        if (emote != null) {
            emote.accept(event.getPlayer().getLocation());
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.deathMessage(Component.text("사람이 죽었다.", NamedTextColor.RED));
    }

    @EventHandler(ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        event.setCancelled(true);

        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        Consumer<Location> emote = Emote.emoteBy(message);

        if (emote != null) {
            Player player = event.getPlayer();

            // 파티클/소리는 메인 스레드에서
            Bukkit.getScheduler().runTask(plugin, () -> emote.accept(player.getLocation()));

            player.sendMessage(Component.text("[" + message + "]", NamedTextColor.RED)
                    .clickEvent(ClickEvent.runCommand("/" + message)));
        }
    }

    @EventHandler
    public void onPaperServerListPing(PaperServerListPingEvent event) {
        Calendar c = Calendar.getInstance();

        event.setNumPlayers(c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH));
        event.setMaxPlayers(c.get(Calendar.HOUR) * 10000 + c.get(Calendar.MINUTE) * 100 + c.get(Calendar.SECOND));
        event.motd(Component.text("AIMLESS SERVER 2026")
                .color(TextColor.color(ThreadLocalRandom.current().nextInt(0xFFFFFF)))
                .decorate(TextDecoration.BOLD));
        event.getListedPlayers().clear();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerSign(SignChangeEvent event) {
        Block block = event.getBlock();
        int x = block.getX();
        int z = block.getZ();

        if (!(isInSpawn(x) && isInSpawn(z))) {
            for (int i = 0; i < event.lines().size(); i++) {
                Component line = event.line(i);
                if (line != null) {
                    event.line(i, Component.text(removeLang(plain(line))));
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerBookEdit(PlayerEditBookEvent event) {
        Location loc = event.getPlayer().getLocation();
        int x = loc.getBlockX();
        int z = loc.getBlockZ();

        if (!(isInSpawn(x) && isInSpawn(z))) {
            BookMeta meta = event.getNewBookMeta();

            List<Component> pages = new ArrayList<>();
            for (Component page : meta.pages()) {
                pages.add(Component.text(removeLang(plain(page))));
            }
            meta.pages(pages);

            Component title = meta.title();
            if (title != null) {
                meta.title(Component.text(removeLang(plain(title))));
            }

            event.setNewBookMeta(meta);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (event.isBedSpawn() || event.isAnchorSpawn()) return;

        event.setRespawnLocation(getSpawnLocation(event.getPlayer().getName()));
    }

    private static Location getSpawnLocation(String name) {
        int seed = name.hashCode();
        Random random = new Random(((long) seed) ^ 0x19940423L);
        World world = Bukkit.getWorlds().getFirst();
        WorldBorder border = world.getWorldBorder();
        double size = border.getSize() / 2.0;

        double x = random.nextDouble() * size - size / 2.0;
        double z = random.nextDouble() * size - size / 2.0;
        Block block = world.getHighestBlockAt(NumberConversions.floor(x), NumberConversions.floor(z));

        return block.getLocation().add(0.5, 1.0, 0.5);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerKick(PlayerKickEvent event) {
        event.setCancelled(true);
    }

    // ---------------------------------------------------------------- text filter

    private static boolean isInSpawn(int v) {
        return v >= -16 && v <= 15;
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static final String MASK = "?????";

    /**
     * 내용이 있으면 글자/숫자/기호 구분 없이, 길이와 상관없이 "?????" 로 바꾼다.
     * 빈 줄(공백만 있는 줄 포함)은 그대로 둔다.
     * <p>
     * 원본은 [a-zA-Z] 와 [ㄱ-힣] 만 한 글자씩 ? 로 바꿔 숫자, 기호, 다른 언어가 그대로 쓰였고
     * 글자 수로 내용을 짐작할 수도 있었다.
     */
    static String removeLang(String text) {
        return text.isBlank() ? text : MASK;
    }
}
