package org.example.app;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.*;

/**
 * Integration tests for the Main application class.
 *
 * <p>These tests run the application as a separate process, simulating real user interaction by
 * feeding scripted input to stdin and capturing stdout/stderr output. Each test creates an isolated
 * temporary working directory to ensure test independence.
 *
 * <p>Test categories:
 *
 * <ul>
 *   <li><b>Basic smoke tests:</b> Verify application startup, exit, and basic menu navigation
 *   <li><b>Super admin scenarios:</b> Test first user registration (becomes SUPER_ADMIN), logout
 *       functionality, and administrative operations
 *   <li><b>Menu navigation tests:</b> Exercise various menu paths including Main Actions,
 *       Administrator Actions, and invalid input handling
 *   <li><b>Transaction operations:</b> Test income/expense addition, wallet viewing, and statistics
 *       display
 * </ul>
 *
 * <p>The tests include JaCoCo agent integration to collect code coverage from the spawned process,
 * ensuring accurate coverage metrics for the entire application flow.
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
   * Create a working directory with an empty data/ folder (without a file) so the 1st user becomes
   * SUPER_ADMIN.
   */
  private Path newWorkDir() throws IOException {
    Path wd = Files.createTempDirectory("finance-app-it-");
    Files.createDirectories(wd.resolve("data"));
    return wd;
  }

  /** Start org.example.app.Main as a separate process and feed the script to stdin. */
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

  @Test
  @DisplayName("Application should exit with code 0 after viewing docs")
  void docs_then_exit_ok() throws Exception {
    Path wd = newWorkDir();
    ExecResult r = runApp(String.join("\n", "2", "3") + "\n", wd); // View docs -> Exit
    if (r.exitCode != 0) {
      System.out.println("OUT:\n" + r.out);
      System.err.println("ERR:\n" + r.err);
    }
    assertEquals(0, r.exitCode, "App must exit with code 0 after docs.");
  }

  // ------ SUPER ADMIN SCENARIO (1st user) ------

  /**
   * First run without data/finance-data.json: - Log in -> create 1st user (=> SUPER_ADMIN) -
   * Provide valid strings for possible additional questions (name, surname, email, etc.) - Then a
   * long tail of menu options: 1/2/3/0/9/... to enter several branches and then exit
   */
  @Test
  @DisplayName(
      "Should complete full user journey: create super admin, add transactions, navigate menus")
  void loginSuperadmin_walkMenus_then_exit() throws Exception {
    Path wd = newWorkDir();

    String login = "adminuser";
    String pass = "StrongPass1!";

    // Input scenario:
    // 1 -> Log in
    // login, pass, pass -> create 1st user => SUPER_ADMIN
    // John/Smith/... -> safe values for text fields
    // Add a couple of expenses (amount + title), view wallet
    // 8 -> back from Main Actions, 4 -> Exit from Actions
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
                "", // <-- дата (пусто = today)
                "2",
                "1",
                "taxi",
                "", // <-- дата (пусто = today)
                "3", // View wallet

                // Return and exit
                "12", // Return to previous menu (from Main Actions)
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
        r.out.contains("You are now in the Main Actions menu"), "Should reach Main Actions menu.");
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

  /** Create first user (becomes SUPER_ADMIN) → immediately Logout → Exit. Covers logout path. */
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
   * Go to Main Actions and immediately return back, then Exit. Covers 'Return to previous menu'
   * branch in main actions.
   */
  @Test
  @DisplayName("Enter Main Actions and return to cover 'Return to previous menu' branch")
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
                "12", // <-- теперь Return to previous menu
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
        r.out.contains("You are now in the Main Actions menu"), "Main Actions should be printed.");
  }

  /**
   * Actions menu: several invalid inputs → then Exit. Covers default-branch in runActionsMenu and
   * readIntSafe loop.
   */
  @Test
  @DisplayName(
      "Actions menu should gracefully handle invalid inputs (text, empty, out-of-range) and exit")
  void actionsMenu_multiple_invalid_then_exit() throws Exception {
    Path wd = newWorkDir();
    String script =
        String.join(
                "\n",
                "1", // Log in (создаём первого пользователя → SUPER_ADMIN)
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
                "", // invalid (пустая строка)
                "99", // invalid (вне диапазона)
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
   * Main actions: add income, then view statistics, back, exit. Exercises income path & stats view.
   */
  @Test
  @DisplayName("Main Actions should allow adding income and viewing statistics")
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
                "12", // back to Actions
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
        r.out.contains("You are now in the Main Actions menu"), "Main actions should appear.");
  }

  /**
   * Full loop: Login → Main actions → invalids → back → Admin actions open/close → Exit. Даёт
   * дополнительную трассу по default в подменю и переход в админ-раздел (без предположения о
   * конкретных пунктах).
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
                "12", // <-- Return to previous menu (заменили 8)
                "2", // Administrator actions
                "0", // invalid inside admin menu
                "8", // <-- Return to previous menu (если у тебя в админ-меню 1–4)
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
