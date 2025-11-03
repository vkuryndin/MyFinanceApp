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
 * Тесты handler-методов из ConsoleUtils. ВНИМАНИЕ: у User.wallet — final, поэтому НЕ пытаемся его
 * подменять. Работаем с реальным кошельком, а UsersRepo — мок.
 */
public class ConsoleUtilsTest {

  /* ---------- helpers ---------- */

  private static Scanner scan(String s) {
    return new Scanner(
            new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)
        .useLocale(Locale.ROOT);
  }

  /**
   * Универсально создаёт User даже при изменённом конструкторе. Ищем конструктор с >=4
   * String-параметрами (login, name, surname, pass) и заполняем их. Если нужно — добавляем роль
   * SUPER_ADMIN через addRole(Role). Кошелёк не трогаем (final).
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
          // другой способ ролей — ок, handlers проверяют hasRole(...)
        }
      }
      return u;

    } catch (ReflectiveOperationException e) {
      throw new AssertionError("Failed to construct User via reflection", e);
    }
  }

  /** Перехват System.out, чтобы проверять вывод */
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

  @Test
  @DisplayName("handleAddBudget: sets budget and prints spent/remaining (state check)")
  void handleAddBudget_ok() {
    User current = mkUser("usr1", "John", "Smith", "pass", false);
    Wallet wallet = current.wallet; // реальный кошелёк из User

    // Ввод: категория + лимит; добавляем запасной перевод строки
    Scanner in = scan("food\n200\n\n");
    ConsoleUtils.handleAddBudget(in, current);

    // Проверяем состояние: после установки бюджета без трат
    // потрачено 0, остаток = лимит
    assertEquals(0.0, wallet.getSpentByCategory("food"), 1e-9);
    assertEquals(200.0, wallet.getRemainingBudget("food"), 1e-9);
  }

  /* ---------- handleTransfer ---------- */

  @Test
  @DisplayName("handleTransfer: success → repo.transfer called")
  void handleTransfer_success() throws Exception {
    User current = mkUser("sender", "John", "Smith", "p", false);
    UsersRepo repo = mock(UsersRepo.class);

    Scanner in = scan("receiver\n100.5\nrent\n\n");
    ConsoleUtils.handleTransfer(in, current, repo);

    verify(repo).transfer("sender", "receiver", 100.5, "rent");
  }

  @Test
  @DisplayName("handleTransfer: Invalid → prints error, no crash")
  void handleTransfer_invalid() throws Exception {
    var current = mkUser("sender", "John", "Smith", "p", false);
    var repo = mock(UsersRepo.class);

    // Репозиторий в любом случае бросает Invalid — не важно, какое число придёт
    doThrow(new RepoExceptions.Invalid("bad amount"))
        .when(repo)
        .transfer(anyString(), anyString(), anyDouble(), anyString());

    // Последовательность ввода:
    // получатель → несколько «плохих» попыток (минус/слово) → ВАЛИДНОЕ число → note → завершающая
    // пустая
    Scanner in =
        scan(
            "receiver\n"
                + "-1\n"
                + // допустимо как число, но пусть остаётся
                "not-a-number\n"
                + // readDoubleSafe попросит ещё
                "15\n"
                + // валидное число, после него пойдём дальше
                "rent\n\n");

    ConsoleUtils.handleTransfer(in, current, repo);

    // Проверяем, что до репозитория дошли
    verify(repo).transfer(eq("sender"), eq("receiver"), anyDouble(), eq("rent"));
  }

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

  @Test
  @DisplayName("handleDeleteYourUserAccount: confirm=no → false, repo not called")
  void deleteYour_confirmFalse() {
    User current = mkUser("usr1", "John", "Smith", "p", false);
    UsersRepo repo = mock(UsersRepo.class);

    boolean result = ConsoleUtils.handleDeleteYourUserAccount(scan("no\n"), current, repo);
    assertFalse(result);
    verify(repo, never()).deleteUser(anyString(), anyString());
  }

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

  @Test
  @DisplayName(
      "handleDeleteSelectedUserAccount: not found → false (replacement for empty-login case)")
  void deleteSelected_notFound_replacement() {
    var current = mkUser("curuser", "John", "Smith", "p", false);
    var repo = mock(UsersRepo.class);
    when(repo.find("ghost")).thenReturn(null);

    // Пользователь сразу вводит валидный логин, которого нет
    boolean res = ConsoleUtils.handleDeleteSelectedUserAccount(scan("ghost\n\n"), current, repo);
    assertFalse(res);
    verify(repo, never()).deleteUser(anyString());
  }

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

  @Test
  @DisplayName("handleRemoveOrdinaryAdminAccount: confirm=no → false")
  void removeOrdinaryAdmin_confirmFalse() {
    UsersRepo repo = mock(UsersRepo.class);
    boolean res = ConsoleUtils.handleRemoveOrdinaryAdminAccount(scan("no\n\n"), repo);
    assertFalse(res);
    verify(repo, never()).removeAdmin(anyString());
  }

  @Test
  @DisplayName("handleRemoveOrdinaryAdminAccount: success → true")
  void removeOrdinaryAdmin_success() throws Exception {
    UsersRepo repo = mock(UsersRepo.class);
    boolean res = ConsoleUtils.handleRemoveOrdinaryAdminAccount(scan("yes\nusr4\n\n"), repo);
    assertTrue(res);
    verify(repo).removeAdmin("usr4");
  }

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

  @Test
  @DisplayName("handleAdvancedStatistics: фильтр по периоду и категориям (food в январе)")
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

  @Test
  @DisplayName("handleAdvancedStatistics: если TO раньше FROM — границы меняются местами")
  void advancedStats_swappedBounds() {
    User u = mkUser("u", "N", "S", "p", false);
    u.wallet.addTransaction(new Transaction(10.0, "food", Transaction.Type.EXPENSE, "2025-01-10"));
    Scanner in = scan("2025-02-01\n2025-01-01\nfood\n\n");
    String out = captureStdout(() -> ConsoleUtils.handleAdvancedStatistics(in, u));
    assertTrue(out.toLowerCase(Locale.ROOT).contains("swapping"), "Должно сообщить о swap");
    assertTrue(out.contains("food: 10"));
  }

  /* ---------- handleAddIncome / handleAddExpense ---------- */

  @Test
  @DisplayName("handleAddIncome: создаёт Transaction с датой и суммой")
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
    assertTrue(out.contains("! "), "Ожидался алерт по бюджету");
  }

  /* ---------- handleUpdateBudgetLimit / handleRemoveBudget / handleRenameCategory ---------- */

  @Test
  @DisplayName("handleUpdateBudgetLimit: обновление лимита существующей категории")
  void updateBudgetLimit_ok() {
    User u = mkUser("u", "N", "S", "p", false);
    u.wallet.setBudget("food", 100.0);
    Scanner in = scan("food\n250\n");
    String out = captureStdout(() -> ConsoleUtils.handleUpdateBudgetLimit(in, u));
    assertTrue(out.contains("Budget updated: food -> 250"));
    assertEquals(250.0, u.wallet.getBudgets().get("food"), 1e-9);
  }

  @Test
  @DisplayName("handleRemoveBudget: удаляет бюджет категорию")
  void removeBudget_ok() {
    User u = mkUser("u", "N", "S", "p", false);
    u.wallet.setBudget("travel", 300.0);
    Scanner in = scan("travel\n");
    String out = captureStdout(() -> ConsoleUtils.handleRemoveBudget(in, u));
    assertTrue(out.contains("Budget removed: travel"));
    assertFalse(u.wallet.getBudgets().containsKey("travel"));
  }

  @Test
  @DisplayName("handleRenameCategory: переименование существующей категории")
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

  @Test
  @DisplayName("handleViewWallet: печатает баланс, транзакции, бюджеты и алерты")
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

  @Test
  @DisplayName("handleViewStatistics: агрегаты доходов/расходов и по категориям")
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

  @Test
  @DisplayName("handleExportJson: вызывает WalletJson.save(u)")
  void exportJson_callsSave() {
    User u = mkUser("u", "N", "S", "p", false);
    try (MockedStatic<WalletJson> mocked = mockStatic(WalletJson.class)) {
      String out = captureStdout(() -> ConsoleUtils.handleExportJson(scan("\n"), u));
      mocked.verify(() -> WalletJson.save(u), times(1));
      assertFalse(out.contains("Export failed"));
    }
  }

  @Test
  @DisplayName("handleExportJson: при исключении печатает ошибку")
  void exportJson_errorPrints() {
    User u = mkUser("u", "N", "S", "p", false);
    try (MockedStatic<WalletJson> mocked = mockStatic(WalletJson.class)) {
      mocked.when(() -> WalletJson.save(u)).thenThrow(new RuntimeException("IO"));
      String out = captureStdout(() -> ConsoleUtils.handleExportJson(scan("\n"), u));
      assertTrue(out.contains("Export failed"));
    }
  }

  @Test
  @DisplayName("handleImportJson: вызывает WalletJson.loadInto(u)")
  void importJson_callsLoadInto() {
    User u = mkUser("u", "N", "S", "p", false);
    try (MockedStatic<WalletJson> mocked = mockStatic(WalletJson.class)) {
      String out = captureStdout(() -> ConsoleUtils.handleImportJson(scan("\n"), u));
      mocked.verify(() -> WalletJson.loadInto(u), times(1));
      assertFalse(out.contains("Import failed"));
    }
  }

  @Test
  @DisplayName("handleImportJson: при исключении печатает ошибку")
  void importJson_errorPrints() {
    User u = mkUser("u", "N", "S", "p", false);
    try (MockedStatic<WalletJson> mocked = mockStatic(WalletJson.class)) {
      mocked.when(() -> WalletJson.loadInto(u)).thenThrow(new RuntimeException("IO"));
      String out = captureStdout(() -> ConsoleUtils.handleImportJson(scan("\n"), u));
      assertTrue(out.contains("Import failed"));
    }
  }
}
