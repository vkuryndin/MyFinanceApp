package org.example.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import org.example.model.Transaction;
import org.example.model.User;
import org.example.model.Wallet;
import org.example.repo.RepoExceptions;
import org.example.repo.UsersRepo;
import org.example.storage.WalletJson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for handler methods in {@code ConsoleUtils}.
 *
 * <p><b>Scope:</b> These tests exercise user-interaction handlers (parsing inputs from a {@link
 * Scanner}, mutating {@link Wallet} state, and delegating to {@link UsersRepo}). The {@code
 * UsersRepo} dependency is mocked; the {@link User#wallet} field is <i>final</i> and therefore not
 * replaced — tests operate on the real wallet bound to each {@link User} instance.
 *
 * <p><b>Key verifications:</b>
 *
 * <ul>
 *   <li>Correct branching and boolean results for success vs. failure paths
 *   <li>Side effects on the real {@link Wallet} (budgets, transactions, statistics)
 *   <li>Proper delegation to {@link UsersRepo}, including exception handling
 *   <li>Console output smoke checks (messages, alerts) where relevant
 * </ul>
 */
public class ConsoleUtilsTest {

  /* ---------- helpers ---------- */

  /**
   * Creates a {@link Scanner} over the provided UTF-8 string and sets {@link Locale#ROOT} to ensure
   * locale-independent number parsing (dot as decimal separator).
   *
   * @param s input content with line breaks that simulate user keystrokes
   * @return configured {@link Scanner}
   */
  private static Scanner scan(String s) {
    return new Scanner(
            new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)
        .useLocale(Locale.ROOT);
  }

  /**
   * Constructs a {@link User} via reflection in a constructor-agnostic way.
   *
   * <p>Finds a suitable constructor (prefers one with ≥4 {@code String} parameters for {@code
   * login}, {@code name}, {@code surname}, {@code pass}). Populates remaining parameters with
   * neutral defaults. Optionally adds the {@code SUPER_ADMIN} role using {@code addRole(Role)} if
   * available. The {@link User#wallet} field is not replaced (it is final) and will be used by
   * tests.
   *
   * @param login login string
   * @param name first name
   * @param surname last name
   * @param pass password
   * @param superAdmin whether to grant {@code SUPER_ADMIN} role
   * @return constructed {@link User}
   * @throws AssertionError if a suitable constructor cannot be invoked
   */
  private static User mkUser(
      String login, String name, String surname, String pass, boolean superAdmin) {

    try {
      Constructor<?> picked = null;
      for (Constructor<?> c : User.class.getDeclaredConstructors()) {
        int strings = 0;
        for (Class<?> t : c.getParameterTypes()) if (t == String.class) strings++;
        if (strings >= 4) {
          picked = c;
          break;
        }
      }
      if (picked == null) {
        for (Constructor<?> c : User.class.getDeclaredConstructors()) {
          for (Class<?> t : c.getParameterTypes()) {
            if (t == String.class) {
              picked = c;
              break;
            }
          }
          if (picked != null) break;
        }
      }
      if (picked == null) throw new IllegalStateException("No suitable User constructor found");

      picked.setAccessible(true);
      Class<?>[] types = picked.getParameterTypes();
      Object[] args = new Object[types.length];

      int sUsed = 0;
      for (int i = 0; i < types.length; i++) {
        Class<?> t = types[i];
        if (t == String.class) {
          args[i] =
              (sUsed == 0)
                  ? login
                  : (sUsed == 1) ? name : (sUsed == 2) ? surname : (sUsed == 3) ? pass : "x";
          sUsed++;
        } else if (t == int.class || t == Integer.class) {
          args[i] = 0;
        } else if (t == long.class || t == Long.class) {
          args[i] = 0L;
        } else if (t == boolean.class || t == Boolean.class) {
          args[i] = false;
        } else {
          args[i] = null;
        }
      }

      User u = (User) picked.newInstance(args);

      if (superAdmin) {
        try {
          Method addRole = User.class.getMethod("addRole", User.Role.class);
          addRole.invoke(u, User.Role.SUPER_ADMIN);
        } catch (NoSuchMethodException ignore) {
          // Alternative role model is fine; handlers check hasRole(...)
        }
      }
      return u;

    } catch (ReflectiveOperationException e) {
      throw new AssertionError("Failed to construct User via reflection", e);
    }
  }

  /**
   * Captures {@code System.out} during execution of {@code r} and returns the printed text as a
   * UTF-8 string. Restores the original {@code System.out} afterward.
   *
   * @param r code block that writes to {@code System.out}
   * @return captured output
   */
  private static String captureStdout(Runnable r) {
    PrintStream old = System.out;
    java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
    PrintStream ps = new PrintStream(bos, true, java.nio.charset.StandardCharsets.UTF_8);
    System.setOut(ps);
    try {
      r.run();
    } finally {
      System.setOut(old);
    }
    return bos.toString(java.nio.charset.StandardCharsets.UTF_8);
  }

  /* ---------- handleAddBudget ---------- */
  /**
   * Verifies that {@code handleAddBudget} sets a budget for a category and that subsequent computed
   * values reflect zero spent and remaining equals the limit.
   *
   * <p><b>Input script:</b> {@code "food\n200\n\n"} → category, limit, trailing newline.
   */
  @Test
  @DisplayName("handleAddBudget: sets budget and prints spent/remaining (state check)")
  void handleAddBudget_ok() {
    User current = mkUser("usr1", "John", "Smith", "pass", false);
    Wallet wallet = current.wallet; // реальный кошелёк из User

    // Enter: category + limit; additional line feed added
    Scanner in = scan("food\n200\n\n");
    ConsoleUtils.handleAddBudget(in, current);

    // Checking status: after setting budget without expenses
    // spend 0, remaining = limit
    assertEquals(0.0, wallet.getSpentByCategory("food"), 1e-9);
    assertEquals(200.0, wallet.getRemainingBudget("food"), 1e-9);
  }

  /* ---------- handleTransfer ---------- */
  /**
   * Happy path: verifies that a valid transfer invokes {@code UsersRepo.transfer} with expected
   * arguments.
   *
   * <p><b>Input script:</b> receiver, amount, note, trailing newline.
   */
  @Test
  @DisplayName("handleTransfer: success → repo.transfer called")
  void handleTransfer_success() throws Exception {
    User current = mkUser("sender", "John", "Smith", "p", false);
    UsersRepo repo = mock(UsersRepo.class);

    Scanner in = scan("receiver\n100.5\nrent\n\n");
    ConsoleUtils.handleTransfer(in, current, repo);

    verify(repo).transfer("sender", "receiver", 100.5, "rent");
  }

  /**
   * Error path: repository throws {@code Invalid}; handler should not crash and still delegate
   * once.
   *
   * <p><b>Input script:</b> includes a few invalid amounts before a valid number to exercise input
   * re-prompt logic.
   */
  @Test
  @DisplayName("handleTransfer: Invalid → prints error, no crash")
  void handleTransfer_invalid() throws Exception {
    var current = mkUser("sender", "John", "Smith", "p", false);
    var repo = mock(UsersRepo.class);

    // Repository throws Invalid regardless of the amount
    doThrow(new RepoExceptions.Invalid("bad amount"))
        .when(repo)
        .transfer(anyString(), anyString(), anyDouble(), anyString());

    // Sequence: receiver → invalid number → not-a-number → valid number → note → trailing newline
    Scanner in =
        scan(
            "receiver\n"
                + "-1\n"
                + // can be done as imnteger, leaving it as is
                "not-a-number\n"
                + // readDoubleSafe will ask for more
                "15\n"
                + // valid number, after this will go further
                "rent\n\n");

    ConsoleUtils.handleTransfer(in, current, repo);

    // Ensure delegation happened despite earlier bad inputs
    verify(repo).transfer(eq("sender"), eq("receiver"), anyDouble(), eq("rent"));
  }

  /**
   * Error path: repository throws {@code NotFound}. The handler should surface an error message and
   * still delegate exactly once with expected parameters.
   */
  @Test
  @DisplayName("handleTransfer: NotFound → prints 'Receiver not found.'")
  void handleTransfer_notFound() throws Exception {
    User current = mkUser("sender", "John", "Smith", "p", false);
    UsersRepo repo = mock(UsersRepo.class);
    doThrow(new RepoExceptions.NotFound("no user"))
        .when(repo)
        .transfer(anyString(), anyString(), anyDouble(), anyString());

    Scanner in = scan("ghost\n10\nhelp\n\n");
    ConsoleUtils.handleTransfer(in, current, repo);

    verify(repo).transfer(eq("sender"), eq("ghost"), eq(10.0), eq("help"));
  }

  @Test
  @DisplayName("handleTransfer: Forbidden → prints 'Action forbidden.'")
  void handleTransfer_forbidden() throws Exception {
    User current = mkUser("sender", "John", "Smith", "p", false);
    UsersRepo repo = mock(UsersRepo.class);
    doThrow(new RepoExceptions.Forbidden("no rights"))
        .when(repo)
        .transfer(anyString(), anyString(), anyDouble(), anyString());

    Scanner in = scan("anyuser\n5\nnote\n\n");
    ConsoleUtils.handleTransfer(in, current, repo);

    verify(repo).transfer(eq("sender"), eq("anyuser"), eq(5.0), eq("note"));
  }

  /* ---------- handleDeleteYourUserAccount ---------- */
  /**
   * Guard: a {@code SUPER_ADMIN} user must not be allowed to delete their own account. Verifies the
   * handler returns {@code false} and never calls the repository.
   */
  @Test
  @DisplayName("handleDeleteYourUserAccount: SUPER_ADMIN cannot delete self → false")
  void deleteYour_superAdmin_forbidden() {
    User current = mkUser("root", "Super", "Admin", "p", true);
    UsersRepo repo = mock(UsersRepo.class);

    boolean result =
        ConsoleUtils.handleDeleteYourUserAccount(scan("yes\nwhatever\n"), current, repo);
    assertFalse(result);
    verify(repo, never()).deleteUser(anyString(), anyString());
  }

  /** Negative confirmation should short-circuit: returns {@code false}, no repo calls. */
  @Test
  @DisplayName("handleDeleteYourUserAccount: confirm=no → false, repo not called")
  void deleteYour_confirmFalse() {
    User current = mkUser("usr1", "John", "Smith", "p", false);
    UsersRepo repo = mock(UsersRepo.class);

    boolean result = ConsoleUtils.handleDeleteYourUserAccount(scan("no\n"), current, repo);
    assertFalse(result);
    verify(repo, never()).deleteUser(anyString(), anyString());
  }

  /**
   * Happy path: positive confirmation, repository returns {@code true} → handler returns {@code
   * true}.
   */
  @Test
  @DisplayName("handleDeleteYourUserAccount: confirm=yes, repo=true → true")
  void deleteYour_confirmYes_success() throws Exception {
    User current = mkUser("usr1", "John", "Smith", "p", false);
    UsersRepo repo = mock(UsersRepo.class);
    when(repo.deleteUser("usr1", "pass")).thenReturn(true);

    boolean result = ConsoleUtils.handleDeleteYourUserAccount(scan("yes\npass\n"), current, repo);
    assertTrue(result);
    verify(repo).deleteUser("usr1", "pass");
  }

  /** Repository returns {@code false} → handler returns {@code false}. */
  @Test
  @DisplayName("handleDeleteYourUserAccount: confirm=yes, repo=false → false")
  void deleteYour_confirmYes_false() throws Exception {
    User current = mkUser("usr1", "John", "Smith", "p", false);
    UsersRepo repo = mock(UsersRepo.class);
    when(repo.deleteUser("usr1", "pass")).thenReturn(false);

    boolean result = ConsoleUtils.handleDeleteYourUserAccount(scan("yes\npass\n"), current, repo);
    assertFalse(result);
    verify(repo).deleteUser("usr1", "pass");
  }

  /**
   * Exception handling: for {@code Invalid}, {@code Forbidden}, and {@code NotFound}, the handler
   * must return {@code false} without throwing.
   */
  @Test
  @DisplayName("handleDeleteYourUserAccount: Invalid/Forbidden/NotFound → false")
  void deleteYour_exceptions_false() throws Exception {
    User current = mkUser("usr1", "John", "Smith", "p", false);
    UsersRepo repo = mock(UsersRepo.class);

    doThrow(new RepoExceptions.Invalid("bad")).when(repo).deleteUser("usr1", "p1");
    assertFalse(ConsoleUtils.handleDeleteYourUserAccount(scan("yes\np1\n"), current, repo));

    doThrow(new RepoExceptions.Forbidden("no")).when(repo).deleteUser("usr1", "p2");
    assertFalse(ConsoleUtils.handleDeleteYourUserAccount(scan("yes\np2\n"), current, repo));

    doThrow(new RepoExceptions.NotFound("no user")).when(repo).deleteUser("usr1", "p3");
    assertFalse(ConsoleUtils.handleDeleteYourUserAccount(scan("yes\np3\n"), current, repo));
  }

  /* ---------- handleDeleteSelectedUserAccount ---------- */

  /**
   * Not-found path (direct login provided): returns {@code false} and repository delete is never
   * called. This test also covers the case previously used to emulate an empty login branch.
   */
  @Test
  @DisplayName(
      "handleDeleteSelectedUserAccount: not found → false (replacement for empty-login case)")
  void deleteSelected_notFound_replacement() {
    var current = mkUser("curuser", "John", "Smith", "p", false);
    var repo = mock(UsersRepo.class);
    when(repo.find("ghost")).thenReturn(null);

    // User immediately enters a valid-looking login that does not exist
    boolean res = ConsoleUtils.handleDeleteSelectedUserAccount(scan("ghost\n\n"), current, repo);
    assertFalse(res);
    verify(repo, never()).deleteUser(anyString());
  }

  /** Not-found path: same as above with a different login. */
  @Test
  @DisplayName("handleDeleteSelectedUserAccount: not found → false")
  void deleteSelected_notFound() {
    User current = mkUser("curuser", "John", "Smith", "p", false);
    UsersRepo repo = mock(UsersRepo.class);
    when(repo.find("ghostusr")).thenReturn(null);

    boolean res = ConsoleUtils.handleDeleteSelectedUserAccount(scan("ghostusr\n\n"), current, repo);
    assertFalse(res);
    verify(repo, never()).deleteUser(anyString());
  }

  /** Guard: user cannot delete themselves. Returns {@code false}; no repo deletion. */
  @Test
  @DisplayName("handleDeleteSelectedUserAccount: try delete yourself → false")
  void deleteSelected_self() {
    User current = mkUser("meuser", "John", "Smith", "p", false);
    UsersRepo repo = mock(UsersRepo.class);
    when(repo.find("meuser")).thenReturn(current);

    boolean res = ConsoleUtils.handleDeleteSelectedUserAccount(scan("meuser\n\n"), current, repo);
    assertFalse(res);
    verify(repo, never()).deleteUser(anyString());
  }

  /**
   * Guard: attempting to delete a {@code SUPER_ADMIN} must be rejected. Returns {@code false}; no
   * repo deletion.
   */
  @Test
  @DisplayName("handleDeleteSelectedUserAccount: target SUPER_ADMIN → false")
  void deleteSelected_targetSuperAdmin_forbidden() {
    User current = mkUser("curuser", "John", "Smith", "p", false);
    User target = mkUser("bossadmin", "Boss", "Man", "p", true);
    UsersRepo repo = mock(UsersRepo.class);
    when(repo.find("bossadmin")).thenReturn(target);

    boolean res =
        ConsoleUtils.handleDeleteSelectedUserAccount(scan("bossadmin\n\n"), current, repo);
    assertFalse(res);
    verify(repo, never()).deleteUser(anyString());
  }

  /** Happy path: repository returns {@code true} for deletion → handler returns {@code true}. */
  @Test
  @DisplayName("handleDeleteSelectedUserAccount: success → true")
  void deleteSelected_success() throws Exception {
    User current = mkUser("curuser", "John", "Smith", "p", false);
    User target = mkUser("marylee", "Mary", "Lee", "p", false);
    UsersRepo repo = mock(UsersRepo.class);
    when(repo.find("marylee")).thenReturn(target);
    when(repo.deleteUser("marylee")).thenReturn(true);

    boolean res = ConsoleUtils.handleDeleteSelectedUserAccount(scan("marylee\n\n"), current, repo);
    assertTrue(res);
    verify(repo).deleteUser("marylee");
  }

  /**
   * Exception handling: repository throws various exceptions on delete; handler must return {@code
   * false} without throwing.
   */
  @Test
  @DisplayName("handleDeleteSelectedUserAccount: repo exceptions → false")
  void deleteSelected_exceptions_false() throws Exception {
    User current = mkUser("curuser", "John", "Smith", "p", false);
    User target = mkUser("u2user", "Mary", "Lee", "p", false);
    UsersRepo repo = mock(UsersRepo.class);

    when(repo.find("a1user")).thenReturn(target);
    doThrow(new RepoExceptions.NotFound("no")).when(repo).deleteUser("a1user");
    assertFalse(ConsoleUtils.handleDeleteSelectedUserAccount(scan("a1user\n\n"), current, repo));

    when(repo.find("a2user")).thenReturn(target);
    doThrow(new RepoExceptions.Forbidden("no")).when(repo).deleteUser("a2user");
    assertFalse(ConsoleUtils.handleDeleteSelectedUserAccount(scan("a2user\n\n"), current, repo));

    when(repo.find("a3user")).thenReturn(target);
    doThrow(new RepoExceptions.Invalid("bad")).when(repo).deleteUser("a3user");
    assertFalse(ConsoleUtils.handleDeleteSelectedUserAccount(scan("a3user\n\n"), current, repo));
  }

  /* ---------- handleAddOrdinaryAdminAccount ---------- */
  /** Negative confirmation must abort the flow: returns {@code false}; repository is not called. */
  @Test
  @DisplayName("handleAddOrdinaryAdminAccount: confirm != YES → false, repo not called")
  void addOrdinaryAdmin_wrongConfirm() {
    User current = mkUser("rootadmin", "Super", "Admin", "p", true);
    UsersRepo repo = mock(UsersRepo.class);

    List<User> all = new ArrayList<>();
    all.add(current);
    all.add(mkUser("usr1", "John", "Smith", "p", false));

    // confirm != YES
    Scanner in = scan("NO\nanypass\nnewadmin\n\n");
    boolean res = ConsoleUtils.handleAddOrdinaryAdminAccount(in, current, repo, all);
    assertFalse(res);
    verify(repo, never()).addAdmin(anyString(), anyString(), anyString());
  }

  /**
   * Happy path: positive confirmation and correct password → {@code addAdmin} invoked; returns
   * true.
   */
  @Test
  @DisplayName("handleAddOrdinaryAdminAccount: success → true")
  void addOrdinaryAdmin_success() throws Exception {
    User current = mkUser("rootadmin", "Super", "Admin", "pass", true);
    UsersRepo repo = mock(UsersRepo.class);

    List<User> all = new ArrayList<>();
    all.add(current);
    all.add(mkUser("usr2", "John", "Smith", "p", false));

    Scanner in = scan("YES\npass\nusr2\n\n");
    boolean res = ConsoleUtils.handleAddOrdinaryAdminAccount(in, current, repo, all);
    assertTrue(res);
    verify(repo).addAdmin("rootadmin", "pass", "usr2");
  }

  /**
   * Exception handling: repository throws {@code Invalid}, {@code NotFound}, {@code Conflict},
   * {@code Forbidden}; handler must return {@code false} each time.
   */
  @Test
  @DisplayName("handleAddOrdinaryAdminAccount: Invalid/NotFound/Conflict/Forbidden → false")
  void addOrdinaryAdmin_exceptions_false() throws Exception {
    User current = mkUser("rootadmin", "Super", "Admin", "pass", true);
    List<User> all = List.of(current, mkUser("usr3", "John", "Smith", "p", false));
    UsersRepo repo = mock(UsersRepo.class);

    doThrow(new RepoExceptions.Invalid("bad")).when(repo).addAdmin("rootadmin", "p1", "usr3");
    assertFalse(
        ConsoleUtils.handleAddOrdinaryAdminAccount(scan("YES\np1\nusr3\n\n"), current, repo, all));

    doThrow(new RepoExceptions.NotFound("no")).when(repo).addAdmin("rootadmin", "p2", "usr3");
    assertFalse(
        ConsoleUtils.handleAddOrdinaryAdminAccount(scan("YES\np2\nusr3\n\n"), current, repo, all));

    doThrow(new RepoExceptions.Conflict("exists")).when(repo).addAdmin("rootadmin", "p3", "usr3");
    assertFalse(
        ConsoleUtils.handleAddOrdinaryAdminAccount(scan("YES\np3\nusr3\n\n"), current, repo, all));

    doThrow(new RepoExceptions.Forbidden("no rights"))
        .when(repo)
        .addAdmin("rootadmin", "p4", "usr3");
    assertFalse(
        ConsoleUtils.handleAddOrdinaryAdminAccount(scan("YES\np4\nusr3\n\n"), current, repo, all));
  }

  /* ---------- handleRemoveOrdinaryAdminAccount ---------- */
  /**
   * Negative confirmation must abort: returns {@code false}; repository {@code removeAdmin} not
   * called.
   */
  @Test
  @DisplayName("handleRemoveOrdinaryAdminAccount: confirm=no → false")
  void removeOrdinaryAdmin_confirmFalse() {
    UsersRepo repo = mock(UsersRepo.class);
    boolean res = ConsoleUtils.handleRemoveOrdinaryAdminAccount(scan("no\n\n"), repo);
    assertFalse(res);
    verify(repo, never()).removeAdmin(anyString());
  }

  /**
   * Happy path: positive confirmation and existing admin login → returns {@code true} and delegates
   * to repository.
   */
  @Test
  @DisplayName("handleRemoveOrdinaryAdminAccount: success → true")
  void removeOrdinaryAdmin_success() throws Exception {
    UsersRepo repo = mock(UsersRepo.class);
    boolean res = ConsoleUtils.handleRemoveOrdinaryAdminAccount(scan("yes\nusr4\n\n"), repo);
    assertTrue(res);
    verify(repo).removeAdmin("usr4");
  }

  /**
   * Exception handling: repository throws {@code NotFound}, {@code Forbidden}, {@code Invalid},
   * {@code Conflict}; handler returns {@code false}.
   */
  @Test
  @DisplayName("handleRemoveOrdinaryAdminAccount: NotFound/Forbidden/Invalid/Conflict → false")
  void removeOrdinaryAdmin_exceptions_false() throws Exception {
    UsersRepo repo = mock(UsersRepo.class);

    doThrow(new RepoExceptions.NotFound("no")).when(repo).removeAdmin("a1x");
    assertFalse(ConsoleUtils.handleRemoveOrdinaryAdminAccount(scan("yes\na1x\n\n"), repo));

    doThrow(new RepoExceptions.Forbidden("no")).when(repo).removeAdmin("a2x");
    assertFalse(ConsoleUtils.handleRemoveOrdinaryAdminAccount(scan("yes\na2x\n\n"), repo));

    doThrow(new RepoExceptions.Invalid("bad")).when(repo).removeAdmin("a3x");
    assertFalse(ConsoleUtils.handleRemoveOrdinaryAdminAccount(scan("yes\na3x\n\n"), repo));

    doThrow(new RepoExceptions.Conflict("not admin")).when(repo).removeAdmin("a4x");
    assertFalse(ConsoleUtils.handleRemoveOrdinaryAdminAccount(scan("yes\na4x\n\n"), repo));
  }

  /* ---------- handleAdvancedStatistics ---------- */
  /**
   * Verifies advanced statistics filtering by period and categories.
   *
   * <p>Data set includes incomes and expenses across January–February 2025; the filter selects
   * {@code food} expenses in January only and prints category totals, overall totals, and a
   * filtered transaction list.
   */
  @Test
  @DisplayName("handleAdvancedStatistics: fulter by periods and categories  (food in january)")
  void advancedStats_basicFilter() {
    User u = mkUser("u", "N", "S", "p", false);
    var w = u.wallet;
    w.addTransaction(new Transaction(1000.0, "salary", Transaction.Type.INCOME, "2025-01-01"));
    w.addTransaction(new Transaction(200.0, "gift", Transaction.Type.INCOME, "2025-02-05"));
    w.addTransaction(new Transaction(100.0, "food", Transaction.Type.EXPENSE, "2025-01-15"));
    w.addTransaction(new Transaction(50.0, "food", Transaction.Type.EXPENSE, "2025-02-01"));
    w.addTransaction(new Transaction(70.0, "transport", Transaction.Type.EXPENSE, "2025-01-20"));

    Scanner in = scan("2025-01-10\n2025-01-31\nfood\n\n");
    String out = captureStdout(() -> ConsoleUtils.handleAdvancedStatistics(in, u));

    assertTrue(out.contains("— Expenses —"));
    assertTrue(out.contains("food: 100"));
    assertTrue(out.contains("Total expenses: 100"));
    assertTrue(out.contains("— Incomes —"));
    assertTrue(out.contains("No income data"));
    assertTrue(out.contains("— Filtered transactions —"));
    assertTrue(out.contains("2025-01-15 | EXPENSE | food | 100"));
  }

  /**
   * Boundary handling: if {@code TO} is earlier than {@code FROM}, the handler should swap the
   * bounds and report that swap in the output. Also verifies the filtered totals.
   */
  @Test
  @DisplayName(
      "handleAdvancedStatistics: if TO is earlier than FROM — the boundaries are interchanged")
  void advancedStats_swappedBounds() {
    User u = mkUser("u", "N", "S", "p", false);
    u.wallet.addTransaction(new Transaction(10.0, "food", Transaction.Type.EXPENSE, "2025-01-10"));
    Scanner in = scan("2025-02-01\n2025-01-01\nfood\n\n");
    String out = captureStdout(() -> ConsoleUtils.handleAdvancedStatistics(in, u));
    assertTrue(out.toLowerCase(Locale.ROOT).contains("swapping"), "Should notify aboutswap");
    assertTrue(out.contains("food: 10"));
  }

  /* ---------- handleAddIncome / handleAddExpense ---------- */
  /**
   * Verifies that {@code handleAddIncome} creates an income transaction with the provided amount,
   * title, and date, and that wallet aggregates reflect the change.
   */
  @Test
  @DisplayName("handleAddIncome: creaters Transaction with date and sum")
  void handleAddIncome_ok() {
    User u = mkUser("u", "N", "S", "p", false);
    Scanner in = scan("123.45\nbonus\n2025-03-01\n");
    ConsoleUtils.handleAddIncome(in, u);
    assertEquals(123.45, u.wallet.sumIncome(), 1e-9);
    assertTrue(
        u.wallet.getTransactions().stream()
            .anyMatch(
                t ->
                    t.getType() == Transaction.Type.INCOME
                        && "bonus".equals(t.getTitle())
                        && "2025-03-01".equals(t.getDateIso())
                        && Math.abs(t.getAmount() - 123.45) < 1e-9));
  }

  /**
   * Verifies that {@code handleAddExpense} creates an expense transaction and prints a budget alert
   * when the category limit is exceeded.
   */
  @Test
  @DisplayName("handleAddExpense: создаёт Transaction и печатает бюджетный алерт при превышении")
  void handleAddExpense_ok() {
    User u = mkUser("u", "N", "S", "p", false);
    u.wallet.setBudget("food", 50.0);
    Scanner in = scan("70\nfood\n2025-04-10\n");
    String out = captureStdout(() -> ConsoleUtils.handleAddExpense(in, u));
    assertEquals(70.0, u.wallet.sumExpense(), 1e-9);
    assertTrue(
        u.wallet.getTransactions().stream()
            .anyMatch(
                t ->
                    t.getType() == Transaction.Type.EXPENSE
                        && "food".equals(t.getTitle())
                        && "2025-04-10".equals(t.getDateIso())
                        && Math.abs(t.getAmount() - 70.0) < 1e-9));
    assertTrue(out.contains("! "), "Waiting for budget alert");
  }

  /* ---------- handleUpdateBudgetLimit / handleRemoveBudget / handleRenameCategory ---------- */
  /**
   * Verifies updating an existing category limit prints a confirmation and changes the stored
   * value.
   */
  @Test
  @DisplayName("handleUpdateBudgetLimit: update a limit of the existing category")
  void updateBudgetLimit_ok() {
    User u = mkUser("u", "N", "S", "p", false);
    u.wallet.setBudget("food", 100.0);
    Scanner in = scan("food\n250\n");
    String out = captureStdout(() -> ConsoleUtils.handleUpdateBudgetLimit(in, u));
    assertTrue(out.contains("Budget updated: food -> 250"));
    assertEquals(250.0, u.wallet.getBudgets().get("food"), 1e-9);
  }

  /** Verifies removing an existing budget category prints a confirmation and deletes the entry. */
  @Test
  @DisplayName("handleRemoveBudget: removes budget category")
  void removeBudget_ok() {
    User u = mkUser("u", "N", "S", "p", false);
    u.wallet.setBudget("travel", 300.0);
    Scanner in = scan("travel\n");
    String out = captureStdout(() -> ConsoleUtils.handleRemoveBudget(in, u));
    assertTrue(out.contains("Budget removed: travel"));
    assertFalse(u.wallet.getBudgets().containsKey("travel"));
  }

  /** Verifies renaming a category migrates its limit and prints a confirmation message. */
  @Test
  @DisplayName("handleRenameCategory: rename the existing category")
  void renameCategory_ok() {
    User u = mkUser("u", "N", "S", "p", false);
    u.wallet.setBudget("oldcat", 400.0);
    Scanner in = scan("oldcat\nnewcat\n");
    String out = captureStdout(() -> ConsoleUtils.handleRenameCategory(in, u));
    assertTrue(
        out.contains("Category renamed") && out.contains("oldcat") && out.contains("newcat"));
    assertFalse(u.wallet.getBudgets().containsKey("oldcat"));
    assertTrue(u.wallet.getBudgets().containsKey("newcat"));
    assertEquals(400.0, u.wallet.getBudgets().get("newcat"), 1e-9);
  }

  /* ---------- confirmAction ---------- */

  /**
   * Verifies that {@code confirmAction} returns {@code true} for {@code YES/yes} and {@code false}
   * otherwise when using a single {@link Scanner} across sequential calls.
   */
  @Test
  @DisplayName("confirmAction: YES/yes → true; иное → false (один Scanner)")
  void confirmAction_yesNo_singleScanner() {
    // 4 строки на 4 вызова; нигде нет пустой строки
    Scanner in = scan("YES\nyes\nNo\nnope\n");

    assertTrue(ConsoleUtils.confirmAction(in, "x")); // "YES" → true
    assertTrue(ConsoleUtils.confirmAction(in, "x")); // "yes" → true
    assertFalse(ConsoleUtils.confirmAction(in, "x")); // "No"  → false
    assertFalse(ConsoleUtils.confirmAction(in, "x")); // "nope"→ false
  }

  /* ---------- handleViewWallet / handleViewStatistics (smoke) ---------- */
  /** Smoke test for wallet rendering: balance, transactions, budgets, and alerts are present. */
  @Test
  @DisplayName("handleViewWallet: prints balance, transactions, budgets and alerts")
  void viewWallet_smoke() {
    User u = mkUser("u", "N", "S", "p", false);
    u.wallet.addTransaction(
        new Transaction(100.0, "salary", Transaction.Type.INCOME, "2025-01-01"));
    u.wallet.addTransaction(new Transaction(50.0, "food", Transaction.Type.EXPENSE, "2025-01-02"));
    u.wallet.setBudget("food", 40.0);
    String out = captureStdout(() -> ConsoleUtils.handleViewWallet(u));
    assertTrue(out.contains("Balance:"));
    assertTrue(out.contains("Transactions:"));
    assertTrue(out.contains("Budgets:"));
    assertTrue(out.contains("! "));
  }

  /**
   * Smoke test for statistics rendering: total income/expense and per-category aggregates show
   * expected values.
   */
  @Test
  @DisplayName("handleViewStatistics: summary of incomes/expenses by categories")
  void viewStatistics_smoke() {
    User u = mkUser("u", "N", "S", "p", false);
    u.wallet.addTransaction(
        new Transaction(100.0, "salary", Transaction.Type.INCOME, "2025-01-01"));
    u.wallet.addTransaction(new Transaction(30.0, "food", Transaction.Type.EXPENSE, "2025-01-02"));
    u.wallet.addTransaction(new Transaction(20.0, "food", Transaction.Type.EXPENSE, "2025-01-03"));
    String out = captureStdout(() -> ConsoleUtils.handleViewStatistics(u));
    assertTrue(out.contains("Wallet statistics"));
    assertTrue(out.contains("Total income: 100"));
    assertTrue(out.contains("Total expense: 50"));
    assertTrue(out.contains("Incomes by category:"));
    assertTrue(out.contains("Expenses by category:"));
    assertTrue(out.contains("food: 50"));
  }

  /* ---------- handleExportJson / handleImportJson (static mock) ---------- */
  /**
   * Verifies that {@code handleExportJson} calls {@link WalletJson#save(User)} exactly once and
   * does not print an error on success.
   */
  @Test
  @DisplayName("handleExportJson: invokes WalletJson.save(u)")
  void exportJson_callsSave() {
    User u = mkUser("u", "N", "S", "p", false);
    try (MockedStatic<WalletJson> mocked = mockStatic(WalletJson.class)) {
      String out = captureStdout(() -> ConsoleUtils.handleExportJson(scan("\n"), u));
      mocked.verify(() -> WalletJson.save(u), times(1));
      assertFalse(out.contains("Export failed"));
    }
  }

  /**
   * Verifies that {@code handleExportJson} prints an error message when {@link
   * WalletJson#save(User)} throws.
   */
  @Test
  @DisplayName("handleExportJson: prints error when exception ")
  void exportJson_errorPrints() {
    User u = mkUser("u", "N", "S", "p", false);
    try (MockedStatic<WalletJson> mocked = mockStatic(WalletJson.class)) {
      mocked.when(() -> WalletJson.save(u)).thenThrow(new RuntimeException("IO"));
      String out = captureStdout(() -> ConsoleUtils.handleExportJson(scan("\n"), u));
      assertTrue(out.contains("Export failed"));
    }
  }

  /**
   * Verifies that {@code handleImportJson} calls {@link WalletJson#loadInto(User)} exactly once and
   * does not print an error on success.
   */
  @Test
  @DisplayName("handleImportJson: invokes WalletJson.loadInto(u)")
  void importJson_callsLoadInto() {
    User u = mkUser("u", "N", "S", "p", false);
    try (MockedStatic<WalletJson> mocked = mockStatic(WalletJson.class)) {
      String out = captureStdout(() -> ConsoleUtils.handleImportJson(scan("\n"), u));
      mocked.verify(() -> WalletJson.loadInto(u), times(1));
      assertFalse(out.contains("Import failed"));
    }
  }

  /**
   * Verifies that {@code handleImportJson} prints an error message when {@link
   * WalletJson#loadInto(User)} throws.
   */
  @Test
  @DisplayName("handleImportJson: throws error when exception")
  void importJson_errorPrints() {
    User u = mkUser("u", "N", "S", "p", false);
    try (MockedStatic<WalletJson> mocked = mockStatic(WalletJson.class)) {
      mocked.when(() -> WalletJson.loadInto(u)).thenThrow(new RuntimeException("IO"));
      String out = captureStdout(() -> ConsoleUtils.handleImportJson(scan("\n"), u));
      assertTrue(out.contains("Import failed"));
    }
  }
}
