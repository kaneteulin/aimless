/*
 * Aimless for Paper 26.3
 *
 * Added 2026-11-24 by kaneteulin (not part of the original Aimless 0.1.2 by Noonmaru,
 * https://github.com/noonmaru/aimless - GPL-3.0).
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, version 3.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License (LICENSE.md) for more details.
 */
package com.github.noonmaru.aimless.plugin;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

/**
 * 침대에서 잘 때 모두에게 뜨는 "n/m 명이 자는 중" 메시지(접속 인원이 드러남)를 보내지 않는다.
 * 잠 자체(모두 자면 밤이 넘어가는 것)는 그대로 작동한다.
 */
@ChannelHandler.Sharable
final class SleepMessageFilter extends ChannelOutboundHandlerAdapter {

    private static final SleepMessageFilter INSTANCE = new SleepMessageFilter();
    private static final String NAME = "aimless_sleep_filter";
    private static final String SLEEP_KEY = "sleep.players_sleeping";

    private SleepMessageFilter() {
    }

    static void inject(Player player) {
        Channel channel = ((CraftPlayer) player).getHandle().connection.connection.channel;
        channel.eventLoop().execute(() -> {
            if (channel.pipeline().get(NAME) == null && channel.pipeline().get("packet_handler") != null) {
                channel.pipeline().addBefore("packet_handler", NAME, INSTANCE);
            }
        });
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ClientboundSystemChatPacket packet
                && packet.overlay()
                && packet.content() != null
                && packet.content().getContents() instanceof TranslatableContents contents
                && SLEEP_KEY.equals(contents.getKey())) {
            promise.setSuccess(); // 보내지 않음
            return;
        }
        super.write(ctx, msg, promise);
    }
}
