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
 * Integration tests for org.example.app.Main. Runs the app in a child JVM with the same JaCoCo
 * agent that Gradle passes via system properties. Covers: plain exit, invalid main-menu option
 * (default branch), doc->back->exit.
 */
class MainTest {

  /** Main menu mapping from your output: 1. Log in 2. View documentation 3. Exit */
  private static final String MAIN_DOCS = "2";

  private static final String MAIN_EXIT = "3";

  private static class RunResult {
    final int exitCode;
    final String out;
    final String err;

    RunResult(int exitCode, String out, String err) {
      this.exitCode = exitCode;
      this.out = out;
      this.err = err;
    }
  }

  /** Build child JVM command with JaCoCo agent wired from Gradle system properties. */
  private static List<String> buildCmd(Path workDir) {
    Path javaBin = Paths.get(System.getProperty("java.home"), "bin", "java");
    String cp = System.getProperty("java.class.path");

    String agentJar = System.getProperty("jacoco.agent.path");
    String destFile = System.getProperty("jacoco.agent.destfile");
    assertNotNull(
        agentJar,
        "jacoco.agent.path is null (check tasks.test { doFirst { ... } } in build.gradle.kts)");
    assertNotNull(
        destFile,
        "jacoco.agent.destfile is null (check tasks.test { doFirst { ... } } in build.gradle.kts)");

    List<String> cmd = new ArrayList<>();
    cmd.add(javaBin.toString());
    cmd.add("-cp");
    cmd.add(cp);
    // write into the same exec file as Gradle’s Test JVM (append=true)
    cmd.add("-javaagent:" + agentJar + "=destfile=" + destFile + ",append=true");
    cmd.add("-Dfile.encoding=UTF-8");
    cmd.add("-Duser.dir=" + workDir.toAbsolutePath()); // so that data/ is under a temp dir
    cmd.add("org.example.app.Main");
    return cmd;
  }

  private static RunResult runApp(Path workDir, String stdinFeed, long timeoutSec)
      throws Exception {
    List<String> cmd = buildCmd(workDir);
    ProcessBuilder pb = new ProcessBuilder(cmd);
    pb.redirectErrorStream(false);
    Process p = pb.start();

    try (BufferedWriter w =
        new BufferedWriter(new OutputStreamWriter(p.getOutputStream(), StandardCharsets.UTF_8))) {
      if (stdinFeed != null) {
        w.write(stdinFeed);
      }
    }

    boolean finished = p.waitFor(timeoutSec, TimeUnit.SECONDS);
    if (!finished) {
      p.destroyForcibly();
      fail(
          "App did not finish within "
              + timeoutSec
              + "s. Probably waiting for input.\nCMD: "
              + cmd);
    }

    String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    String err = new String(p.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
    return new RunResult(p.exitValue(), out, err);
  }

  @Test
  @DisplayName("Simple start → immediate exit (menu: 3)")
  void start_then_exit_ok(@TempDir Path tmp) throws Exception {
    // Just press Exit at the main menu
    String feed = MAIN_EXIT + "\n";
    RunResult r = runApp(tmp, feed, 20);
    assertEquals(
        0,
        r.exitCode,
        "App should exit with code 0 on plain exit.\nOUT:\n" + r.out + "\nERR:\n" + r.err);
  }

  @Test
  @DisplayName("Main menu: invalid option (default branch) → then exit")
  void invalid_option_then_exit(@TempDir Path tmp) throws Exception {
    // 9 triggers "Invalid option, Choose  1-3", then give a valid Exit = 3
    String feed = "9\n" + MAIN_EXIT + "\n";
    RunResult r = runApp(tmp, feed, 25);
    assertEquals(
        0,
        r.exitCode,
        "Expected exit code 0 after invalid option and then Exit.\nOUT:\n"
            + r.out
            + "\nERR:\n"
            + r.err);
    assertTrue(
        r.out.contains("Invalid option") || r.out.contains("Choose  1-3"),
        "Should print invalid-option message. OUT:\n" + r.out);
  }

  @Test
  @DisplayName("Docs → back to main → exit (menu: 2 → 3)")
  void docs_then_exit(@TempDir Path tmp) throws Exception {
    // “View documentation” (2) usually returns back to the main menu without extra prompts; then
    // exit (3)
    String feed = MAIN_DOCS + "\n" + MAIN_EXIT + "\n";
    RunResult r = runApp(tmp, feed, 25);
    assertEquals(
        0,
        r.exitCode,
        "Expected code 0 after viewing docs and exiting.\nOUT:\n" + r.out + "\nERR:\n" + r.err);
  }
}
