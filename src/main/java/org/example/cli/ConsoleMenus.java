package org.example.cli;

// class to display all my console menus,
// stores all menu in one place, removes clutter from the main.java

public class ConsoleMenus {
  private ConsoleMenus() {}

  // main actions menu
  public static void showMainActionsMenu() {
    System.out.println("==========================");
    System.out.println("You are now in the User Actions menu. Please select an option:");
    System.out.println("1. Add income");
    System.out.println("2. Add expense");
    System.out.println("3. View wallet");
    System.out.println("4. Add budget");
    System.out.println("5. View statistics");
    System.out.println("6. Transfer money to other user");
    System.out.println("7. Advanced statistics (period / categories)");
    System.out.println("8. Update budget limit");
    System.out.println("9. Remove budget");
    System.out.println("10. Rename budget category");
    System.out.println("11. Export Wallet data to JSON");
    System.out.println("12. Import Wallet data to JSON");
    System.out.println("13. Delete my user account");
    System.out.println("14. Return to the previous menu");
    System.out.println("> ");
  }

  // super admin menu
  public static void showSuperAdminMenu() {
    System.out.println("==========================");
    System.out.println("You are now in the Super Administrator menu. Please select an option:");
    System.out.println("1. View all users");
    System.out.println("2. View statistics for all users");
    System.out.println("3. Delete a user account");
    System.out.println("4. Delete all user accounts");
    System.out.println("5. Add administrator");
    System.out.println("6. Remove administrator");
    System.out.println("7. Remove saved data");
    System.out.println("8. Return to the previous menu");
    System.out.println("> ");
  }

  // ordinary admin menu
  public static void showOrdinaryAdminMenu() {
    System.out.println("==========================");
    System.out.println("You are now in the Administrator menu. Please select an option:");
    System.out.println("1. View all users");
    System.out.println("2. View statistics for all users");
    System.out.println("3. Delete a user account");
    System.out.println("4. Return to the previous menu");
    System.out.println("> ");
  }

  // Login menu
  public static void showLoginMenu() {
    System.out.println("You are now in the LogIn menu. Please select an option:");
    System.out.println("1. Log in");
    System.out.println("2. View documentation");
    System.out.println("3. Exit");
    System.out.println("> ");
  }

  // actions menu
  public static void showActionsMenu() {
    System.out.println("You are now in the Actions menu. Please select an option:");
    System.out.println("1. User actions");
    System.out.println("2. Administrator actions");
    System.out.println("3. Log out");
    System.out.println("4. Exit");
    System.out.println("> ");
  }
  public static void showDocumenation() {
      System.out.println("==========================");
      System.out.println("MyFinanceApp Quick Start Guide");
      System.out.println("Welcome to MyFinanceApp Quick Start Guide.");
      System.out.println("The app is a simple multi-user application allowing you and your friends to manage your personal finances");
      System.out.println("The first user to log in to the program becomes a super admin, who can manage other admins,  ");
      System.out.println("view advanced statistics, manage users and storage data");
      System.out.println("To create a new user just log in with the nonexistent login, new user will be created automatically");
      System.out.println(" ");
      System.out.println("As a user you can add income, add expense, set a bugget for expense categories, ");
      System.out.println("update limits for budget categories, remove or rename your budget categories");
      System.out.println(" ");
      System.out.println("You can also transfer your money to another user registered in the application.");
      System.out.println(" ");
      System.out.println("If you no longer want to use the app you can delete your user account");
      System.out.println("(pay attention, that the first user (e.g. super account) cannot be deleted)");
      System.out.println(" ");
      System.out.println("You can also use the Export/Import wallet feature. With this feature you can ");
      System.out.println("add expenses and categories (so called transactions) in a bulk by directly editing the wallet.json file ");
      System.out.println(" ");
      System.out.println("All users, wallets, transactions are saved to the /data/finance-data.json file");
      System.out.println("when you exit the program");
      System.out.println("This file is used as some kind of local database.");
      System.out.println("If you want to start anew, simnply delete the /data/finance-data.json, the app will start with no data. ");
      System.out.println(" ");
      System.out.println(" ");
      System.out.println("==========================");
      System.out.println("Press 1 to return to the Login menu...");


  }
}
