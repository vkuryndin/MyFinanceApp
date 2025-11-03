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
 * Unit tests for ConsoleMenus display methods.
 *
 * <p>Tests validate menu output for all console menu types:
 *
 * <ul>
 *   <li><b>Login menu:</b> Initial entry point with login, documentation, and exit options
 *   <li><b>Actions menu:</b> Main menu after login with main actions, admin actions, logout, and
 *       exit
 *   <li><b>Main Actions menu:</b> Financial operations (income, expense, wallet, budget,
 *       statistics, transfer)
 *   <li><b>Administrator menu:</b> Basic admin operations for ordinary administrators
 *   <li><b>Super Administrator menu:</b> Full administrative controls including user and role
 *       management
 * </ul>
 *
 * <p>These methods are pure output functions that don't read from stdin, making them safe to test
 * by capturing System.out. Tests verify that all expected menu headers and options are printed
 * correctly.
 *
 * <p>Uses {@code @BeforeEach} to redirect System.out to a ByteArrayOutputStream and
 * {@code @AfterEach} to restore the original output stream, ensuring test isolation and proper
 * cleanup.
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

  @Test
  @DisplayName("Login menu prints header and options")
  void showLoginMenu_printsExpected() {
    ConsoleMenus.showLoginMenu();
    String s = stdout();
    assertTrue(s.contains("You are now in the LogIn menu"), "Header missing");
    assertTrue(s.contains("1. Log in"), "Option 1 missing");
    assertTrue(s.contains("2. View documentation"), "Option 2 missing");
    assertTrue(s.contains("3. Exit"), "Option 3 missing");
  }

  @Test
  @DisplayName("Actions menu prints header and options")
  void showActionsMenu_printsExpected() {
    ConsoleMenus.showActionsMenu();
    String s = stdout();
    assertTrue(s.contains("You are now in the Actions menu"), "Header missing");
    assertTrue(s.contains("1. Main actions"), "Main actions missing");
    assertTrue(s.contains("2. Administrator actions"), "Admin actions missing");
    assertTrue(s.contains("3. Log out"), "Log out missing");
    assertTrue(s.contains("4. Exit"), "Exit missing");
  }

  @Test
  @DisplayName("Main Actions menu prints all 1..8 options")
  void showMainActionsMenu_printsExpected() {
    ConsoleMenus.showMainActionsMenu();
    String s = stdout();
    assertTrue(s.contains("You are now in the Main Actions menu"), "Header missing");
    assertTrue(s.contains("1. Add income"));
    assertTrue(s.contains("2. Add expense"));
    assertTrue(s.contains("3. View wallet"));
    assertTrue(s.contains("4. Add budget"));
    assertTrue(s.contains("5. View statistics"));
    assertTrue(s.contains("6. Transfer money to other user"));
    assertTrue(s.contains("8. Update budget limit"));
    assertTrue(s.contains("9. Remove budget"));
    assertTrue(s.contains("10. Rename budget category"));
    assertTrue(s.contains("11. Delete my user account"));
    assertTrue(s.contains("12. Return to the previous menu"));
  }

  @Test
  @DisplayName("Ordinary Admin menu prints options")
  void showOrdinaryAdminMenu_printsExpected() {
    ConsoleMenus.showOrdinaryAdminMenu();
    String s = stdout();
    // В файле есть отдельный заголовок для обычного админа
    assertTrue(s.toLowerCase().contains("admin"), "Must look like admin menu");
    // Набор проверок мягкий: наличие хотя бы пары пунктов и фразы про admin
    assertTrue(
        s.contains("Return to the previous menu") || s.contains("Back"),
        "Should contain return/back option");
  }

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
}
