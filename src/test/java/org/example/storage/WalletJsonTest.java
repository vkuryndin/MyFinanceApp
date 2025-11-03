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

@DisplayName("WalletJson: save/load full coverage")
class WalletJsonTest {

  @TempDir Path tmp;
  private String oldUserDir;

  /* ================= helpers ================= */

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

  /** Универсальный конструктор User, как в твоих других тестах */
  private static User mkUser(
      String login, String name, String surname, String pass, boolean superAdmin) {
    try {
      // ищем любой конструктор с >=4 строковыми параметрами
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

  @BeforeEach
  void chdirToTmp() {
    // чтобы тестировать save(User)/loadInto(User) c DEFAULT_FILE=data/wallet.json в tmp
    oldUserDir = System.getProperty("user.dir");
    System.setProperty("user.dir", tmp.toString());
  }

  @AfterEach
  void restoreDir() {
    if (oldUserDir != null) System.setProperty("user.dir", oldUserDir);
  }

  /* ================= save(Path,User) ================= */

  @Test
  @DisplayName("save(Path): записывает транзакции и бюджеты в JSON")
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
        "Должно печатать успешное сохранение");

    String json = Files.readString(file, StandardCharsets.UTF_8);
    assertTrue(json.contains("\"transactions\""));
    assertTrue(json.contains("\"budgets\""));
    assertTrue(json.contains("\"salary\""));
    assertTrue(json.contains("\"food\""));
    assertTrue(json.contains("\"amount\""));
    assertTrue(json.contains("\"id\""), "Должен сохраняться GUID id транзакции");
  }

  @Test
  @DisplayName("save(Path): parent существует как файл → печатает ошибку и не пишет")
  void save_parentIsFile_printsError() throws Exception {
    User u = mkUser("a", "b", "c", "d", false);
    Path parentIsFile = tmp.resolve("notadir");
    Files.writeString(parentIsFile, "I am file", StandardCharsets.UTF_8);
    Path target = parentIsFile.resolve("wallet.json"); // parent не каталог

    String err = captureStdErr(() -> WalletJson.save(target, u));
    assertTrue(
        err.toLowerCase(Locale.ROOT).contains("parent is not a directory"),
        "Ожидалась ошибка о неверном родителе");
    assertFalse(Files.exists(target), "Файл не должен создаться");
  }

  /* ================= save(User) через DEFAULT_FILE ================= */

  @Test
  @DisplayName("save(User): пишет в data/wallet.json (DEFAULT_FILE)")
  void save_defaultFile_writesToData() throws Exception {
    User u = mkUser("x", "y", "z", "p", false);
    u.wallet.addTransaction(new Transaction(10.0, "gift", Transaction.Type.INCOME, "2025-02-02"));
    String out = captureStdOut(() -> WalletJson.save(u));
    Path expected = Paths.get("data", "wallet.json"); // относительно tmp (user.dir)
    assertTrue(Files.exists(expected), "Должен появиться data/wallet.json");
    assertTrue(out.toLowerCase(Locale.ROOT).contains("saved wallet to"));
  }

  /* ================= loadInto(Path,User) ================= */

  @Test
  @DisplayName(
      "loadInto(Path): корректный JSON → импорт транзакций и бюджетов; дубликаты пропускаются")
  void load_ok_withDuplicatesAndBudgets() throws Exception {
    User u = mkUser("admin", "A", "B", "p", false);

    // Предсуществующие транзакции: одна с id=t1 и одна без id (для сигнатуры)
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
    // единственная правка для SpotBugs: getParent() защищаем от null
    Files.createDirectories(
        Objects.requireNonNull(f.getParent(), "Parent directory must not be null"));
    Files.writeString(f, json, StandardCharsets.UTF_8);

    String out = captureStdOut(() -> WalletJson.loadInto(f, u));
    assertTrue(
        out.contains("transactions: +1"), "Импортироваться должна только одна новая транзакция");
    assertTrue(
        out.toLowerCase(Locale.ROOT).contains("budgets updated"),
        "Должно посчитать обновлённые бюджеты");

    // Состояние: 2 старые (t1 и без id) + новая t2
    assertEquals(3, u.wallet.getTransactions().size(), "Итог: старая t1, старая без id, новая t2");

    // Новая t2
    boolean hasT2 = u.wallet.getTransactions().stream().anyMatch(t -> "t2".equals(t.getId()));
    assertTrue(hasT2, "Должна появиться транзакция t2");

    // Бюджеты: валидные неотрицательные числа — безопасно, без авто-распаковки
    assertNotNull(u.wallet, "wallet must not be null");
    Map<String, Double> budgets = u.wallet.getBudgets();
    assertNotNull(budgets, "budgets map must not be null");

    assertTrue(budgets.containsKey("food"), "Budget 'food' must exist");
    final Double foodLimit = budgets.get("food");
    assertNotNull(foodLimit, "Budget 'food' value must be non-null");
    assertEquals(
        200.0, foodLimit.doubleValue(), 1e-9); // <- избегаем авто-распаковки в assertEquals

    assertFalse(budgets.containsKey("bad"));
    assertFalse(budgets.containsKey("neg"));
  }

  @Test
  @DisplayName("loadInto(Path): файл отсутствует → печатает No wallet backup found")
  void load_path_missing_printsNotice() {
    User u = mkUser("u", "n", "s", "p", false);
    Path missing = tmp.resolve("nope/wallet.json");
    String out = captureStdOut(() -> WalletJson.loadInto(missing, u));
    assertTrue(
        out.toLowerCase(Locale.ROOT).contains("no wallet backup found"),
        "Ожидалось сообщение об отсутствии файла");
  }

  @Test
  @DisplayName("loadInto(Path): путь — каталог → ловим IOException и печатаем ошибку")
  void load_path_isDirectory_printsError() throws Exception {
    User u = mkUser("u", "n", "s", "p", false);
    Path dir = tmp.resolve("as_dir");
    Files.createDirectories(dir);

    String err = captureStdErr(() -> WalletJson.loadInto(dir, u));
    assertTrue(
        err.toLowerCase(Locale.ROOT).contains("error loading wallet"),
        "Должна печататься ошибка загрузки");
  }

  /* ================= loadInto(User) через DEFAULT_FILE ================= */

  @Test
  @DisplayName("loadInto(User): нет data/wallet.json → печатает No wallet backup found")
  void load_default_missing_printsNotice() throws Exception {
    User u = mkUser("u", "n", "s", "p", false);

    // Убедимся, что файла точно нет (и что нет каталога на его месте)
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
        "Ожидалось сообщение об отсутствии файла");
  }

  @Test
  @DisplayName("loadInto(User): нормальный импорт из data/wallet.json")
  void load_default_ok() throws Exception {
    // Подготовим data/wallet.json в рабочем tmp-каталоге
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
    assertTrue(out.contains("transactions: +1"), "Должна импортироваться одна транзакция");
    assertEquals(1, u.wallet.getTransactions().size());
    assertEquals(100.0, u.wallet.getBudgets().get("transport"), 1e-9);
  }

  /* ================= malformed JSON (твой кейс) ================= */

  @Test
  @DisplayName("loadInto(Path): malformed JSON → stderr содержит line/column/path")
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
