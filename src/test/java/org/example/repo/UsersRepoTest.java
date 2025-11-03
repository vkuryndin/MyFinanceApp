package org.example.repo;

import static org.junit.jupiter.api.Assertions.*;

import org.example.model.Transaction;
import org.example.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the UsersRepo repository class.
 *
 * <p>Tests validate core repository functionality including:
 *
 * <ul>
 *   <li><b>User registration:</b> Login normalization, validation, and deduplication
 *   <li><b>Authentication:</b> Password verification and failed login attempts
 *   <li><b>Money transfers:</b> Inter-user transfers with proper transaction recording
 *   <li><b>User deletion:</b> Simple deletion and password-protected deletion
 *   <li><b>Role management:</b> Adding and removing ADMIN roles with proper authorization
 *   <li><b>Error handling:</b> Validation of custom exceptions (Invalid, NotFound, Conflict,
 *       Forbidden)
 * </ul>
 *
 * <p>Login normalization ensures case-insensitive matching and whitespace trimming. Login
 * validation enforces format: 3-32 characters, starts with letter, alphanumeric with ._- allowed.
 *
 * <p>Tests use a fresh UsersRepo instance for each test via {@code @BeforeEach} to ensure
 * isolation.
 *
 * @see org.example.repo.UsersRepo
 * @see org.example.repo.RepoExceptions
 */
public class UsersRepoTest {

  private UsersRepo repo;

  @BeforeEach
  void setup() {
    repo = new UsersRepo();
  }

  @Test
  @DisplayName("Регистрация нормализует логин; повторная — возвращает того же пользователя")
  void registerAndNormalize() {
    User u1 = repo.register("  Ivan-123 ", "Ivan", "Petrov", "pass");
    assertEquals("ivan-123", u1.login);

    User u2 = repo.register("ivan-123", "Ivan2", "Petrov2", "pass2");
    assertSame(u1, u2);
  }

  @Test
  @DisplayName("Неверный логин — IllegalArgumentException")
  void invalidLogin() {
    assertThrows(IllegalArgumentException.class, () -> repo.register("1bad", "A", "B", "p"));
    assertThrows(
        IllegalArgumentException.class, () -> repo.register("ab", "A", "B", "p")); // < 3 символов
  }

  @Test
  @DisplayName("Аутентификация: успех и провал")
  void authenticate() {
    repo.register("userone", "A", "B", "qwerty");
    assertNotNull(repo.authenticate("userone", "qwerty"));
    assertNull(repo.authenticate("userone", "bad"));
    assertNull(repo.authenticate("unknown", "qwerty"));
  }

  @Test
  @DisplayName("Перевод между пользователями: у отправителя EXPENSE, у получателя INCOME")
  void transferHappyPath() {
    repo.register("alice", "A", "B", "1");
    repo.register("bob", "C", "D", "2");

    boolean ok = repo.transfer("alice", "bob", 150.0, "gift");
    assertTrue(ok);

    User alice = repo.find("alice");
    User bob = repo.find("bob");

    assertEquals(150.0, alice.wallet.sumExpense(), 1e-9);
    assertEquals(150.0, bob.wallet.sumIncome(), 1e-9);

    Transaction lastOut = alice.wallet.transactions.get(alice.wallet.transactions.size() - 1);
    Transaction lastIn = bob.wallet.transactions.get(bob.wallet.transactions.size() - 1);

    assertEquals(Transaction.Type.EXPENSE, lastOut.type);
    assertTrue(lastOut.title.toLowerCase().contains("transfer to bob"));
    assertTrue(lastOut.title.toLowerCase().contains("gift"));

    assertEquals(Transaction.Type.INCOME, lastIn.type);
    assertTrue(lastIn.title.toLowerCase().contains("transfer from alice"));
    assertTrue(lastIn.title.toLowerCase().contains("gift"));
  }

  @Test
  @DisplayName(
      "Переводы с ошибками: на себя, нулевая/отрицательная сумма, несуществующие пользователи")
  void transferErrors() {
    repo.register("alice", "A", "B", "1");
    repo.register("bob", "C", "D", "2");

    assertThrows(IllegalArgumentException.class, () -> repo.transfer("alice", "alice", 10, "t"));
    assertThrows(IllegalArgumentException.class, () -> repo.transfer("alice", "bob", 0, "t"));
    assertThrows(IllegalArgumentException.class, () -> repo.transfer("alice", "bob", -5, "t"));
    assertThrows(IllegalArgumentException.class, () -> repo.transfer("ghost", "bob", 10, "t"));
    assertThrows(IllegalArgumentException.class, () -> repo.transfer("alice", "ghost", 10, "t"));
  }

  @Test
  @DisplayName("Удаление пользователя: true если был, false если нет")
  void deleteUserSimple() {
    repo.register("xuser", "A", "B", "1");
    assertTrue(repo.deleteUser("xuser"));
    assertFalse(repo.deleteUser("xuser"));
  }

  @Test
  @DisplayName("Удаление с паролем: валидные/невалидные учётки")
  void deleteUserWithPassword() {
    repo.register("xuser", "A", "B", "1");
    assertTrue(repo.deleteUser("xuser", "1"));

    // уже удалён
    assertFalse(repo.deleteUser("xuser", "1"));
    // null-ы
    assertFalse(repo.deleteUser(null, "1"));
    assertFalse(repo.deleteUser("xuser", null));
  }

  @Test
  @DisplayName("Назначение и снятие админа")
  void addRemoveAdmin() {
    repo.register("rooter", "R", "R", "r");
    repo.register("userok", "U", "U", "p");

    // логинимся rooter и назначаем userok админом
    assertTrue(repo.addAdmin("rooter", "r", "userok"));
    assertTrue(repo.find("userok").hasRole(User.Role.ADMIN));

    // снимаем роль
    assertTrue(repo.removeAdmin("userok"));
    assertFalse(repo.find("userok").hasRole(User.Role.ADMIN));
  }

  @Test
  @DisplayName("addAdmin: ошибки учётки/ролей (валидные логины)")
  void addAdminErrors() {
    // используем валидные логины (>=3 символов)
    repo.register("alice", "A", "A", "p");
    repo.register("bob", "B", "B", "p");

    // неправильные креды назначающего
    assertThrows(RepoExceptions.Invalid.class, () -> repo.addAdmin("alice", "bad", "bob"));

    // несуществующий кандидат
    assertThrows(RepoExceptions.NotFound.class, () -> repo.addAdmin("alice", "p", "nope"));

    // успешное назначение
    assertTrue(repo.addAdmin("alice", "p", "bob"));

    // повторное назначение должно давать Conflict
    assertThrows(RepoExceptions.Conflict.class, () -> repo.addAdmin("alice", "p", "bob"));
  }
}
