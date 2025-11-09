package org.example.app;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import org.example.cli.ConsoleInput;
import org.example.cli.ConsoleMenus;
import org.example.model.User;
import org.example.repo.UsersRepo;
import org.example.storage.StorageJson;
import org.example.util.ConsoleUtils;

public class Main {

  private static final Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
  private static UsersRepo USERS = new UsersRepo();
  private static User currentUser = null;
  private static final Path DATA_FILE = Paths.get("data", "finance-data.json");

  private static boolean isExit =
      false; // this will allow us to exit from the 2 tier (Actions) menu
  private static boolean isloggedOut =
      false; // this will allow us to enter log in menu after user deletion

  public static void main(String[] args) {
    // showFirstMenu();
    System.out.println("==========================");
    USERS = StorageJson.loadOrNew(DATA_FILE);

    // changing the welcome string whether the previously saved data exists
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
        ConsoleMenus.showLoginMenu();
        int option = ConsoleInput.readIntSafe(scanner);
        switch (option) {
          case 1:
            System.out.println("Log in");
            String login = ConsoleInput.readLoginSafe(scanner);
            User u = USERS.find(login);
            if (u == null) {
              System.out.println("User not found. Creating new user...");
              String name = ConsoleInput.readStringSafe(scanner, "Please enter your name: ", true);
              String surname =
                  ConsoleInput.readStringSafe(scanner, "Please enter your surname: ", true);
              String pass = ConsoleInput.readStringSafe(scanner, "Please enter your password: ");
              u = USERS.register(login, name, surname, pass);
              System.out.println("User created successfully: " + u.toString());
            } else {
              // TO FIX implement password policy
              String pass = ConsoleInput.readStringSafe(scanner, "Please enter your password: ");
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
            runDocumentation();
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
        ConsoleMenus.showActionsMenu();
        int option = ConsoleInput.readIntSafe(scanner);
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
      if (!ConsoleUtils.checkLogonStatus(currentUser)) break;
      ConsoleMenus.showMainActionsMenu();
      int option = ConsoleInput.readIntSafe(scanner);
      switch (option) {
        case 1:
          ConsoleUtils.handleAddIncome(scanner, currentUser);
          break;
        case 2:
          ConsoleUtils.handleAddExpense(scanner, currentUser);
          break;
        case 3:
          ConsoleUtils.handleViewWallet(currentUser);
          break;
        case 4:
          ConsoleUtils.handleAddBudget(scanner, currentUser);
          break;
        case 5:
          ConsoleUtils.handleViewStatistics(currentUser);
          break;
        case 6:
          ConsoleUtils.handleTransfer(scanner, currentUser, USERS);
          break;

          // ===== Advanced Statistics Feature=====
        case 7:
          // Advanced statistics: periods + several categories
          ConsoleUtils.handleAdvancedStatistics(scanner, currentUser);
          break;
        case 8:
          // Update budget limit
          ConsoleUtils.handleUpdateBudgetLimit(scanner, currentUser);
          break;
        case 9:
          // Delete a budget
          ConsoleUtils.handleRemoveBudget(scanner, currentUser);
          break;
        case 10:
          // Rename budget category
          ConsoleUtils.handleRenameCategory(scanner, currentUser);
          break;
          // =================
          // export /import to json
        case 11:
          ConsoleUtils.handleExportJson(scanner, currentUser);
          break;
        case 12:
          ConsoleUtils.handleImportJson(scanner, currentUser);
          break;
        case 13:
          if (ConsoleUtils.handleDeleteYourUserAccount(scanner, currentUser, USERS)) {
            currentUser = null;
            isloggedOut = true;
          }
          break;
        case 14:
          System.out.println("You are going to return to main menu");
          return;
        default:
          System.out.println("Invalid option, Choose  1-14");
          System.out.println("> ");
          break;
      }
    }
  }

  private static void runSuperAdminMenu() {
    while (true) {
      ConsoleMenus.showSuperAdminMenu();
      int option = ConsoleInput.readIntSafe(scanner);
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
            ConsoleUtils.handleViewStatistics(u);
          }
          break;
        case 3:
          ConsoleUtils.handleDeleteSelectedUserAccount(scanner, currentUser, USERS);
          break;
        case 4:
          System.out.println("You are going to delete all users except super admin ");
          if (ConsoleUtils.confirmAction(scanner, "deleting all users")) USERS.deleteAllUsers();
          break;
        case 5:
          ConsoleUtils.handleAddOrdinaryAdminAccount(scanner, currentUser, USERS, allUsers);
          break;
        case 6:
          // super admin cannot be removed
          // ordinary admins can be removed by super admin
          ConsoleUtils.handleRemoveOrdinaryAdminAccount(scanner, USERS);
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
          System.out.println("Invalid option, Choose  1-8");
          System.out.println("> ");
          break;
      }
    }
  }

  private static void runOrdinaryAdminMenu() {
    while (true) {
      ConsoleMenus.showOrdinaryAdminMenu();
      int option = ConsoleInput.readIntSafe(scanner);
      switch (option) {
        case 1:
          System.out.println("You are going to view all users");
          USERS.listAllUsers();
          break;
        case 2:
          System.out.println("You are going to view statistics for all users");
          List<User> allUsers = USERS.listAll();
          for (User u : allUsers) {
            System.out.println("Displaying statistics for user: " + u.login);
            ConsoleUtils.handleViewStatistics(u);
          }
          break;
        case 3:
          if (ConsoleUtils.confirmAction(scanner, "deleting the selected user account")) {
            ConsoleUtils.handleDeleteSelectedUserAccount(scanner, currentUser, USERS);
          }
          break;
        case 4:
          System.out.println("You are going to return to main menu");
          return;
        default:
          System.out.println("Invalid option, Choose  1-4");
      }
    }
  }

  private static void runDocumentation() {
    while (true) {
      ConsoleMenus.showDocumenation();
      int option = ConsoleInput.readIntSafe(scanner);
      if (option == 1) {
        return;
      }
    }
  }
}
