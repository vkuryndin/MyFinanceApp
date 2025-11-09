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

  /**
   * Initializes a fresh repository for each test to preserve isolation and avoid cross-test state.
   */
  @BeforeEach
  void setup() {
    repo = new UsersRepo();
  }

  /**
   * <b>Intent:</b> Registration normalizes the login (trim + lower-case) and is idempotent for the
   * same normalized key.
   *
   * <p><b>Asserts:</b> First registration yields normalized login; repeated registration with the
   * same normalized login returns the same {@link User} instance.
   */
  @Test
  @DisplayName("Registration is normalizing the login; the second one returns the same userв")
  void registerAndNormalize() {
    User u1 = repo.register("  Ivan-123 ", "Ivan", "Petrov", "pass");
    assertEquals("ivan-123", u1.login);

    User u2 = repo.register("ivan-123", "Ivan2", "Petrov2", "pass2");
    assertSame(u1, u2);
  }

  /**
   * <b>Intent:</b> Invalid logins are rejected according to format rules.
   *
   * <p><b>Asserts:</b> Too short or non-letter-starting logins cause {@link
   * IllegalArgumentException}.
   */
  @Test
  @DisplayName("Invalid login — IllegalArgumentException")
  void invalidLogin() {
    assertThrows(IllegalArgumentException.class, () -> repo.register("1bad", "A", "B", "p"));
    assertThrows(
        IllegalArgumentException.class, () -> repo.register("ab", "A", "B", "p")); // < 3 символов
  }

  /**
   * <b>Intent:</b> Authentication returns a user on success and {@code null} on failure.
   *
   * <p><b>Asserts:</b> Correct password authenticates; wrong password or unknown login yields
   * {@code null}.
   */
  @Test
  @DisplayName("Authentications: success and failure")
  void authenticate() {
    repo.register("userone", "A", "B", "qwerty");
    assertNotNull(repo.authenticate("userone", "qwerty"));
    assertNull(repo.authenticate("userone", "bad"));
    assertNull(repo.authenticate("unknown", "qwerty"));
  }

  /**
   * <b>Intent:</b> Happy-path transfer records an {@code EXPENSE} for sender and an {@code INCOME}
   * for recipient with informative titles, and updates wallet totals symmetrically.
   *
   * <p><b>Asserts:</b> Totals: sender expense == amount; recipient income == amount. Transaction
   * types and titles contain directional markers and the memo.
   */
  @Test
  @DisplayName("Transfer between users: EXPENSE from the sender, INCOME gets the receiver")
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

    assertEquals(Transaction.Type.EXPENSE, lastOut.getType());
    assertTrue(lastOut.getTitle().toLowerCase().contains("transfer to bob"));
    assertTrue(lastOut.getTitle().toLowerCase().contains("gift"));

    assertEquals(Transaction.Type.INCOME, lastIn.getType());
    assertTrue(lastIn.getTitle().toLowerCase().contains("transfer from alice"));
    assertTrue(lastIn.getTitle().toLowerCase().contains("gift"));
  }

  /**
   * <b>Intent:</b> Invalid transfers must be rejected with {@link IllegalArgumentException}.
   *
   * <p><b>Asserts:</b> Self-transfer; zero/negative amount; unknown sender/recipient → all throw
   * {@link IllegalArgumentException}.
   */
  @Test
  @DisplayName("Transfer with errors: to myself, null/negative sum, non existent users")
  void transferErrors() {
    repo.register("alice", "A", "B", "1");
    repo.register("bob", "C", "D", "2");

    assertThrows(IllegalArgumentException.class, () -> repo.transfer("alice", "alice", 10, "t"));
    assertThrows(IllegalArgumentException.class, () -> repo.transfer("alice", "bob", 0, "t"));
    assertThrows(IllegalArgumentException.class, () -> repo.transfer("alice", "bob", -5, "t"));
    assertThrows(IllegalArgumentException.class, () -> repo.transfer("ghost", "bob", 10, "t"));
    assertThrows(IllegalArgumentException.class, () -> repo.transfer("alice", "ghost", 10, "t"));
  }

  /**
   * <b>Intent:</b> Simple deletion returns {@code true} only for an existing user and {@code false}
   * when the user does not exist.
   */
  @Test
  @DisplayName("Deleting a user: true if there was a user, false if not")
  void deleteUserSimple() {
    repo.register("xuser", "A", "B", "1");
    assertTrue(repo.deleteUser("xuser"));
    assertFalse(repo.deleteUser("xuser"));
  }

  /**
   * <b>Intent:</b> Password-guarded deletion succeeds only with valid credentials and returns
   * {@code false} for already removed or {@code null} inputs.
   */
  @Test
  @DisplayName("Deletion with a password: valid/non valid user accounts")
  void deleteUserWithPassword() {
    repo.register("xuser", "A", "B", "1");
    assertTrue(repo.deleteUser("xuser", "1"));

    // already deleted
    assertFalse(repo.deleteUser("xuser", "1"));
    // nulls
    assertFalse(repo.deleteUser(null, "1"));
    assertFalse(repo.deleteUser("xuser", null));
  }

  /**
   * <b>Intent:</b> Role management: a valid administrator can grant and later revoke {@code ADMIN}
   * from another user.
   *
   * <p><b>Asserts:</b> {@code addAdmin} succeeds and sets the role; {@code removeAdmin} succeeds
   * and clears the role.
   */
  @Test
  @DisplayName("Assign and remove ordinary admin account")
  void addRemoveAdmin() {
    repo.register("rooter", "R", "R", "r");
    repo.register("userok", "U", "U", "p");

    // log in rooter and assing userok as ordinary admin
    assertTrue(repo.addAdmin("rooter", "r", "userok"));
    assertTrue(repo.find("userok").hasRole(User.Role.ADMIN));

    // removing ordinary admin role
    assertTrue(repo.removeAdmin("userok"));
    assertFalse(repo.find("userok").hasRole(User.Role.ADMIN));
  }

  /**
   * <b>Intent:</b> {@code addAdmin} error cases with valid login formats:
   *
   * <ul>
   *   <li>Invalid credentials of the acting admin → {@link RepoExceptions.Invalid}
   *   <li>Non-existent candidate → {@link RepoExceptions.NotFound}
   *   <li>Repeated assignment → {@link RepoExceptions.Conflict}
   * </ul>
   *
   * <p><b>Asserts:</b> Exceptions match the scenario; a single successful assignment returns {@code
   * true}.
   */
  @Test
  @DisplayName("addAdmin: user account errors/roles (valid logins)")
  void addAdminErrors() {
    // using valid logins (>=3 characters)
    repo.register("alice", "A", "A", "p");
    repo.register("bob", "B", "B", "p");

    // wrong credentials of the assigner
    assertThrows(RepoExceptions.Invalid.class, () -> repo.addAdmin("alice", "bad", "bob"));

    // non existing candidate
    assertThrows(RepoExceptions.NotFound.class, () -> repo.addAdmin("alice", "p", "nope"));

    // successful assignment
    assertTrue(repo.addAdmin("alice", "p", "bob"));

    // the repeat assigment should throw a conflict
    assertThrows(RepoExceptions.Conflict.class, () -> repo.addAdmin("alice", "p", "bob"));
  }
}
