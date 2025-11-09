package org.example.app;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.*;

/**
 * Integration tests for the {@code org.example.app.Main} application.
 *
 * <p>Each test launches the app in a <b>separate JVM process</b> via {@link ProcessBuilder},
 * simulates user input by writing a scripted sequence to {@code stdin}, and captures both {@code
 * stdout} and {@code stderr}. Every test runs in an isolated temporary working directory with its
 * own {@code data/} folder to ensure independence and repeatability.
 *
 * <p><b>Test coverage areas:</b>
 *
 * <ul>
 *   <li><b>Smoke:</b> startup, menu rendering, clean exit
 *   <li><b>First-user flow:</b> registration of the initial user who becomes {@code SUPER_ADMIN},
 *       logout path, and basic admin navigation
 *   <li><b>Menu navigation:</b> main actions, administrator actions, and handling of invalid input
 *   <li><b>Transactions:</b> adding income/expense, viewing wallet, and viewing statistics
 * </ul>
 *
 * <p><b>JaCoCo integration:</b> if the system properties {@code jacoco.agent.path} and {@code
 * jacoco.agent.destfile} are provided, the spawned process is started with the JaCoCo Java agent
 * ({@code append=true}) so that coverage from the external JVM is recorded into the specified exec
 * file.
 *
 * <p><b>Notes:</b> Temporary directories are created with and are not explicitly deleted by these
 * tests.
 *
 * @see org.example.app.Main
 */
class MainTest {

  static class ExecResult {
    final int exitCode;
    final String out;
    final String err;

    ExecResult(int exitCode, String out, String err) {
      this.exitCode = exitCode;
      this.out = out;
      this.err = err;
    }
  }

  /**
   * Creates a new temporary working directory and an empty {@code data/} subdirectory.
   *
   * <p>Absence of {@code data/finance-data.json} ensures that on first login the created user is
   * promoted to {@code SUPER_ADMIN}.
   */
  private Path newWorkDir() throws IOException {
    Path wd = Files.createTempDirectory("finance-app-it-");
    Files.createDirectories(wd.resolve("data"));
    return wd;
  }

  /**
   * Starts {@code org.example.app.Main} in a separate JVM process, feeds the provided script into
   * {@code stdin}, and captures {@code stdout}/{@code stderr}.
   *
   * <p>Behavioral details:
   *
   * <ul>
   *   <li>Classpath is taken from {@code System.getProperty("java.class.path")}.
   *   <li>Working directory is forced to {@code workDir} via {@code -Duser.dir} and
   *       ProcessBuilder's {@link ProcessBuilder#directory(File)}.
   *   <li>UTF-8 is enforced via {@code -Dfile.encoding=UTF-8} and streams are read/written in
   *       UTF-8.
   *   <li>If {@code jacoco.agent.path} and {@code jacoco.agent.destfile} system properties are set,
   *       the JaCoCo agent is attached with {@code append=true}.
   * </ul>
   *
   * @param script newline-separated keystrokes to feed into the app (may be {@code null})
   * @param workDir working directory to use for the spawned process
   * @return exit code and captured output
   */
  private ExecResult runApp(String script, Path workDir) throws Exception {
    String javaBin =
        Paths.get(
                System.getProperty("java.home"),
                "bin",
                System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java")
            .toString();
    String cp = System.getProperty("java.class.path");

    String agentJar = System.getProperty("jacoco.agent.path");
    String destFile = System.getProperty("jacoco.agent.destfile");

    List<String> cmd = new ArrayList<>();
    cmd.add(javaBin);
    cmd.add("-cp");
    cmd.add(cp);
    if (agentJar != null && !agentJar.isBlank()) {
      cmd.add("-javaagent:" + agentJar + "=destfile=" + destFile + ",append=true");
    }
    cmd.add("-Dfile.encoding=UTF-8");
    cmd.add("-Duser.dir=" + workDir.toAbsolutePath());
    cmd.add("org.example.app.Main");

    ProcessBuilder pb = new ProcessBuilder(cmd);
    pb.directory(workDir.toFile());
    pb.redirectErrorStream(false);
    Process p = pb.start();

    // stdin
    try (BufferedWriter w =
        new BufferedWriter(new OutputStreamWriter(p.getOutputStream(), StandardCharsets.UTF_8))) {
      if (script != null) w.write(script);
    }

    // stdout / stderr
    String out, err;
    try (InputStream is = p.getInputStream();
        InputStream es = p.getErrorStream()) {
      out = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      err = new String(es.readAllBytes(), StandardCharsets.UTF_8);
    }

    int code = p.waitFor();
    return new ExecResult(code, out, err);
  }

  // ------ BASIC SMOKE TESTS ------

  /**
   * Verifies that the application shows the Login menu and exits cleanly with status code 0 when
   * the user immediately selects the Exit option.
   *
   * <p><b>Flow:</b> feed {@code 3} (Exit) → expect header of the Login menu to be printed and
   * {@code exitCode == 0}.
   *
   * <p><b>Asserts:</b>
   *
   * <ul>
   *   <li>Exit code equals 0
   *   <li>Login menu header {@code "You are now in the LogIn menu"} is present in stdout
   * </ul>
   */
  @Test
  @DisplayName("Application should display login menu and exit with code 0")
  void start_then_exit_ok() throws Exception {
    Path wd = newWorkDir();
    ExecResult r = runApp("3\n", wd); // Exit
    if (r.exitCode != 0) {
      System.out.println("OUT:\n" + r.out);
      System.err.println("ERR:\n" + r.err);
    }
    assertEquals(0, r.exitCode, "App must exit with code 0.");
    assertTrue(r.out.contains("You are now in the LogIn menu"), "Main menu should be shown.");
  }

  /**
   * Ensures graceful handling of an invalid option at the Login menu followed by Exit.
   *
   * <p><b>Flow:</b> feed {@code 99} (invalid) → {@code 3} (Exit).
   *
   * <p><b>Asserts:</b>
   *
   * <ul>
   *   <li>Exit code equals 0
   * </ul>
   */
  @Test
  @DisplayName("Application should exit with code 0 after invalid option input")
  void invalid_then_exit_ok() throws Exception {
    Path wd = newWorkDir();
    ExecResult r = runApp(String.join("\n", "99", "3") + "\n", wd);
    if (r.exitCode != 0) {
      System.out.println("OUT:\n" + r.out);
      System.err.println("ERR:\n" + r.err);
    }
    assertEquals(0, r.exitCode, "App must exit with code 0 after invalid option.");
  }

  /**
   * Validates the documentation screen flow and successful exit afterward.
   *
   * <p><b>Flow:</b>
   *
   * <ol>
   *   <li>{@code 2} → open documentation
   *   <li>{@code 1} → return to Login menu
   *   <li>{@code 3} → exit application
   * </ol>
   *
   * <p><b>Asserts:</b>
   *
   * <ul>
   *   <li>Exit code equals 0
   *   <li>Documentation header is printed
   *   <li>Return instruction is printed
   * </ul>
   */
  @Test
  @DisplayName("Application should exit with code 0 after viewing docs")
  void docs_then_exit_ok() throws Exception {
    Path wd = newWorkDir();

    // Flow:
    // 2 -> View docs
    // 1 -> Return from documentation back to the Login/Main menu
    // 3 -> Exit application
    String userFlow = String.join("\n", "2", "1", "3") + "\n";

    ExecResult r = runApp(userFlow, wd);

    if (r.exitCode != 0) {
      System.out.println("OUT:\n" + r.out);
      System.err.println("ERR:\n" + r.err);
    }

    // must exit successfully
    assertEquals(0, r.exitCode, "App must exit with code 0 after docs navigation + exit.");

    // sanity checks: docs were shown and prompt to return was printed
    assertTrue(
        r.out.contains("MyFinanceApp Quick Start Guide"), "Documentation header should be printed");
    assertTrue(
        r.out.contains("Press 1 to return to the Login menu"),
        "Docs screen should instruct how to return");
  }

  // ------ SUPER ADMIN SCENARIO (1st user) ------

  /**
   * Covers the first-user bootstrap flow: the initial login creates a {@code SUPER_ADMIN}, then the
   * user navigates through Main Actions to add a couple of expenses, views the wallet, returns to
   * the Actions menu and exits.
   *
   * <p><b>Flow:</b>
   *
   * <ol>
   *   <li>Login → create first user (becomes {@code SUPER_ADMIN})
   *   <li>Open Main Actions, add two expenses (empty date = today), view wallet
   *   <li>Return to Actions, then Exit
   * </ol>
   *
   * <p><b>Asserts:</b>
   *
   * <ul>
   *   <li>Exit code equals 0
   *   <li>Actions and Main Actions headers are printed
   * </ul>
   */
  @Test
  @DisplayName(
      "Should complete full user journey: create super admin, add transactions, navigate menus")
  void loginSuperadmin_walkMenus_then_exit() throws Exception {
    Path wd = newWorkDir();

    String login = "adminuser";
    String pass = "StrongPass1!";

    // Input scenario:
    // 1 -> Login
    // login, pass, pass -> create first user => SUPER_ADMIN
    // John/Smith/... -> safe values for profile fields
    // Main actions (1):
    //   add expense (2): amount -> title -> "" (empty date = today)
    //   add expense (2): amount -> title -> "" (empty date = today)
    //   view wallet (3)
    // return from Main actions (14), then exit from Actions (4)
    String script =
        String.join(
                "\n",
                "1",
                login,
                pass,
                pass,
                "John",
                "Smith",
                "john@example.com",
                "Armenia",
                "Yerevan",
                "30",
                "100",
                "0",

                // In Actions: go to Main actions (1), add expense twice (2),
                // each time: amount, then title; then view wallet (3)
                "1", // Main actions
                "2",
                "4",
                "coffee",
                "", // <-- date(empty = today)
                "2",
                "1",
                "taxi",
                "", // <-- date (empty = today)
                "3", // View wallet

                // Return and exit
                "14", // Return to previous menu (from Main Actions)
                "4" // Exit (from Actions menu)
                )
            + "\n";

    ExecResult r = runApp(script, wd);

    if (r.exitCode != 0) {
      System.out.println("OUT:\n" + r.out);
      System.err.println("ERR:\n" + r.err);
    }

    assertEquals(0, r.exitCode, "App must exit with code 0.");
    assertTrue(r.out.contains("You are now in the Actions menu"), "Should reach Actions menu.");
    assertTrue(
        r.out.contains("You are now in the User Actions menu"), "Should reach User Actions menu.");
  }

  /** Invalid option at Login menu → then Exit. Covers default-branch in runLoginMenu. */
  @Test
  @DisplayName("Login menu should handle invalid input (9) and allow exit with code 0")
  void loginMenu_invalid_then_exit() throws Exception {
    Path wd = newWorkDir();
    String script =
        String.join(
                "\n",
                "9", // invalid option at login menu
                "3" // Exit
                )
            + "\n";
    ExecResult r = runApp(script, wd);
    if (r.exitCode != 0) {
      System.out.println("OUT:\n" + r.out);
      System.err.println("ERR:\n" + r.err);
    }
    assertEquals(0, r.exitCode, "App should exit with code 0 after invalid→exit.");
    assertTrue(r.out.contains("You are now in the LogIn menu"), "Login menu should be printed.");
  }

  /**
   * Creates the first user (becoming {@code SUPER_ADMIN}), immediately logs out, and exits the
   * application. Exercises the logout path right after bootstrap.
   *
   * <p><b>Flow:</b> Login → create first user → Actions → Logout → Login menu → Exit.
   *
   * <p><b>Asserts:</b>
   *
   * <ul>
   *   <li>Exit code equals 0
   *   <li>Actions menu header is printed before logout
   * </ul>
   */
  @Test
  @DisplayName("First user becomes super admin, logs out, and exits with code 0")
  void createSuperadmin_then_logout_then_exit() throws Exception {
    Path wd = newWorkDir();
    String script =
        String.join(
                "\n",
                "1", // Log in
                "adminuser", // login
                "StrongPass1!", // pass
                "StrongPass1!", // confirm
                "John", // name
                "Smith", // surname
                "john@example.com", // email
                "Armenia", // country
                "Yerevan", // city
                "30",
                "100",
                "0", // age? salary? children?
                // В Actions:
                "3", // Log out
                // Снова логин-меню:
                "3" // Exit
                )
            + "\n";
    ExecResult r = runApp(script, wd);
    if (r.exitCode != 0) {
      System.out.println("OUT:\n" + r.out);
      System.err.println("ERR:\n" + r.err);
    }
    assertEquals(0, r.exitCode, "App should exit with code 0 after logout→exit.");
    assertTrue(
        r.out.contains("You are now in the Actions menu"), "Actions menu should be reached.");
  }

  /**
   * Opens User Actions and immediately returns to the previous menu before exiting. Covers the
   * "Return to previous menu" branch in Main Actions.
   *
   * <p><b>Flow:</b> Login (bootstrap) → User Actions → Return (14) → Exit.
   *
   * <p><b>Asserts:</b>
   *
   * <ul>
   *   <li>Exit code equals 0
   *   <li>Main Actions header is printed
   * </ul>
   */
  @Test
  @DisplayName("Enter User Actions and return to cover 'Return to previous menu' branch")
  void mainActions_return_immediately_then_exit() throws Exception {
    Path wd = newWorkDir();
    String script =
        String.join(
                "\n",
                "1",
                "adminuser",
                "P@ssw0rd!",
                "P@ssw0rd!",
                "John",
                "Smith",
                "john@example.com",
                "Armenia",
                "Yerevan",
                "30",
                "100",
                "0",
                "1", // Actions → Main actions
                "14", // <-- теперь Return to previous menu
                "4" // Exit (из Actions)
                )
            + "\n";
    ExecResult r = runApp(script, wd);
    if (r.exitCode != 0) {
      System.out.println("OUT:\n" + r.out);
      System.err.println("ERR:\n" + r.err);
    }
    assertEquals(0, r.exitCode, "App must exit with code 0.");
    assertTrue(
        r.out.contains("You are now in the User Actions menu"), "User Actions should be printed.");
  }

  /**
   * Exercises invalid input handling in the Actions menu: non-numeric, empty, and out-of-range
   * values, then exits successfully.
   *
   * <p><b>Flow:</b> Login (bootstrap) → Actions → {@code x}, empty string, {@code 99} → {@code 4}
   * (Exit).
   *
   * <p><b>Asserts:</b>
   *
   * <ul>
   *   <li>Exit code equals 0
   *   <li>At least one "Invalid option" message is printed
   * </ul>
   */
  @Test
  @DisplayName(
      "Actions menu should gracefully handle invalid inputs (text, empty, out-of-range) and exit")
  void actionsMenu_multiple_invalid_then_exit() throws Exception {
    Path wd = newWorkDir();
    String script =
        String.join(
                "\n",
                "1", // Log in (сreate new user → SUPER_ADMIN)
                "adminuser",
                "Qwerty12!",
                "Qwerty12!",
                "John",
                "Smith",
                "john@example.com",
                "Armenia",
                "Yerevan",
                "30",
                "100",
                "0",
                // В Actions:
                "x", // invalid
                "", // invalid (empty string)
                "99", // invalid (out of range)
                "4" // Exit
                )
            + "\n";
    ExecResult r = runApp(script, wd);
    if (r.exitCode != 0) {
      System.out.println("OUT:\n" + r.out);
      System.err.println("ERR:\n" + r.err);
    }
    assertEquals(0, r.exitCode, "App should exit with 0 after invalids→exit.");
    assertTrue(r.out.contains("Invalid option"), "Should print invalid option at least once.");
  }

  /**
   * Runs a short Main Actions scenario: add an income transaction, open the statistics view, return
   * to Actions, and exit.
   *
   * <p><b>Flow:</b> Login (bootstrap) → Main Actions → Add income → View statistics → Return →
   * Exit.
   *
   * <p><b>Rationale:</b> validates the income path and statistics screen without asserting exact
   * report content (smoke/path coverage).
   *
   * <p><b>Asserts:</b>
   *
   * <ul>
   *   <li>Exit code equals 0
   *   <li>Main Actions header is printed
   * </ul>
   */
  @Test
  @DisplayName("User Actions should allow adding income and viewing statistics")
  void mainActions_addIncome_viewStats_then_exit() throws Exception {
    Path wd = newWorkDir();
    String script =
        String.join(
                "\n",
                "1", // Log in
                "adminuser",
                "StrongPass1!",
                "StrongPass1!",
                "John",
                "Smith",
                "john@example.com",
                "Armenia",
                "Yerevan",
                "30",
                "100",
                "0",
                "1", // Main actions
                "1",
                "150",
                "salary",
                "", // <-- дата (пусто = today)
                "5", // View statistics
                "14", // back to Actions
                "4" // Exit
                )
            + "\n";
    ExecResult r = runApp(script, wd);
    if (r.exitCode != 0) {
      System.out.println("OUT:\n" + r.out);
      System.err.println("ERR:\n" + r.err);
    }
    assertEquals(0, r.exitCode, "App must exit 0.");
    assertTrue(
        r.out.contains("You are now in the User Actions menu"), "User actions should appear.");
  }

  /**
   * Exercises a full navigation loop: Login → Main Actions → invalid input handling → return →
   * Admin Actions → invalid input handling → exit.
   *
   * <p>This test intentionally triggers several default branches in both Main Actions and
   * Administrator Actions menus. It verifies that the application:
   *
   * <ul>
   *   <li>recovers gracefully from invalid numeric inputs
   *   <li>correctly processes “Return to previous menu” commands in nested menus
   *   <li>properly enters and exits the Administrator Actions section
   *   <li>exits cleanly with status code 0
   * </ul>
   *
   * <p>The goal is not to validate specific admin functions but to ensure stable menu traversal,
   * error path handling, and complete control flow coverage across submenus.
   */
  @Test
  @DisplayName("Navigate Main Actions and Admin Actions menus, handle invalid inputs, and exit")
  void openAdminActions_then_back_then_exit() throws Exception {
    Path wd = newWorkDir();
    String script =
        String.join(
                "\n",
                "1",
                "adminuser",
                "Aa11!!aa",
                "Aa11!!aa",
                "John",
                "Smith",
                "john@example.com",
                "Armenia",
                "Yerevan",
                "30",
                "100",
                "0",
                "1", // Main actions
                "0", // invalid in main actions
                "14", // <-- Return to previous menu
                "2", // Administrator actions
                "0", // invalid inside admin menu
                "8", // <-- Return to previous menu
                "4" // Exit из Actions
                )
            + "\n";
    ExecResult r = runApp(script, wd);
    if (r.exitCode != 0) {
      System.out.println("OUT:\n" + r.out);
      System.err.println("ERR:\n" + r.err);
    }
    assertEquals(0, r.exitCode, "App must exit 0.");
  }
}
