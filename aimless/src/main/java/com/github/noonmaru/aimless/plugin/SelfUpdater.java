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

import org.bukkit.Bukkit;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub Releases 에 새 버전이 있으면 내려받아 Paper 의 update 폴더에 넣는다.
 * Paper 가 다음 서버 시작(자동 재시작 포함) 때 플러그인을 새 버전으로 바꾼다.
 * (server.ps1 로 실행하면 server.ps1 이 업데이트를 맡으므로 쓰지 않는다)
 */
final class SelfUpdater {

    private static final String REPO = "kaneteulin/aimless";
    private static final Pattern TAG = Pattern.compile("\"tag_name\"\\s*:\\s*\"v?([0-9.]+)\"");

    private SelfUpdater() {
    }

    static void check(String currentVersion, String jarName, Logger logger) {
        int[] current = parse(currentVersion);
        if (current == null) {
            logger.info("개발 빌드(" + currentVersion + ")라 자동 업데이트를 건너뜁니다.");
            return;
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> release = client.send(
                    HttpRequest.newBuilder(URI.create("https://api.github.com/repos/" + REPO + "/releases/latest"))
                            .header("User-Agent", "Aimless-Updater")
                            .header("Accept", "application/vnd.github+json")
                            .timeout(Duration.ofSeconds(20))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());

            Matcher matcher = TAG.matcher(release.body());
            if (release.statusCode() != 200 || !matcher.find()) {
                logger.warning("자동 업데이트: 최신 버전을 확인하지 못했습니다 (HTTP " + release.statusCode() + ")");
                return;
            }

            String latestVersion = matcher.group(1);
            int[] latest = parse(latestVersion);
            if (latest == null || compare(latest, current) <= 0) {
                return; // 최신 버전
            }

            File updateFolder = Bukkit.getUpdateFolderFile();
            updateFolder.mkdirs();
            Path temp = new File(updateFolder, jarName + ".download").toPath();

            HttpResponse<Path> download = client.send(
                    HttpRequest.newBuilder(URI.create("https://github.com/" + REPO + "/releases/download/v" + latestVersion + "/Aimless.jar"))
                            .header("User-Agent", "Aimless-Updater")
                            .timeout(Duration.ofMinutes(2))
                            .build(),
                    HttpResponse.BodyHandlers.ofFile(temp));

            if (download.statusCode() != 200 || Files.size(temp) == 0) {
                Files.deleteIfExists(temp);
                logger.warning("자동 업데이트: 내려받기 실패 (HTTP " + download.statusCode() + ")");
                return;
            }

            Files.move(temp, new File(updateFolder, jarName).toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.info("새 버전 " + latestVersion + " 을(를) 내려받았습니다. 다음 서버 시작(자동 재시작 포함) 때 적용됩니다.");
        } catch (Exception e) {
            logger.warning("자동 업데이트 확인 실패: " + e);
        }
    }

    /** "0.2.7" / "v0.2.7" -> {0, 2, 7}, 숫자 버전이 아니면 null */
    private static int[] parse(String version) {
        if (version == null) {
            return null;
        }
        String v = version.startsWith("v") ? version.substring(1) : version;
        if (!v.matches("[0-9]+(\\.[0-9]+)*")) {
            return null;
        }
        String[] parts = v.split("\\.");
        int[] numbers = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            numbers[i] = Integer.parseInt(parts[i]);
        }
        return numbers;
    }

    private static int compare(int[] a, int[] b) {
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = i < a.length ? a[i] : 0;
            int y = i < b.length ? b[i] : 0;
            if (x != y) {
                return Integer.compare(x, y);
            }
        }
        return 0;
    }
}
