package org.example.app;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class MainAppIT {

  private static final String REL_PATH = "data/finance-data.json";
  private static final Pattern SAVE_PATH_RX =
      Pattern.compile("Saving\\s+data\\s+to\\s+file:\\s*(.+)", Pattern.CASE_INSENSITIVE);

  /** Запуск Main в дочернем JVM и возврат stdout (UTF-8). */
  private static String runApp(Path workDir, String stdin, long timeoutSec) throws Exception {
    String javaHome = System.getProperty("java.home");
    Path javaExe = Paths.get(javaHome, "bin", "java");
    String classpath = System.getProperty("java.class.path");

    String agentJar = System.getProperty("jacoco.agent.path");
    String destFile = System.getProperty("jacoco.agent.destfile");
    if (agentJar == null || destFile == null) {
      throw new IllegalStateException(
          "JaCoCo agent props missing; check build.gradle.kts tasks.test { doFirst{...} }");
    }

    List<String> cmd = new ArrayList<>();
    cmd.add(javaExe.toString());
    cmd.add("-cp");
    cmd.add(classpath);
    cmd.add("-javaagent:" + agentJar + "=destfile=" + destFile + ",append=true"); // без includes
    cmd.add("-Dfile.encoding=UTF-8");
    cmd.add("-Duser.dir=" + workDir.toAbsolutePath());
    cmd.add("org.example.app.Main");

    Files.createDirectories(workDir.resolve("data"));

    ProcessBuilder pb = new ProcessBuilder(cmd);
    pb.directory(workDir.toFile());
    pb.redirectErrorStream(true);

    Process p = pb.start();
    try (BufferedWriter w =
        new BufferedWriter(new OutputStreamWriter(p.getOutputStream(), StandardCharsets.UTF_8))) {
      if (stdin != null) w.write(stdin);
    }

    boolean finished = p.waitFor(timeoutSec, TimeUnit.SECONDS);
    if (!finished) {
      p.destroyForcibly();
      throw new RuntimeException("Main did not finish within timeout");
    }

    String out;
    try (InputStream in = p.getInputStream()) {
      out = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    // Быстрая диагностика: проверим, что exec от jacoco реально есть и не пустой
    Path execPath = Paths.get(destFile);
    if (!Files.exists(execPath) || Files.size(execPath) == 0) {
      System.err.println("WARN: JaCoCo exec empty or missing: " + execPath);
    }

    return out;
  }

  /** Парсинг явного пути из stdout (если приложение его печатает). */
  private static Path parsePathFromStdout(String stdout) {
    Matcher m = SAVE_PATH_RX.matcher(stdout);
    if (m.find()) {
      String raw = m.group(1).trim().replaceAll("[\"']", "");
      Path p = Paths.get(raw);
      return p.isAbsolute() ? p : Paths.get(System.getProperty("user.dir")).resolve(p).normalize();
    }
    return null;
  }

  /** Ищем файл в возможных местах, с ретраями. */
  private static Path findDataFileWithRetry(Path tmp, String stdout) throws Exception {
    Path[] candidates =
        new Path[] {
          tmp.resolve(REL_PATH),
          Paths.get(REL_PATH).toAbsolutePath().normalize(), // вдруг user.dir проигнорирован
          parsePathFromStdout(stdout)
        };

    // до 10 ретраев с шагом 200мс (учёт запаздывания записи/антивируса на Windows)
    for (int i = 0; i < 10; i++) {
      for (Path c : candidates) {
        if (c != null && Files.exists(c)) return c;
      }
      Thread.sleep(200);
    }
    return null;
  }

  @Test
  @DisplayName("First run → Exit: app runs, no crash; file may be absent on empty repo")
  void firstRun_exit_noCrash(@TempDir Path tmp) throws Exception {
    String out = runApp(tmp, "3\n", 30);
    assertTrue(out.length() > 0, "Expected some console output on first run");

    // Если приложение всё-таки что-то создало — сообщим, но не фейлим, если файла нет
    Path maybe = findDataFileWithRetry(tmp, out);
    if (maybe != null) {
      System.out.println("First run: data file created at " + maybe);
    } else {
      System.out.println("First run: no data file (expected for empty repository).");
    }
  }

  @Test
  @DisplayName("Register → Logout → Exit: data file is saved (must exist)")
  void register_logout_exit(@TempDir Path tmp) throws Exception {
    String input =
        String.join(
                "\n",
                "1", // Login / Register
                "testuser", // login
                "Ivan", // name
                "Petrov", // surname
                "pass123", // password
                "3", // Logout/back to main menu
                "3" // Exit
                )
            + "\n";

    String out = runApp(tmp, input, 45);

    Path found = findDataFileWithRetry(tmp, out);
    if (found == null) {
      fail(
          "Expected data file after register/logout/exit, but not found.\n"
              + "Checked:\n  - "
              + tmp.resolve(REL_PATH)
              + "\n  - "
              + Paths.get(REL_PATH).toAbsolutePath()
              + "\n(also parsed any explicit path from stdout)\n\nSTDOUT:\n"
              + out);
    }
    System.out.println("Register scenario: data file at " + found);
  }
}
