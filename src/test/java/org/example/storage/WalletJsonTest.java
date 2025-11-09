package org.example.storage;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.Locale;
import org.example.model.Transaction;
import org.example.model.User;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration-style unit tests for {@link WalletJson} covering save/load behavior with both
 * explicit paths and the default location {@code data/wallet.json}.
 *
 * <p>Key scenarios:
 *
 * <ul>
 *   <li>Persisting transactions and budgets to JSON
 *   <li>Graceful handling of invalid parent paths
 *   <li>Loading with duplicate transactions (by id and signature) and mixed budget values
 *   <li>Missing files, directories passed as paths, and malformed JSON diagnostics
 * </ul>
 *
 * <p>Each test runs under a temporary working directory by overriding {@code user.dir} to ensure
 * that default-path operations are fully isolated.
 */
@DisplayName("WalletJson: save/load full coverage")
class WalletJsonTest {

  @TempDir Path tmp;
  private String oldUserDir;

  /* ================= helpers ================= */
  /**
   * Captures {@code System.out} for the duration of {@code r} and returns captured text as UTF-8.
   * Restores the original stream afterwards.
   */
  private static String captureStdOut(Runnable r) {
    PrintStream old = System.out;
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(bos, true, StandardCharsets.UTF_8);
    System.setOut(ps);
    try {
      r.run();
    } finally {
      System.setOut(old);
    }
    return bos.toString(StandardCharsets.UTF_8);
  }

  /**
   * Captures {@code System.err} for the duration of {@code r} and returns captured text as UTF-8.
   * Restores the original stream afterwards.
   */
  private static String captureStdErr(Runnable r) {
    PrintStream old = System.err;
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(bos, true, StandardCharsets.UTF_8);
    System.setErr(ps);
    try {
      r.run();
    } finally {
      System.setErr(old);
    }
    return bos.toString(StandardCharsets.UTF_8);
  }

  /**
   * Reflection-friendly {@link User} constructor used across tests. Attempts to find a constructor
   * with at least four {@code String} parameters and populate login, name, surname, and password.
   * Grants {@code SUPER_ADMIN} if requested via {@code addRole(Role)} when available.
   */
  private static User mkUser(
      String login, String name, String surname, String pass, boolean superAdmin) {
    try {
      // searching for any constructor with >=4 string parameters
      java.lang.reflect.Constructor<?> picked = null;
      for (var c : User.class.getDeclaredConstructors()) {
        int strings = 0;
        for (var t : c.getParameterTypes()) if (t == String.class) strings++;
        if (strings >= 4) {
          picked = c;
          break;
        }
      }
      if (picked == null) {
        for (var c : User.class.getDeclaredConstructors()) {
          for (var t : c.getParameterTypes()) {
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
          var addRole = User.class.getMethod("addRole", User.Role.class);
          addRole.invoke(u, User.Role.SUPER_ADMIN);
        } catch (NoSuchMethodException ignore) {
        }
      }
      return u;
    } catch (ReflectiveOperationException e) {
      throw new AssertionError("Failed to construct User via reflection", e);
    }
  }

  /**
   * Redirects {@code user.dir} to the {@link TempDir} so that calls to default locations (e.g.,
   * {@code data/wallet.json}) affect only the temporary workspace.
   */
  @BeforeEach
  void chdirToTmp() {
    // чтобы тестировать save(User)/loadInto(User) c DEFAULT_FILE=data/wallet.json в tmp
    oldUserDir = System.getProperty("user.dir");
    System.setProperty("user.dir", tmp.toString());
  }

  /**
   * Restores the original {@code user.dir} after each test to avoid leaking environment changes.
   */
  @AfterEach
  void restoreDir() {
    if (oldUserDir != null) System.setProperty("user.dir", oldUserDir);
  }

  /* ================= save(Path,User) ================= */

  /**
   * Saves transactions and budgets to a custom JSON path and verifies that:
   *
   * <ul>
   *   <li>Success message is printed
   *   <li>The written file exists and contains expected JSON sections and fields
   *   <li>Transaction IDs are persisted
   * </ul>
   */
  @Test
  @DisplayName("save(Path): writes transactions and budgets to JSON")
  void save_path_writesJson() throws Exception {
    User u = mkUser("admin", "Ivan", "Petrov", "secret", false);
    u.wallet.addTransaction(
        new Transaction(1000.0, "salary", Transaction.Type.INCOME, "2025-01-10"));
    u.wallet.addTransaction(new Transaction(50.0, "food", Transaction.Type.EXPENSE, "2025-01-11"));
    u.wallet.setBudget("food", 200.0);

    Path file = tmp.resolve("out/wallet.json");
    String out = captureStdOut(() -> WalletJson.save(file, u));
    assertTrue(
        out.toLowerCase(Locale.ROOT).contains("saved wallet to"),
        "Should write success saving messages");

    String json = Files.readString(file, StandardCharsets.UTF_8);
    assertTrue(json.contains("\"transactions\""));
    assertTrue(json.contains("\"budgets\""));
    assertTrue(json.contains("\"salary\""));
    assertTrue(json.contains("\"food\""));
    assertTrue(json.contains("\"amount\""));
    assertTrue(json.contains("\"id\""), "Transaction GUID id should be persisted");
  }

  /**
   * When the parent of the target path is an existing file (not a directory), verifies that the
   * method prints a clear error and does not create the JSON file.
   */
  @Test
  @DisplayName(
      "save(Path): parent exists as a file  → prints an error and does not continue saving")
  void save_parentIsFile_printsError() throws Exception {
    User u = mkUser("a", "b", "c", "d", false);
    Path parentIsFile = tmp.resolve("notadir");
    Files.writeString(parentIsFile, "I am file", StandardCharsets.UTF_8);
    Path target = parentIsFile.resolve("wallet.json"); // parent не каталог

    String err = captureStdErr(() -> WalletJson.save(target, u));
    assertTrue(
        err.toLowerCase(Locale.ROOT).contains("parent is not a directory"),
        "Expected error about invalid parent");
    assertFalse(Files.exists(target), "Target file must not be created");
  }

  /* ================= save(User) via DEFAULT_FILE ================= */

  /**
   * Saves to the default file {@code data/wallet.json} under the temporary working directory and
   * verifies that the file is created and a success message is printed.
   */
  @Test
  @DisplayName("save(User): пишет в data/wallet.json (DEFAULT_FILE)")
  void save_defaultFile_writesToData() throws Exception {
    User u = mkUser("x", "y", "z", "p", false);
    u.wallet.addTransaction(new Transaction(10.0, "gift", Transaction.Type.INCOME, "2025-02-02"));
    String out = captureStdOut(() -> WalletJson.save(u));
    Path expected = Paths.get("data", "wallet.json"); //  tmp (user.dir) location
    assertTrue(Files.exists(expected), "data/wallet.json should be created");
    assertTrue(out.toLowerCase(Locale.ROOT).contains("saved wallet to"));
  }

  /* ================= loadInto(Path,User) ================= */
  /**
   * Loads from a well-formed JSON file into an existing user wallet that already contains:
   *
   * <ul>
   *   <li>One transaction with {@code id=t1}
   *   <li>One transaction without an id (signature-only duplicate check)
   * </ul>
   *
   * Verifies that:
   *
   * <ul>
   *   <li>Only one new transaction ({@code t2}) is imported
   *   <li>Budgets are updated for valid non-negative values
   *   <li>Invalid budget entries are ignored
   * </ul>
   *
   * Includes a null-safe parent directory creation to satisfy static analysis.
   */
  @Test
  @DisplayName(
      "loadInto(Path): correct JSON → importing transactions and budgets; duplicates are skipped")
  void load_ok_withDuplicatesAndBudgets() throws Exception {
    User u = mkUser("admin", "A", "B", "p", false);

    // previous transactions: the one with id=t1 and one without id (for signature)
    Transaction existing =
        new Transaction("t1", 1000.0, "salary", Transaction.Type.INCOME, "2025-01-10");
    u.wallet.addTransaction(existing);
    Transaction sigOnly =
        new Transaction(50.0, "food", Transaction.Type.EXPENSE, "2025-01-11"); // без id
    u.wallet.addTransaction(sigOnly);

    String json =
        """
{
  "transactions": [
    {
      "id": "t1",
      "date": "2025-01-10",
      "type": "INCOME",
      "title": "salary",
      "amount": 1000.0
    },
    {
      "date": "2025-01-11",
      "type": "EXPENSE",
      "title": "food",
      "amount": 50.0
    },
    {
      "id": "t2",
      "date": "2025-02-01",
      "type": "INCOME",
      "title": "gift",
      "amount": 200.0
    }
  ],
  "budgets": {
    "food": 200,
    "bad": "xx",
    "neg": -5
  }
}
""";

    Path f = tmp.resolve("in/w.json");
    // fixing SpotBugs error here: getParent() prevent from getting null
    Files.createDirectories(
        Objects.requireNonNull(f.getParent(), "Parent directory must not be null"));
    Files.writeString(f, json, StandardCharsets.UTF_8);

    String out = captureStdOut(() -> WalletJson.loadInto(f, u));
    assertTrue(out.contains("transactions: +1"), "Exactly one new transaction should be imported");
    assertTrue(
        out.toLowerCase(Locale.ROOT).contains("budgets updated"),
        "Budgets update summary should be printed");

    // Status: 2 old (t1 and without id) + new t2
    assertEquals(3, u.wallet.getTransactions().size(), "Old t1 + old (no id) + new t2");

    // New t2
    boolean hasT2 = u.wallet.getTransactions().stream().anyMatch(t -> "t2".equals(t.getId()));
    assertTrue(hasT2, "Transaction t2 should be present");

    // Budgets:  valid non null integers — secure, without auto unpack
    assertNotNull(u.wallet, "wallet must not be null");
    Map<String, Double> budgets = u.wallet.getBudgets();
    assertNotNull(budgets, "budgets map must not be null");

    assertTrue(budgets.containsKey("food"), "Budget 'food' must exist");
    final Double foodLimit = budgets.get("food");
    assertNotNull(foodLimit, "Budget 'food' value must be non-null");
    assertEquals(200.0, foodLimit.doubleValue(), 1e-9); // <- avoid auto unpack in assertEquals

    assertFalse(budgets.containsKey("bad"));
    assertFalse(budgets.containsKey("neg"));
  }

  /**
   * When the target path is missing, verifies that a friendly "no backup found" message is printed
   * and nothing is loaded.
   */
  @Test
  @DisplayName("loadInto(Path): file does not exist  → printing no wallet backup found")
  void load_path_missing_printsNotice() {
    User u = mkUser("u", "n", "s", "p", false);
    Path missing = tmp.resolve("nope/wallet.json");
    String out = captureStdOut(() -> WalletJson.loadInto(missing, u));
    assertTrue(
        out.toLowerCase(Locale.ROOT).contains("no wallet backup found"),
        "Expected message about missing file");
  }

  /**
   * When the given path is a directory, verifies that an error message is printed to {@code
   * System.err}.
   */
  @Test
  @DisplayName("loadInto(Path): path — folder → catch IOException and prinding error")
  void load_path_isDirectory_printsError() throws Exception {
    User u = mkUser("u", "n", "s", "p", false);
    Path dir = tmp.resolve("as_dir");
    Files.createDirectories(dir);

    String err = captureStdErr(() -> WalletJson.loadInto(dir, u));
    assertTrue(
        err.toLowerCase(Locale.ROOT).contains("error loading wallet"),
        "An error message must be printed on load failure");
  }

  /* ================= loadInto(User) via DEFAULT_FILE ================= */

  /**
   * Verifies that loading from the default path prints a "no backup found" message when {@code
   * data/wallet.json} is absent.
   */
  @Test
  @DisplayName("loadInto(User): no data/wallet.json → prints No wallet backup found")
  void load_default_missing_printsNotice() throws Exception {
    User u = mkUser("u", "n", "s", "p", false);

    // Making sure, the th file really does not exist (and there is no folder on its place)
    Path data = Paths.get("data");
    Path file = data.resolve("wallet.json");
    if (Files.exists(file)) {
      if (Files.isDirectory(file)) {
        Files.walk(file)
            .sorted(Comparator.reverseOrder())
            .forEach(
                p -> {
                  try {
                    Files.delete(p);
                  } catch (IOException ignored) {
                  }
                });
      } else {
        Files.deleteIfExists(file);
      }
    }

    String out = captureStdOut(() -> WalletJson.loadInto(u));

    assertTrue(
        out.toLowerCase(Locale.ROOT).contains("no wallet backup found"),
        "Expected message about missing file");
  }

  /**
   * Successful default-path import: prepares {@code data/wallet.json}, performs a load, and
   * verifies that transactions and budgets are imported and summarized correctly.
   */
  @Test
  @DisplayName("loadInto(User): normal import from data/wallet.json")
  void load_default_ok() throws Exception {
    // Preparing data/wallet.json in the working temp directory
    Path data = Paths.get("data");
    Files.createDirectories(data);
    Path file = data.resolve("wallet.json");

    String json =
        """
                      {
                        "transactions": [
                          {
                            "id": "aa1",
                            "date": "2025-03-03",
                            "type": "EXPENSE",
                            "title": "transport",
                            "amount": 70.0
                          }
                        ],
                        "budgets": {
                          "transport": 100
                        }
                      }
                      """;
    Files.writeString(file, json, StandardCharsets.UTF_8);

    User u = mkUser("u", "n", "s", "p", false);
    String out = captureStdOut(() -> WalletJson.loadInto(u));
    assertTrue(out.contains("transactions: +1"), "Exactly one new transaction should be imported");
    assertEquals(1, u.wallet.getTransactions().size());
    assertEquals(100.0, u.wallet.getBudgets().get("transport"), 1e-9);
  }

  /* ================= malformed JSON  ================= */

  /**
   * Verifies that malformed JSON produces an error on {@code System.err} that mentions the problem
   * (syntax/malformed) and includes line/column and JSON path information when available.
   */
  @Test
  @DisplayName("loadInto(Path): malformed JSON → stderr сontains line/column/path")
  void load_malformedJson_printsLocation() throws Exception {
    String badJson =
        """
                      {
                        "transactions": [
                          {
                            "id": "t1",
                            "date": "2025-10-10",
                            "type": "INCOME",
                            "title": "salary",
                            "amount": 1000.0
                          },
                          {
                            "id"  "t2",
                            "date": "2025-10-11",
                            "type": "EXPENSE",
                            "title": "food",
                            "amount": 50.0
                          }
                                   ^ missing colon here intentionally
                        ],
                        "budgets": {
                          "food": 200
                        }
                      }
                      """;

    Path file = tmp.resolve("wallet.json");
    Files.writeString(file, badJson, StandardCharsets.UTF_8);

    User u = mkUser("admin", "Ivan", "Petrov", "secret", false);

    String err = captureStdErr(() -> WalletJson.loadInto(file, u)).toLowerCase(Locale.ROOT);
    System.out.println("Captured stderr:\n" + err);

    assertTrue(
        err.contains("malformed") || err.contains("syntax"),
        "must mention malformed JSON or syntax error");
    assertTrue(
        err.contains("line") && err.contains("column"), "should include line/column information");
    assertTrue(err.contains("path"), "should include JSON path if available");
  }
}
