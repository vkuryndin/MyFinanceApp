package org.example.util;

import java.time.LocalDate;
import java.util.*;
import org.example.cli.ConsoleInput;
import org.example.model.Transaction;
import org.example.model.User;
import org.example.repo.RepoExceptions;
import org.example.repo.UsersRepo;
import org.example.storage.WalletJson;

public class ConsoleUtils {
  private ConsoleUtils() {}

  // private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

  /*
    private static LocalDate askDate(Scanner scanner, String prompt) {
      System.out.println(prompt);
      System.out.print("> ");
      String s = scanner.nextLine().trim();
      if (s.isEmpty()) return null; // без границы
      try {
        return LocalDate.parse(s, ISO);
      } catch (DateTimeParseException e) {
        System.out.println("Invalid date format. Expected yyyy-MM-dd. Try again.");
        return askDate(scanner, prompt);
      }
    }

    private static String askDateIsoOrToday(Scanner scanner, String prompt) {
      System.out.println(prompt);
      System.out.print("> ");
      String s = scanner.nextLine().trim();
      if (s.isEmpty()) return LocalDate.now().format(ISO);
      try {
        // проверим, что формат корректный
        LocalDate.parse(s, ISO);
        return s;
      } catch (DateTimeParseException e) {
        System.out.println("Invalid date format. Expected yyyy-MM-dd. Using today.");
        return LocalDate.now().format(ISO);
      }
    }

    private static Set<String> askCategories(Scanner scanner, String prompt) {
      System.out.println(prompt);
      System.out.print("> ");
      String line = scanner.nextLine();
      if (line == null || line.trim().isEmpty()) return Collections.emptySet();
      String[] parts = line.split(",");
      Set<String> set = new LinkedHashSet<>();
      for (String p : parts) {
        String v = p.trim();
        if (!v.isEmpty()) set.add(v);
      }
      return set;
    }
  */
  // my older methods

  public static boolean checkLogonStatus(User currentUser) {
    if (!(currentUser == null)) {
      System.out.println(
          "Logged in as: " + currentUser.login + " || " + currentUser.getAdminStatus());
      return true;
    } else {
      System.out.println("Status: You are not logged in");
      return false;
    }
  }

  public static void checkLogonStatusSimnple(User currentUser) {
    if (currentUser != null) {
      System.out.println(
          "Logged in as: " + currentUser.login + " || " + currentUser.getAdminStatus());
    } else {
      System.out.println("Status: You are not logged in");
    }
  }

  public static void handleViewStatistics(User u) {
    System.out.println("You are going to view statistics");
    double totalIncome = u.wallet.sumIncome();
    double totalExpense = u.wallet.sumExpense();
    double balance = u.wallet.getBalance();

    System.out.println("==========================");
    System.out.println("Wallet statistics");
    System.out.println("Total income: " + totalIncome);
    System.out.println("Total expense: " + totalExpense);
    System.out.println("Balance: " + balance);

    var incMap = u.wallet.incomesByCategory();
    if (incMap.isEmpty()) {
      System.out.println("No incomes yet");
    } else {
      System.out.println("Incomes by category:");
      for (var e : incMap.entrySet()) {
        System.out.println("- " + e.getKey() + ": " + e.getValue());
      }
    }
    var expMap = u.wallet.expensesByCategory();
    if (expMap.isEmpty()) {
      System.out.println("No expenses yet");
    } else {
      System.out.println("Expenses by category:");
      for (var e : expMap.entrySet()) {
        System.out.println("- " + e.getKey() + ": " + e.getValue());
      }
    }
    System.out.println("==========================");

    //  TO FIX we need to add some budgets statistics here also
    // double spent = u.wallet.getSpentByCategory(cat);
    // double rem = u.wallet.getRemainingBudget(cat);
    // System.out.println("Spent in: " + cat + ": " + spent + ", remaining: " + rem);
  }

  // ====== ИЗМЕНЕНО: добавлен ввод даты и создание Transaction с датой ======

  // adding income
  public static void handleAddIncome(Scanner scanner, User currentUser) {
    System.out.println("You are going to add income");
    double amount = ConsoleInput.readDoubleSafe(scanner, "Enter income amount");
    String title = ConsoleInput.readStringSafe(scanner, "Enter income title");
    String iso =
        ConsoleInput.readDateIsoOrToday(scanner, "Enter date (yyyy-MM-dd, empty = today):");

    // Если в Wallet есть addTransaction(Transaction tx) — используем его.
    // Иначе можно вернуть на старый вызов currentUser.wallet.addTransaction(amount, title,
    // Type.INCOME)
    Transaction tx = new Transaction(amount, title, Transaction.Type.INCOME, iso);
    currentUser.wallet.addTransaction(tx);

    System.out.println("Income added successfully: " + amount + " (" + title + ") on " + iso);
  }

  // adding expense
  public static void handleAddExpense(Scanner scanner, User currentUser) {
    System.out.println("You are going to add expense");
    double amount = ConsoleInput.readDoubleSafe(scanner, "Enter expense amount");
    String title = ConsoleInput.readStringSafe(scanner, "Enter expense title:");
    String iso =
        ConsoleInput.readDateIsoOrToday(scanner, "Enter date (yyyy-MM-dd, empty = today):");

    Transaction tx = new Transaction(amount, title, Transaction.Type.EXPENSE, iso);
    currentUser.wallet.addTransaction(tx);

    System.out.println("Expense added successfully: " + amount + " (" + title + ") on " + iso);
    if (currentUser.wallet.getBudgets().containsKey(title)) {
      double spent = currentUser.wallet.getSpentByCategory(title);
      double rem = currentUser.wallet.getRemainingBudget(title);
      System.out.println("Remaining budget for " + title + ": " + rem);
    }
    for (String a : currentUser.wallet.getBudgetAlerts()) {
      System.out.println("! " + a);
    }
  }

  // viewing wallet, transactions, budgets, alerts
  public static void handleViewWallet(User currentUser) {
    if (currentUser == null) {
      System.out.println("Status: You are not logged in");
      return;
    }

    System.out.println("You are going to view wallet");

    // баланс + ранние предупреждения по кошельку — СРАЗУ
    double bal = currentUser.wallet.getBalance();
    System.out.println("Balance: " + bal);
    if (bal == 0.0) {
      System.out.println("! Your wallet balance is zero.");
    } else if (bal < 0) {
      System.out.println("! Your wallet is negative!");
    }

    // viewing transactions
    var txs = currentUser.wallet.getTransactions();
    if (txs.isEmpty()) {
      System.out.println("No transactions yet");
    } else {
      System.out.println("Transactions:");
      for (Transaction t : txs) {
        System.out.println("- " + t);
      }
    }

    // viewing budgets
    var budgets = currentUser.wallet.getBudgets();
    if (budgets.isEmpty()) {
      System.out.println("No budgets yet");
    } else {
      System.out.println("Budgets:");
      for (var e : budgets.entrySet()) {
        String cat = e.getKey();
        double limit = e.getValue();
        double spent = currentUser.wallet.getSpentByCategory(cat);
        double rem = currentUser.wallet.getRemainingBudget(cat);
        System.out.println("- " + cat + ": " + limit + ", spent: " + spent + ", remaining: " + rem);
      }
    }

    // viewing alerts (пороговые уведомления по бюджетам)
    // ВАЖНО: имя метода — getBudgetAlerts() (CamelCase). Если у тебя ещё старое имя, замени на
    // него.
    var alerts = currentUser.wallet.getBudgetAlerts();
    for (String a : alerts) {
      System.out.println("! " + a);
    }
  }

  // adding budgets
  public static void handleAddBudget(Scanner scanner, User currentUser) {
    System.out.println("You are going to add budget for your categories");
    String cat = ConsoleInput.readStringSafe(scanner, "Enter category name: ");
    double limit = ConsoleInput.readNonNegativeDouble(scanner, "Enter budget limit (>= 0):");
    currentUser.wallet.setBudget(cat, limit);
    System.out.println("Budget added successfully: " + limit + " (" + cat + ")");

    // showing current spent and the remaining budget
    double spent = currentUser.wallet.getSpentByCategory(cat);
    double rem = currentUser.wallet.getRemainingBudget(cat);
    System.out.println("Spent in: " + cat + ": " + spent + ", remaining: " + rem);
  }

  // transferring money between users
  public static void handleTransfer(Scanner scanner, User currentUser, UsersRepo USERS) {
    String toLogin =
        ConsoleInput.readStringSafe(scanner, "Enter login of user to transfer money to: ");
    double amount = ConsoleInput.readDoubleSafe(scanner, "Enter amount to transfer: ");
    String note = ConsoleInput.readStringSafe(scanner, "Enter note (reason for transfer): ");

    try {
      USERS.transfer(currentUser.login, toLogin, amount, note);
      System.out.println("Transferred " + amount + " to " + toLogin);
      System.out.println("Your new balance: " + currentUser.wallet.getBalance());
    } catch (RepoExceptions.Invalid e) {
      System.out.println("Transfer failed: " + e.getMessage());
    } catch (RepoExceptions.NotFound e) {
      System.out.println("Receiver not found.");
    } catch (RepoExceptions.Forbidden e) {
      System.out.println("Action forbidden.");
    }
  }

  public static boolean handleDeleteYourUserAccount(
      Scanner scanner, User currentUser, UsersRepo USERS) {
    System.out.println("You are going to delete your user account");
    // we are not allowing super admin to delete his/hers account
    if (currentUser.hasRole(User.Role.SUPER_ADMIN)) {
      System.out.println("You are a super admin. You cannot delete this account");
      return false;
    }
    if (!ConsoleUtils.confirmAction(scanner, "")) return false;
    else {
      String pass =
          ConsoleInput.readStringSafe(scanner, "Enter your password to confirm deletion: ");
      try {
        boolean result = USERS.deleteUser(currentUser.login, pass);
        if (result) {
          System.out.println("Account " + currentUser.login + " deleted successfully");
          return true;
        } else {
          System.out.println("Account deletion failed");
          return false;
        }
      } catch (RepoExceptions.Invalid e) {
        System.out.println("Wrong password.");
        return false;
      } catch (RepoExceptions.Forbidden e) {
        System.out.println("Cannot delete super admin.");
        return false;
      } catch (RepoExceptions.NotFound e) {
        System.out.println("User not found.");
        return false;
      }
    }
  }

  public static boolean handleDeleteSelectedUserAccount(
      Scanner scanner, User currentUser, UsersRepo USERS) {
    System.out.println("You are going to delete the user account you select...");
    String login = ConsoleInput.readStringSafe(scanner, "Enter login of the user to delete:");
    if (login.isEmpty()) {
      System.out.println("Login cannot be empty. Operation cancelled.");
      return false;
    }
    User userToDelete = USERS.find(login);
    if (userToDelete == null) {
      System.out.println("User not found. Operation cancelled.");
      return false;
    } else if (userToDelete.equals(currentUser)) {
      System.out.println("You cannot delete yourself. Operation cancelled.");
      return false;
    } else if (userToDelete.hasRole(User.Role.SUPER_ADMIN)) {
      System.out.println("You cannot delete super admin account. Operation cancelled.");
      return false;
    } else {
      try {
        if (USERS.deleteUser(login)) {
          System.out.println("Account " + login + " deleted successfully");
          return true;
        } else {
          System.out.println("Account deletion failed");
          return false;
        }
      } catch (RepoExceptions.Forbidden e) {
        System.out.println("Cannot delete super admin.");
        return false;
      } catch (RepoExceptions.NotFound e) {
        System.out.println("User not found.");
        return false;
      } catch (RepoExceptions.Invalid e) {
        System.out.println("Invalid data: " + e.getMessage());
        return false;
      }
    }
  }

  public static boolean handleAddOrdinaryAdminAccount(
      Scanner scanner, User currentUser, UsersRepo USERS, List<User> allUsers) {
    System.out.println("You are now going to add ordinary administrator account...");

    System.out.println("The super administrator is: ");
    for (User u : allUsers) {
      if (u.hasRole(User.Role.SUPER_ADMIN)) {
        System.out.println(u.name + " " + u.surname);
      }
    }
    System.out.println("The current administrators are: ");
    for (User u : allUsers) {
      if (u.hasRole(User.Role.ADMIN)) {
        System.out.println(u.name + " " + u.surname);
      }
    }
    System.out.println("All other users are: ");
    for (User u : allUsers) {
      if (u.hasRole(User.Role.USER) && u.getRoles().size() == 1) {
        System.out.println(u.name + " " + u.surname);
      }
    }
    String sure =
        ConsoleInput.readStringSafe(
            scanner, "Type YES to confirm adding new administrator account: ");
    if (!"YES".equalsIgnoreCase(sure)) {
      System.out.println("Wrong input, try again.");
      return false;
    }
    String pass =
        ConsoleInput.readStringSafe(
            scanner, "Enter your password to confirm adding new administrator account: ");
    String newAdminLogin =
        ConsoleInput.readStringSafe(scanner, "Enter new ordinary administrator login: ");

    try {
      USERS.addAdmin(currentUser.login, pass, newAdminLogin);
      System.out.println("Admin account added successfully.");
      return true;
    } catch (RepoExceptions.Invalid e) {
      System.out.println("Invalid data: " + e.getMessage());
      return false;
    } catch (RepoExceptions.NotFound e) {
      System.out.println("User not found.");
      return false;
    } catch (RepoExceptions.Conflict e) {
      System.out.println("User is already admin.");
      return false;
    } catch (RepoExceptions.Forbidden e) {
      System.out.println("Only super admin can do this.");
      return false;
    }
  }

  public static boolean handleRemoveOrdinaryAdminAccount(Scanner scanner, UsersRepo USERS) {
    System.out.println("You are now going to remove administrator account...");
    if (!ConsoleUtils.confirmAction(scanner, "removing ordinary admin account")) return false;
    String removeAdminLogin =
        ConsoleInput.readStringSafe(
            scanner, "Enter login of the ordinary administrator to remove: ");
    try {
      USERS.removeAdmin(removeAdminLogin);
      System.out.println("Admin account removed successfully.");
      return true;
    } catch (RepoExceptions.NotFound e) {
      System.out.println("User not found.");
      return false;
    } catch (RepoExceptions.Forbidden e) {
      System.out.println("Cannot modify super admin.");
      return false;
    } catch (RepoExceptions.Invalid e) {
      System.out.println("Invalid input: " + e.getMessage());
      return false;
    } catch (RepoExceptions.Conflict e) {
      System.out.println("User is not an admin.");
      return false;
    }
  }

  public static boolean confirmAction(Scanner scanner, String what) {
    String sure = ConsoleInput.readStringSafe(scanner, "Type YES to confirm: " + what);
    if (!"YES".equalsIgnoreCase(sure)) {
      System.out.println("Wrong input, try again.");
      return false;
    }
    return true;
  }

  // ====== ДОБАВЛЕНО: Расширенная статистика (критерий 11 = 2/2) ======

  public static void handleAdvancedStatistics(Scanner scanner, User u) {
    System.out.println("=== Advanced statistics (period/categories) ===");

    LocalDate from =
        ConsoleInput.readDateOrNull(
            scanner, "Enter FROM date (yyyy-MM-dd) or empty for no lower bound:");
    ;
    LocalDate to =
        ConsoleInput.readDateOrNull(
            scanner, "Enter TO date (yyyy-MM-dd) or empty for no upper bound:");
    if (from != null && to != null && to.isBefore(from)) {
      System.out.println("TO is before FROM — swapping bounds.");
      LocalDate tmp = from;
      from = to;
      to = tmp;
    }

    Set<String> categories =
        ConsoleInput.readCategoriesSet(scanner, "Enter categories comma-separated (empty = all):");

    System.out.println();
    System.out.println("— Expenses —");
    double exp = u.wallet.sumExpense(from, to, categories);
    if (exp == 0.0) {
      System.out.println("No expense data for selected period/categories.");
    } else {
      var byCat = u.wallet.expensesByCategory(from, to, categories);
      printMap(byCat);
      System.out.println("Total expenses: " + exp);
    }

    System.out.println();
    System.out.println("— Incomes —");
    double inc = u.wallet.sumIncome(from, to, categories);
    if (inc == 0.0) {
      System.out.println("No income data for selected period/categories.");
    } else {
      var byCat = u.wallet.incomesByCategory(from, to, categories);
      printMap(byCat);
      System.out.println("Total incomes: " + inc);
    }

    System.out.println();
    System.out.println("— Filtered transactions —");
    List<Transaction> list = u.wallet.findTransactions(from, to, categories, null);
    if (list.isEmpty()) {
      System.out.println("No transactions for selected conditions.");
    } else {
      for (Transaction t : list) {
        System.out.println(
            t.getDateIso() + " | " + t.getType() + " | " + t.getTitle() + " | " + t.getAmount());
      }
    }

    System.out.println("=== Done ===");
  }

  private static void printMap(Map<String, Double> map) {
    if (map == null || map.isEmpty()) {
      System.out.println("(empty)");
      return;
    }
    for (var e : map.entrySet()) {
      System.out.println("  " + e.getKey() + ": " + e.getValue());
    }
  }

  // ====== ДОБАВЛЕНО: Хендлеры для критерия 12 (редактирование бюджетов) ======

  public static void handleUpdateBudgetLimit(Scanner scanner, User u) {
    System.out.println("You are going to update a budget limit");

    // СНАЧАЛА — выбрать корректную существующую категорию:
    String cat = askExistingBudgetCategory(scanner, u);
    if (cat == null) {
      System.out.println("Operation cancelled.");
      return; // пользователь ввёл CANCEL или нет категорий
    }

    // ТЕПЕРЬ — спросить новый лимит (>= 0):
    double newLimit = ConsoleInput.readNonNegativeDouble(scanner, "Enter new limit (>= 0):");
    boolean ok = u.wallet.updateBudgetLimit(cat, newLimit);
    if (ok) {
      System.out.println("Budget updated: " + cat + " -> " + newLimit);
      double spent = u.wallet.getSpentByCategory(cat);
      double rem = u.wallet.getRemainingBudget(cat);
      System.out.println("Spent: " + spent + ", remaining: " + rem);
    } else {
      // Теоретически не должно случиться (категорию мы уже верифицировали),
      // но оставим защитное сообщение.
      System.out.println("Category not found in budgets.");
    }
  }

  public static void handleRemoveBudget(Scanner scanner, User u) {
    System.out.println("You are going to remove a budget");
    String cat = askExistingBudgetCategory(scanner, u);
    if (cat == null) {
      System.out.println("Operation cancelled.");
      return;
    }
    boolean ok = u.wallet.removeBudget(cat);
    System.out.println(ok ? "Budget removed: " + cat : "Category not found in budgets.");
  }

  public static void handleRenameCategory(Scanner scanner, User u) {
    System.out.println("You are going to rename a budget category");
    String oldName = askExistingBudgetCategory(scanner, u);
    if (oldName == null) {
      System.out.println("Operation cancelled.");
      return;
    }

    String newName = ConsoleInput.readStringSafe(scanner, "Enter new category name:");
    if (oldName.equals(newName)) {
      System.out.println("Old and new names are the same.");
      return;
    }
    boolean ok = u.wallet.renameCategory(oldName, newName);
    if (ok) {
      System.out.println("Category renamed: " + oldName + " -> " + newName);
      var budgets = u.wallet.getBudgets();
      if (budgets.containsKey(newName)) {
        double limit = budgets.get(newName);
        double spent = u.wallet.getSpentByCategory(newName);
        double rem = u.wallet.getRemainingBudget(newName);
        System.out.println(
            "- " + newName + ": " + limit + ", spent: " + spent + ", remaining: " + rem);
      }
    } else {
      System.out.println("Nothing changed. Category not found or invalid names.");
    }
  }

  private static String askExistingBudgetCategory(Scanner scanner, User u) {
    var budgets = u.wallet.getBudgets(); // Map<String, Double>
    if (budgets.isEmpty()) {
      System.out.println("There are no budget categories yet.");
      return null;
    }

    System.out.println("Existing budget categories:");
    for (var e : budgets.entrySet()) {
      System.out.println("  - " + e.getKey() + " (limit: " + e.getValue() + ")");
    }
    System.out.println("Type a category name exactly as above.");
    System.out.println("Or type CANCEL to abort.");
    System.out.print("> ");

    while (true) {
      String cat = scanner.nextLine().trim();
      if (cat.equalsIgnoreCase("CANCEL")) return null;
      if (budgets.containsKey(cat)) return cat;

      // не нашли — повторно показываем список и просим ещё раз
      System.out.println(
          "Category not found. Please choose one from the list above or type CANCEL.");
      System.out.print("> ");
    }
  }

  // handle methods for exporting and iporting json
  public static void handleExportJson(Scanner scanner, User u) {
    // String path = ConsoleInput.readStringSafe(scanner, "Enter file path for JSON export:");
    try {
      WalletJson.save(u);
      // System.out.println("Exported to " + Path.of(path).toAbsolutePath());
    } catch (Exception e) {
      System.out.println("Export failed: " + e.getMessage());
    }
  }

  public static void handleImportJson(Scanner scanner, User u) {
    // String path = ConsoleInput.readStringSafe(scanner, "Enter JSON file path to import:");
    // java.nio.file.Path p = java.nio.file.Path.of(path);
    // if (!java.nio.file.Files.exists(p)) {
    //    System.out.println("File not found: " + p.toAbsolutePath());
    //    return;
    // }
    try {
      WalletJson.loadInto(u);
    } catch (Exception e) {
      System.out.println("Import failed: " + e.getMessage());
    }
  }
}
