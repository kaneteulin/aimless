/*
 * Aimless for Paper 26.3
 *
 * Original work: Aimless 0.1.2 (Emote.kt) by Noonmaru (Nemo)
 *   https://github.com/noonmaru/aimless - GPL-3.0
 *
 * Modified 2026-11-20 by kaneteulin:
 *   - Rewritten from Kotlin to Java
 *   - Particle names updated for Paper 26.3 (VILLAGER_ANGRY -> ANGRY_VILLAGER,
 *     BARRIER -> BLOCK_MARKER with barrier block data)
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, version 3.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License (LICENSE.md) for more details.
 */
package com.github.noonmaru.aimless.plugin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

public final class Emote {
    private Emote() {
    }

    private static final Map<String, Consumer<Location>> emotes = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    private static final BlockData BARRIER = Material.BARRIER.createBlockData();

    static {
        register("angry", "화남", it ->
                it.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, it.getX(), it.getY() + 2.0, it.getZ(),
                        0, 0.0, 0.0, 0.0, 0.0, null, true));

        register("heart", "하트", it ->
                it.getWorld().spawnParticle(Particle.HEART, it.getX(), it.getY() + 2.0, it.getZ(),
                        0, 0.0, 0.0, 0.0, 0.0, null, true));

        // 원본 그대로 (의도): ㅗ / 망할 은 하트
        register("ㅗ", "망할", it ->
                it.getWorld().spawnParticle(Particle.HEART, it.getX(), it.getY() + 2.0, it.getZ(),
                        0, 0.0, 0.0, 0.0, 0.0, null, true));

        register("damage", "상처", it ->
                it.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, it.getX(), it.getY() + 2.0, it.getZ(),
                        0, 0.0, 0.0, 0.0, 0.0, null, true));

        register("critical", "많은상처", it ->
                it.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, it.getX(), it.getY() + 2.0, it.getZ(),
                        16, 0.5, 0.5, 0.5, 0.0, null, true));

        register("spit", "퉤", it -> {
            Vector v = it.getDirection();
            World world = it.getWorld();
            world.spawnParticle(Particle.SPIT, it.getX(), it.getY() + 1.62, it.getZ(),
                    0, v.getX(), v.getY(), v.getZ(), 1.0, null, true);

            world.playSound(it, Sound.ENTITY_LLAMA_SPIT, 1.0F, 1.0F);
        });

        register("no", "하지마", it -> {
            World world = it.getWorld();
            world.spawnParticle(Particle.BLOCK_MARKER, it.getX(), it.getY() + 2.5, it.getZ(),
                    0, 0.0, 0.0, 0.0, 0.0, BARRIER, true);

            world.playSound(it, Sound.BLOCK_ANVIL_LAND, 0.5F, 0.1F);
        });

        register("note", "즐", it ->
                it.getWorld().spawnParticle(Particle.NOTE, it.getX(), it.getY() + 2.0, it.getZ(),
                        0, 0.0, 0.0, 0.0, 1.0, null, true));

        register("rage", "열받", it -> {
            World world = it.getWorld();
            world.spawnParticle(Particle.LAVA, it.getX(), it.getY() + 2.0, it.getZ(),
                    128, 0.0, 0.0, 0.0, 1.0, null, true);

            world.playSound(it, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 1.2F);
        });

        register("tear", "눈물", it -> eyes(it, Particle.FALLING_WATER));
        register("lava", "피눈물", it -> eyes(it, Particle.FALLING_LAVA));
        register("honey", "눈꼽", it -> eyes(it, Particle.FALLING_HONEY));
    }

    private static void eyes(Location it, Particle particle) {
        for (int i = 0; i <= 1; i++) {
            Vector v = new Vector(-0.1 + i * 0.2, 0.1, 0.4);

            v.rotateAroundX(Math.toRadians(it.getPitch())).rotateAroundY(Math.toRadians(-it.getYaw()));

            it.getWorld().spawnParticle(particle, it.getX() + v.getX(), it.getY() + 1.3 + v.getY(), it.getZ() + v.getZ(),
                    0, 0.0, 0.0, 0.0, 1.0, null, true);
        }
    }

    private static void register(String name, String subname, Consumer<Location> emote) {
        emotes.put(name, emote);
        emotes.put(subname, emote);
    }

    public static Consumer<Location> emoteBy(String name) {
        return emotes.get(name);
    }
}
