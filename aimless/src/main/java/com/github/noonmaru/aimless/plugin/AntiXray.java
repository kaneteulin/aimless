/*
 * Aimless for Paper 26.3
 *
 * Added 2026-11-22 by kaneteulin (not part of the original Aimless 0.1.2 by Noonmaru,
 * https://github.com/noonmaru/aimless - GPL-3.0).
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, version 3.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License (LICENSE.md) for more details.
 */
package com.github.noonmaru.aimless.plugin;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Paper 내장 Anti-Xray 설정을 서버 설정 파일에 적용한다.
 * <p>
 * Paper 는 이 설정을 서버가 켜질 때(플러그인보다 먼저) 읽으므로, 파일을 바꾼 경우 다음 시작부터 적용된다.
 */
final class AntiXray {

    private static final String PATH = "anticheat.anti-xray.";

    private AntiXray() {
    }

    /**
     * @return 설정 파일을 바꿨으면 true (적용하려면 재시작 필요)
     */
    static boolean apply(Logger logger) {
        boolean changed = false;

        // 기본값 (오버월드): 광석과 동굴(air)을 가짜 블록으로 숨김
        Map<String, Object> overworld = new LinkedHashMap<>();
        overworld.put("enabled", true);
        overworld.put("engine-mode", 2);
        overworld.put("hidden-blocks", List.of(
                "air",
                "copper_ore", "deepslate_copper_ore", "raw_copper_block",
                "diamond_ore", "deepslate_diamond_ore",
                "gold_ore", "deepslate_gold_ore",
                "iron_ore", "deepslate_iron_ore", "raw_iron_block",
                "lapis_ore", "deepslate_lapis_ore",
                "redstone_ore", "deepslate_redstone_ore"));
        overworld.put("lava-obscures", false);
        overworld.put("max-block-height", 64);
        overworld.put("replacement-blocks", List.of(
                "chest", "amethyst_block", "andesite", "budding_amethyst", "calcite",
                "coal_ore", "deepslate_coal_ore", "deepslate", "diorite", "dirt",
                "emerald_ore", "deepslate_emerald_ore", "granite", "gravel",
                "oak_planks", "smooth_basalt", "stone", "tuff"));
        overworld.put("update-radius", 2);
        overworld.put("use-permission", false);
        changed |= write(new File(Bukkit.getWorldContainer(), "config/paper-world-defaults.yml"), overworld, logger);

        for (World world : Bukkit.getWorlds()) {
            Map<String, Object> values = new LinkedHashMap<>();

            switch (world.getEnvironment()) {
                case NETHER -> {
                    // 고대 잔해, 네더 광석
                    values.put("enabled", true);
                    values.put("engine-mode", 2);
                    values.put("hidden-blocks", List.of("ancient_debris", "nether_gold_ore", "nether_quartz_ore"));
                    values.put("max-block-height", 128);
                    values.put("replacement-blocks", List.of(
                            "basalt", "blackstone", "gravel", "netherrack", "soul_sand", "soul_soil"));
                }
                case THE_END -> values.put("enabled", false); // 엔드에는 숨길 광석이 없음
                default -> {
                    continue;
                }
            }

            changed |= write(new File(world.getWorldFolder(), "paper-world.yml"), values, logger);
        }

        return changed;
    }

    private static boolean write(File file, Map<String, Object> values, Logger logger) {
        if (!file.isFile()) {
            logger.warning("Anti-Xray: " + file + " 파일이 없어 건너뜁니다.");
            return false;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        boolean changed = false;

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String key = PATH + entry.getKey();
            if (!Objects.equals(yaml.get(key), entry.getValue())) {
                yaml.set(key, entry.getValue());
                changed = true;
            }
        }

        if (!changed) {
            return false;
        }

        try {
            yaml.save(file);
            logger.info("Anti-Xray 설정을 적용했습니다: " + file);
            return true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Anti-Xray: " + file + " 저장 실패", e);
            return false;
        }
    }
}
