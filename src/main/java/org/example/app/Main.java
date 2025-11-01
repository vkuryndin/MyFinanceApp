package org.example.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import org.example.model.Transaction;
import org.example.model.User;
import org.example.repo.UsersRepo;
import org.example.storage.StorageJson;
import org.example.util.ConsoleMenu;
import org.example.util.Input;
import org.example.util.Misc;

public class Main {

  private static final Scanner scanner = new Scanner(System.in);
  private static UsersRepo USERS = new UsersRepo();
  private static User currentUser = null;
  private static final Path DATA_FILE = Paths.get("data", "finance-data.json");

  private static boolean isExit =
      false; // this will allow us to exit from the 2 tier (Actions) menu
  private static boolean isloggedOut =
      false; // this will allow us to enter log in menu after user deletion

  public static void main(String[] args) {
    // showFirstMenu();
    USERS = StorageJson.loadOrNew(DATA_FILE);

    // changing the welcome string whether this is the previously saved data exists
    if (USERS.getIsPreviousDataExists()) {
      System.out.println("Welcome back to my finance app");
    } else {
      System.out.println("Welcome to my finance app");
    }
    System.out.println("==========================");
    // runFirstMenu();
    runLoginMenu();
  }

  private static void runLoginMenu() {
    while (true) {
      if (!isExit) {
        ConsoleMenu.showLogInMenu();
        int option = Input.readIntSafe(scanner);
        switch (option) {
          case 1:
            System.out.println("Log in");
            String login = Input.readLoginSafe(scanner);
            User u = USERS.find(login);
            if (u == null) {
              System.out.println("User not found. Creating new user...");
              String name = Input.readStringSafe(scanner, "Please enter your name: ", true);
              String surname = Input.readStringSafe(scanner, "Please enter your surname: ", true);
              String pass = Input.readStringSafe(scanner, "Please enter your password: ");
              u = USERS.register(login, name, surname, pass);
              System.out.println("User created successfully: " + u.toString());
            } else {
              // TO FIX implement password policy
              String pass = Input.readStringSafe(scanner, "Please enter your password: ");
              if (USERS.authenticate(login, pass) == null) {
                System.out.println("Wrong password");
                System.out.println("> ");
                break;
              }
              System.out.println("Logged in successfully: " + u);
            }
            currentUser = u;
            runActionsMenu();
            break;
          case 2:
            System.out.println("View docs");
            // TO FIX::write the documentation of the program
            break;
          case 3:
            System.out.println("You have exited");
            StorageJson.save(DATA_FILE, USERS);
            System.out.println("Saving data to file: " + DATA_FILE.toAbsolutePath());
            System.out.println("Bye!");
            scanner.close();
            return;
          default:
            System.out.println("Invalid option, Choose  1-3");
            break;
        }
      } else return;
    }
  }

  private static void runActionsMenu() {
    while (true) {
      if (!isloggedOut) {
        ConsoleMenu.showActionsMenu();
        int option = Input.readIntSafe(scanner);
        switch (option) {
          case 1:
            System.out.println("Main actions");
            runMainActionsMenu();
            break;
          case 2:
            // System.out.println("Admin actions");
            System.out.println(
                "Your are going to enter either super of ordinary administrator "
                    + "menu depending on the level of your access rights");
            // check what kind of admin we have to show different menus
            if (currentUser != null && currentUser.hasRole(User.Role.SUPER_ADMIN)) {
              runSuperAdminMenu(); // running super admin menu
            } else if (currentUser != null && currentUser.hasRole(User.Role.ADMIN)) {
              runOrdinaryAdminMenu(); // running ordinary admin menu
            } else {
              System.out.println("You are not administrator");
              System.out.println("Log in as either super or ordinary administrator and try again");
            }
            break;
          case 3:
            System.out.println("Log out");
            if (currentUser == null) {
              System.out.println("You are not logged in");
              System.out.println("> ");
            } else {
              System.out.println("You have logged out");
              currentUser = null;
              System.out.println("> ");
            }
            return;
          case 4:
            System.out.println("You have exited");
            StorageJson.save(DATA_FILE, USERS);
            System.out.println("Saving data to file: " + DATA_FILE.toAbsolutePath());
            System.out.println("Bye!");
            scanner.close();
            isExit = true;
            return;
          default:
            System.out.println("Invalid option, Choose  1-4");
            break;
        }
      } else return;
    }
  }

  private static void runMainActionsMenu() {
    while (true) {
      if (!Misc.checkLogonStatus(currentUser)) break;
      ConsoleMenu.showMainActionsMenu();
      int option = Input.readIntSafe(scanner);
      switch (option) {
        case 1:
          System.out.println("You are going to add income");
          double incomeAmount = Input.readDoubleSafe(scanner, "Enter income amount");
          String incomeTitle = Input.readStringSafe(scanner, "Enter income title");
          currentUser.wallet.addTransaction(incomeAmount, incomeTitle, Transaction.Type.INCOME);
          System.out.println(
              "Income added successfully: " + incomeAmount + " (" + incomeTitle + ")");
          break;
        case 2:
          System.out.println("You are going to add expense");
          double expenseAmount = Input.readDoubleSafe(scanner, "Enter expense amount");
          String expenseTitle = Input.readStringSafe(scanner, "Enter expense title:");
          currentUser.wallet.addTransaction(expenseAmount, expenseTitle, Transaction.Type.EXPENSE);
          System.out.println(
              "Expense added successfully: " + expenseAmount + " (" + expenseTitle + ")");
          if (currentUser.wallet.getBudgets().containsKey(expenseTitle)) {
            double rem = currentUser.wallet.getRemainingBudget(expenseTitle);
            System.out.println("Remaining budget for " + expenseTitle + ": " + rem);
            if (rem < 0) {
              System.out.println(
                  "You have exceeded your budget for " + expenseTitle + " by " + (-rem));
            }
          }
          break;
        case 3:
          System.out.println("You are going to view wallet");
          // System.out.println(currentUser.wallet.toString());
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
              System.out.println(
                  "- " + cat + ": " + limit + ", spent: " + spent + ", remaining: " + rem);
            }
          }
          // viewing alerts
          var alerts = currentUser.wallet.getbudgetAlerts();
          for (String a : alerts) {
            System.out.println("! " + a);
          }
          break;
        case 4:
          System.out.println("You are going to add budget for your categories");
          String cat = Input.readStringSafe(scanner, "Enter category name: ");
          double limit = Input.readDoubleSafe(scanner, "Enter budget limit: ");
          currentUser.wallet.setBudget(cat, limit);
          System.out.println("Budget added successfully: " + limit + " (" + cat + ")");

          // showing current spent and the remaining budget
          double spent = currentUser.wallet.getSpentByCategory(cat);
          double rem = currentUser.wallet.getRemainingBudget(cat);
          System.out.println("Spent in: " + cat + ": " + spent + ", remaining: " + rem);
          break;
        case 5:
          System.out.println("You are going to view statistics");
          Misc.displayStatitics(currentUser);
          break;
        case 6:
          String toLogin =
              Input.readStringSafe(scanner, "Enter login of user to transfer money to: ");
          double amount = Input.readDoubleSafe(scanner, "Enter amount to transfer: ");
          String note = Input.readStringSafe(scanner, "Enter note (reason for transfer): ");

          try {
            USERS.transfer(currentUser.login, toLogin, amount, note);
            System.out.println("Transferred " + amount + "to " + toLogin);
            System.out.println("Your new balance: " + currentUser.wallet.getBalance());
          } catch (IllegalArgumentException e) {
            System.out.println("Transfer fails " + e.getMessage());
          }
          break;
        case 7:
          System.out.println("You are going to delete your user account");
          // we ae not allowing super admin to delete his/hers account
          if (currentUser.hasRole(User.Role.SUPER_ADMIN)) {
            System.out.println("You are a super admin. You cannot delete this account");
            break;
          }
          if (!Misc.confirmAction(scanner)) break;
          else {
            String pass =
                Input.readStringSafe(scanner, "Enter your password to confirm deletion: ");
            boolean result = USERS.deleteUser(currentUser.login, pass);
            if (result) {
              System.out.println("Account " + currentUser.login + " deleted successfully");
              currentUser = null;
            } else {
              System.out.println("Account deletion failed");
            }
          }
          isloggedOut = true;
          break;
        case 8:
          System.out.println("You are going to return to main menu");
          return;
        default:
          System.out.println("Invalid option, Choose  1-8");
          System.out.println("> ");
          break;
      }
    }
  }

  private static void runSuperAdminMenu() {
    while (true) {
      ConsoleMenu.showSuperAdminMenu();
      int option = Input.readIntSafe(scanner);
      List<User> allUsers = USERS.listAll();
      switch (option) {
        case 1:
          System.out.println("You are going to view all users");
          USERS.listAllUsers();
          break;
        case 2:
          System.out.println("You are going to view statistics for all users");
          for (User u : allUsers) {
            System.out.println("Displaying statistics for user: " + u.login);
            Misc.displayStatitics(u);
          }
          break;
        case 3:
          System.out.println("You are going to delete the user account you select...");
          String login = Input.readStringSafe(scanner, "Enter login of the user to delete:");
          if (login.isEmpty()) {
            System.out.println("Login cannot be empty. Operation cancelled.");
            break;
          }
          User userToDelete = USERS.find(login);
          if (userToDelete == null) {
            System.out.println("User not found. Operation cancelled.");
            break;
          } else if (userToDelete.equals(currentUser)) {
            System.out.println("You cannot delete yourself. Operation cancelled.");
            break;
          } else if (userToDelete.hasRole(User.Role.SUPER_ADMIN)) {
            System.out.println("You cannot delete super admin account. Operation cancelled.");
            break;
          } else {
            if (USERS.deleteUser(login)) {
              System.out.println("Account " + login + " deleted successfully");
              break;
            } else {
              System.out.println("Account " + login + "  deletion failed");
            }
          }
          break;
        case 4:
          System.out.println("You are going to delete all users except super admin ");
          if (Misc.confirmAction(scanner)) USERS.deleteAllUsers();
          break;
        case 5:
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
              // adminUsers.add(u);
              System.out.println(u.name + " " + u.surname);
            }
          }
          System.out.println("All other users are: ");
          for (User u : allUsers) {
            if (u.hasRole(User.Role.USER) && u.getRoles().size() == 1) {
              // otherUsers.add(u);
              System.out.println(u.name + " " + u.surname);
            }
          }
          String sure =
              Input.readStringSafe(
                  scanner, "Type YES to confirm adding new administrator account: ");
          if (!"YES".equalsIgnoreCase(sure)) {
            System.out.println("Wrong input, try again.");
            break;
          }
          String pass =
              Input.readStringSafe(
                  scanner, "Enter your password to confirm adding new administrator account: ");
          String newAdminLogin =
              Input.readStringSafe(scanner, "Enter new ordinary administrator login: ");

          boolean result = USERS.addAdmin(currentUser.login, pass, newAdminLogin);
          if (result) {
            System.out.println("Admin account added successfully");
          } else {
            System.out.println("Admin account addition failed");
          }
          break;
        case 6:
          // super admin cannot be removed
          // ordinary admins can be removed by super admin

          System.out.println("You are now going to remove administrator account...");
          if (!Misc.confirmAction(scanner)) break;
          // sure = Input.readStringSafe(scanner, "Type YES to confirm removing administrator
          // account: ");
          // if (!"YES".equalsIgnoreCase(sure)) {
          //    System.out.println("Wrong input, try again.");
          //    break;
          // }
          String removeAdminLogin =
              Input.readStringSafe(
                  scanner, "Enter login of the ordinary administrator to remove: ");
          result = USERS.removeAdmin(removeAdminLogin);
          if (result) {
            System.out.println("Admin account removed successfully");
          } else {
            System.out.println("Admin account removal failed");
          }
          break;
        case 7:
          System.out.println("You are now going to remove all saved data...");
          // TO FIX think about how to solve the issue, that the file will be recreated on exit...
          try {
            Files.deleteIfExists(DATA_FILE);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          break;
        case 8:
          System.out.println("You are going to return to main menu");
          return;
        default:
          System.out.println("Invalid option, Choose  1-6");
          System.out.println("> ");
          break;
      }
    }
  }

  private static void runOrdinaryAdminMenu() {
    ConsoleMenu.showOrdinaryAdminMenu();
    int option = Input.readIntSafe(scanner);
    switch (option) {
      case 1:
        System.out.println("Case1");
        // TO FIX: implement logic here
        break;
      case 2:
        System.out.println("Case2");
        // TO FIX: implement logic here
        break;
      case 3:
        System.out.println("You are going to return to main menu");
        return;
      default:
        System.out.println("Invalid option, Choose  1-3");
    }
  }
}
