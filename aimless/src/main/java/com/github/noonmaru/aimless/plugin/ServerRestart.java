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
import org.spigotmc.RestartCommand;
import org.spigotmc.SpigotConfig;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 서버 재시작. 실행 방식에 따라 알맞은 방법을 고른다.
 * <ol>
 *     <li>server.ps1 로 실행한 경우: 재시작 표시 파일을 남기고 종료 → server.ps1 이 다시 켠다</li>
 *     <li>Windows 에서 그 밖의 방법(bat 파일 등)으로 실행한 경우: 같은 명령으로 새 서버 창을 띄운 뒤 종료</li>
 *     <li>그 밖의 OS: spigot.yml 의 restart-script 가 있으면 그것으로 재시작</li>
 *     <li>어느 것도 안 되면: 재시작하지 않음 (서버가 꺼진 채로 남지 않도록)</li>
 * </ol>
 */
final class ServerRestart {

    /** server.ps1 이 이 파일을 보면 서버를 바로 다시 시작한다 (서버 폴더 기준). */
    static final String RESTART_MARKER = "aimless-restart.flag";

    private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");

    private ServerRestart() {
    }

    /**
     * server.ps1 (PowerShell) 이 java 를 실행했는지.
     * Windows 의 PATH 에 있는 java.exe 는 진짜 java 를 띄우는 실행기인 경우가 있어
     * (Oracle javapath), 부모가 java.exe 이면 한 단계 더 올라가서 확인한다.
     */
    static boolean isManagedByScript() {
        java.util.Optional<ProcessHandle> handle = ProcessHandle.current().parent();
        for (int depth = 0; depth < 4 && handle.isPresent(); depth++) {
            String command = handle.get().info().command().orElse("");
            String name = new File(command).getName().toLowerCase(Locale.ROOT);
            if (name.equals("powershell.exe") || name.equals("pwsh.exe") || name.equals("pwsh")) {
                return true;
            }
            if (!name.equals("java.exe") && !name.equals("javaw.exe") && !name.equals("java")) {
                return false;
            }
            handle = handle.get().parent();
        }
        return false;
    }

    private static boolean hasSpigotRestartScript() {
        String script = SpigotConfig.restartScript;
        return script != null && !script.isBlank() && new File(script.trim().split(" ")[0]).isFile();
    }

    /** 이 환경에서 서버를 다시 켤 수 있는지 */
    static boolean canRestart() {
        return isManagedByScript() || WINDOWS || hasSpigotRestartScript();
    }

    /**
     * @return 재시작을 시작했으면 true
     */
    static boolean restart(Logger logger) {
        if (isManagedByScript()) {
            try {
                new File(Bukkit.getWorldContainer(), RESTART_MARKER).createNewFile();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to create restart marker", e);
            }
            Bukkit.shutdown();
            return true;
        }

        if (WINDOWS) {
            try {
                File script = writeRelaunchScript();
                // 이 서버가 완전히 꺼진 뒤(JVM 종료 시) 새 창에서 다시 실행
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        new ProcessBuilder("cmd", "/c", "start", "\"Aimless Server\"", script.getAbsolutePath())
                                .directory(new File(".").getAbsoluteFile())
                                .start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }, "Aimless-Relaunch"));
                Bukkit.shutdown();
                return true;
            } catch (IOException e) {
                logger.log(Level.SEVERE, "재시작 스크립트를 만들지 못해 재시작하지 않습니다.", e);
                return false;
            }
        }

        if (hasSpigotRestartScript()) {
            RestartCommand.restart();
            return true;
        }

        logger.warning("이 환경에서는 서버를 자동으로 다시 켤 수 없어 재시작하지 않습니다. "
                + "(server.ps1 을 쓰거나 spigot.yml 의 restart-script 를 설정하세요)");
        return false;
    }

    /** 지금 서버를 실행한 것과 같은 명령(JVM 옵션, jar, 인자)으로 다시 실행하는 bat 파일 */
    private static File writeRelaunchScript() throws IOException {
        String java = ProcessHandle.current().info().command().orElse("java");
        List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        String command = System.getProperty("sun.java.command", "paper.jar --nogui"); // 예: "paper.jar --nogui"

        StringBuilder line = new StringBuilder(quote(java));
        for (String arg : jvmArgs) {
            line.append(' ').append(quote(arg));
        }
        String first = command.split(" ")[0];
        if (first.toLowerCase(Locale.ROOT).endsWith(".jar")) {
            line.append(" -jar ").append(command);
        } else {
            line.append(" -cp ").append(quote(System.getProperty("java.class.path"))).append(' ').append(command);
        }

        File dir = new File(".").getAbsoluteFile().getParentFile();
        String bat = "@echo off\r\n"
                + "cd /d \"" + dir.getAbsolutePath() + "\"\r\n"
                + "rem 이전 서버가 완전히 종료될 때까지 잠시 대기\r\n"
                + "timeout /t 5 /nobreak >nul\r\n"
                + line + "\r\n";

        File file = new File(Bukkit.getPluginsFolder(), "Aimless/restart.bat");
        file.getParentFile().mkdirs();
        // cmd 는 bat 파일을 시스템 코드 페이지로 읽으므로 (한글 경로 대비) 같은 인코딩으로 저장
        Charset charset = Charset.forName(System.getProperty("sun.jnu.encoding", Charset.defaultCharset().name()));
        Files.writeString(file.toPath(), bat, charset);
        return file;
    }

    private static String quote(String arg) {
        return arg.contains(" ") || arg.contains("&") || arg.contains("(") ? "\"" + arg + "\"" : arg;
    }
}
