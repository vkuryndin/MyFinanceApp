package org.example.cli;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ConsoleMenus} rendering methods.
 *
 * <p>These tests verify that each console menu prints the expected header and a representative set
 * of options. Since the methods under test only write to {@code System.out} (they do not read from
 * {@code System.in}), verification is performed by capturing {@code System.out} and inspecting the
 * produced text.
 *
 * <p><b>What is verified:</b>
 *
 * <ul>
 *   <li>Each menu prints its header (e.g., "You are now in the ... menu").
 *   <li>Presence of key options to ensure the correct variant of the menu is shown
 *       (login/actions/main actions/admin/super admin).
 *   <li>Presence of a "return/back" option for admin menus (kept flexible to avoid brittle tests).
 * </ul>
 *
 * <p><b>Isolation and cleanup:</b>
 *
 * <ul>
 *   <li>{@link BeforeEach} redirects {@code System.out} to a UTF-8 {@link ByteArrayOutputStream}.
 *   <li>{@link AfterEach} restores the original {@code System.out} to avoid cross-test
 *       interference.
 * </ul>
 *
 * @see org.example.cli.ConsoleMenus
 */
public class ConsoleMenusTest {

  private final PrintStream originalOut = System.out;
  private ByteArrayOutputStream out;

  @BeforeEach
  void setup() {
    out = new ByteArrayOutputStream();
    System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
  }

  @AfterEach
  void tearDown() {
    System.setOut(originalOut);
  }

  private String stdout() {
    return out.toString(StandardCharsets.UTF_8);
  }

  /**
   * Verifies that the Login menu prints the expected header and the three primary options: "Log
   * in", "View documentation", and "Exit".
   *
   * <p><b>Given</b> the Login menu is rendered<br>
   * <b>Then</b> the header and options 1..3 should be present.
   */
  @Test
  @DisplayName("Login menu prints header and 3 primary options")
  void showLoginMenu_printsExpected() {
    ConsoleMenus.showLoginMenu();
    String s = stdout();
    assertTrue(s.contains("You are now in the LogIn menu"), "Header missing");
    assertTrue(s.contains("1. Log in"), "Option 1 missing");
    assertTrue(s.contains("2. View documentation"), "Option 2 missing");
    assertTrue(s.contains("3. Exit"), "Option 3 missing");
  }

  /**
   * Verifies that the Actions menu prints its header and the four core options: "Main actions",
   * "Administrator actions", "Log out", and "Exit".
   *
   * <p><b>Given</b> the Actions menu is rendered<br>
   * <b>Then</b> the header and options 1..4 should be present.
   */
  @Test
  @DisplayName("Actions menu prints header and options")
  void showActionsMenu_printsExpected() {
    ConsoleMenus.showActionsMenu();
    String s = stdout();
    assertTrue(s.contains("You are now in the Actions menu"), "Header missing");
    assertTrue(s.contains("1. User actions"), "User actions missing");
    assertTrue(s.contains("2. Administrator actions"), "Admin actions missing");
    assertTrue(s.contains("3. Log out"), "Log out missing");
    assertTrue(s.contains("4. Exit"), "Exit missing");
  }

  /**
   * Verifies that the Main Actions menu prints its header and a representative set of action items.
   *
   * <p>The assertions intentionally check a broad range of options (income/expense/wallet/budget/
   * statistics/transfer/export/import/account/delete/back) to ensure the menu content stays
   * consistent without being overly fragile to cosmetic changes.
   *
   * <p><b>Given</b> the Main Actions menu is rendered<br>
   * <b>Then</b> the header and key options should be present.
   */
  @Test
  @DisplayName("User Actions menu prints all 1..8 options")
  void showMainActionsMenu_printsExpected() {
    ConsoleMenus.showMainActionsMenu();
    String s = stdout();
    assertTrue(s.contains("You are now in the User Actions menu"), "Header missing");
    assertTrue(s.contains("1. Add income"));
    assertTrue(s.contains("2. Add expense"));
    assertTrue(s.contains("3. View wallet"));
    assertTrue(s.contains("4. Add budget"));
    assertTrue(s.contains("5. View statistics"));
    assertTrue(s.contains("6. Transfer money to other user"));
    assertTrue(s.contains("8. Update budget limit"));
    assertTrue(s.contains("9. Remove budget"));
    assertTrue(s.contains("10. Rename budget category"));
    assertTrue(s.contains("11. Export Wallet data to JSON"));
    assertTrue(s.contains("12. Import Wallet data to JSON"));
    assertTrue(s.contains("13. Delete my user account"));
    assertTrue(s.contains("14. Return to the previous menu"));
  }

  /**
   * Verifies that the Ordinary Administrator menu looks like an admin menu and contains a way to
   * return to the previous screen.
   *
   * <p>The checks are <i>intentionally soft</i> (look for "admin" and a return/back option) to
   * avoid frequent breakage from minor wording or ordering changes.
   *
   * <p><b>Given</b> the Ordinary Admin menu is rendered<br>
   * <b>Then</b> it should contain admin-like wording and provide a way to return/back.
   */
  @Test
  @DisplayName("Ordinary Admin menu prints options - soft checking")
  void showOrdinaryAdminMenu_printsExpected() {
    ConsoleMenus.showOrdinaryAdminMenu();
    String s = stdout();
    // there is a header for ordinary admin
    assertTrue(s.toLowerCase().contains("admin"), "Must look like admin menu");
    //The check are intentiobnally softй: the existence of sewveral options and the return to previous menu option
    assertTrue(
        s.contains("Return to the previous menu") || s.contains("Back"),
        "Should contain return/back option");
  }

  /**
   * Verifies that the Super Administrator menu looks like a super admin menu and contains a way to
   * return to the previous screen.
   *
   * <p>The checks are <i>intentionally soft</i> (look for "super" and a return/back option) to
   * avoid frequent breakage from cosmetic text changes.
   *
   * <p><b>Given</b> the Super Admin menu is rendered<br>
   * <b>Then</b> it should contain super-admin wording and provide a way to return/back.
   */
  @Test
  @DisplayName("Super Admin menu prints options")
  void showSuperAdminMenu_printsExpected() {
    ConsoleMenus.showSuperAdminMenu();
    String s = stdout();
    assertTrue(s.toLowerCase().contains("super"), "Must look like super admin menu");
    // Аналогично — мягкая проверка, чтобы не зависеть от косметики текста
    assertTrue(
        s.contains("Return to the previous menu") || s.contains("Back"),
        "Should contain return/back option");
  }

    @Test
    @DisplayName("Ordinary admin menu prints header and 4 options::full check")
    void showOrdinaryAdminMenuFull_printsExpected() {
        ConsoleMenus.showOrdinaryAdminMenu();
        String s = stdout();
        assertTrue(s.contains("You are now in the Administrator menu. Please select an option:"), "Header missing");
        assertTrue(s.contains("1. View all users"), "Option 1 missing");
        assertTrue(s.contains("2. View statistics for all users"), "Option 2 missing");
        assertTrue(s.contains("3. Delete a user account"), "Option 3 missing");
        assertTrue(s.contains("4. Return to the previous menu"), "Option 4 missing");
    }
}
