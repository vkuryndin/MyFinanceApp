package org.example.app;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Интеграционные тесты Main в отдельном процессе, с прикрученным JaCoCo-агентом.
 * - Никаких проблем "Scanner closed" (это внутри дочернего JVM).
 * - Покрытие Main считается (javaagent подвешен).
 */
public class MainAppIT {

    private static String runApp(Path workDir, String stdin, long timeoutSec) throws Exception {
        // Путь к java
        String javaHome = System.getProperty("java.home");
        Path javaExe = Paths.get(javaHome, "bin", "java");
        String classpath = System.getProperty("java.class.path");

        // Путь к агенту и итоговому .exec — Gradle прокинул их в system properties (см. build.gradle.kts)
        String agentJar = System.getProperty("jacoco.agent.path");
        String destFile = System.getProperty("jacoco.agent.destfile");
        if (agentJar == null || destFile == null) {
            throw new IllegalStateException("Jacoco agent props are missing. Check build.gradle.kts tasks.test doFirst{}");
        }

        // Собираем команду
        List<String> cmd = new ArrayList<>();
        cmd.add(javaExe.toString());
        cmd.add("-cp"); cmd.add(classpath);
        cmd.add("-javaagent:" + agentJar + "=destfile=" + destFile + ",append=true,includes=org.example.*");
        cmd.add("org.example.app.Main");

        // Рабочая директория — temp; подготовим data/
        Files.createDirectories(workDir.resolve("data"));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(workDir.toFile());
        pb.redirectErrorStream(true);

        Process p = pb.start();

        // Передаём ввод
        try (BufferedWriter w = new BufferedWriter(
                new OutputStreamWriter(p.getOutputStream(), StandardCharsets.UTF_8))) {
            if (stdin != null) {
                w.write(stdin);
            }
        }

        boolean finished = p.waitFor(timeoutSec, TimeUnit.SECONDS);
        if (!finished) {
            p.destroyForcibly();
            throw new RuntimeException("Main did not finish within timeout");
        }

        // Читаем stdout
        try (InputStream in = p.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("Первый запуск → Exit: создаётся data/finance-data.json, есть вывод")
    void firstRun_exit_createsData(@TempDir Path tmp) throws Exception {
        String out = runApp(tmp, "3\n", 20);

        assertTrue(out.length() > 0, "Ожидали какой-то вывод приложения");
        assertTrue(Files.exists(tmp.resolve("data").resolve("finance-data.json")),
                "Ожидали появление data/finance-data.json");
    }

    @Test
    @DisplayName("Регистрация → Logout → Exit: файл данных сохраняется")
    void register_logout_exit(@TempDir Path tmp) throws Exception {
        String input = String.join("\n",
                "1",          // Login
                "testuser",   // login
                "Ivan",       // name
                "Petrov",     // surname
                "pass123",    // password
                "3",          // Logout / назад
                "3"           // Exit
        ) + "\n";

        String out = runApp(tmp, input, 30);

        assertTrue(out.length() > 0);
        assertTrue(Files.exists(tmp.resolve("data").resolve("finance-data.json")),
                "Ожидали появление data/finance-data.json");
    }
}
