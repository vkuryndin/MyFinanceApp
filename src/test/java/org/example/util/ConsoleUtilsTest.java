package org.example.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import org.example.model.User;
import org.example.model.Wallet;
import org.example.repo.RepoExceptions;
import org.example.repo.UsersRepo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
