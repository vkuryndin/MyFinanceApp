package org.example.util;

import java.util.List;
import java.util.Scanner;
import org.example.cli.ConsoleInput;
import org.example.model.Transaction;
import org.example.model.User;
import org.example.repo.RepoExceptions;
import org.example.repo.UsersRepo;

public class ConsoleUtils {
  private ConsoleUtils() {}

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

  // adding income
  public static void handleAddIncome(Scanner scanner, User currentUser) {
    System.out.println("You are going to add income");
    double incomeAmount = ConsoleInput.readDoubleSafe(scanner, "Enter income amount");
    String incomeTitle = ConsoleInput.readStringSafe(scanner, "Enter income title");
    currentUser.wallet.addTransaction(incomeAmount, incomeTitle, Transaction.Type.INCOME);
    System.out.println("Income added successfully: " + incomeAmount + " (" + incomeTitle + ")");
  }

  // adding expense
  public static void handleAddExpense(Scanner scanner, User currentUser) {
    System.out.println("You are going to add expense");
    double expenseAmount = ConsoleInput.readDoubleSafe(scanner, "Enter expense amount");
    String expenseTitle = ConsoleInput.readStringSafe(scanner, "Enter expense title:");
    currentUser.wallet.addTransaction(expenseAmount, expenseTitle, Transaction.Type.EXPENSE);
    System.out.println("Expense added successfully: " + expenseAmount + " (" + expenseTitle + ")");
    if (currentUser.wallet.getBudgets().containsKey(expenseTitle)) {
      double rem = currentUser.wallet.getRemainingBudget(expenseTitle);
      System.out.println("Remaining budget for " + expenseTitle + ": " + rem);
      if (rem < 0) {
        System.out.println("You have exceeded your budget for " + expenseTitle + " by " + (-rem));
      }
    }
  }

  // viewing wallet, transactions, budgets, alerts
  public static void handleViewWallet(User currentUser) {
    System.out.println("You are going to view wallet");
    System.out.println("Balance: " + currentUser.wallet.getBalance());

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
    // viewing alerts
    var alerts = currentUser.wallet.getbudgetAlerts();
    for (String a : alerts) {
      System.out.println("! " + a);
    }
  }

  // adding budgets
  public static void handleAddBudget(Scanner scanner, User currentUser) {
    System.out.println("You are going to add budget for your categories");
    String cat = ConsoleInput.readStringSafe(scanner, "Enter category name: ");
    double limit = ConsoleInput.readDoubleSafe(scanner, "Enter budget limit: ");
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
    if (!ConsoleUtils.confirmAction(scanner)) return false;
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
    if (!ConsoleUtils.confirmAction(scanner)) return false;
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

  public static boolean confirmAction(Scanner scanner) {
    String sure = ConsoleInput.readStringSafe(scanner, "Type YES to confirm account deletion: ");
    if (!"YES".equalsIgnoreCase(sure)) {
      System.out.println("Wrong input, try again.");
      return false;
    }
    return true;
  }
}
