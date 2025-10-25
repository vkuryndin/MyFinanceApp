package org.example.app;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.example.storage.StorageJson;
import org.example.model.Transaction;
import org.example.repo.UsersRepo;
import org.example.model.User;
import org.example.util.ConsoleMenu;
import org.example.util.Input;
import org.example.util.Misc;

public class Main {

   private static final Scanner scanner = new Scanner(System.in);
   private static UsersRepo USERS = new UsersRepo();
   private static User currentUser = null;
   private static final Path DATA_FILE = Paths.get("data", "finance-data.json");

    public static void main(String[] args) {
        //showFirstMenu();
        USERS = StorageJson.loadOrNew(DATA_FILE);

        //changing the welcome string whether this is the previously saved data exists
        if (USERS.getIsPreviousDataExists()) {
            System.out.println("Welcome back to my finance app");
        }
        else {
            System.out.println("Welcome to my finance app");
        }
        System.out.println("==========================");
        runFirstMenu(); 
    }

    private static void runFirstMenu() {

        while (true) {
            //showFirstMenu();
            Misc.checkLogonStatusSimnple(currentUser);
            ConsoleMenu.showFirstMenu();
            int option = Input.readIntSafe(scanner);
            switch (option) {
                case 1:
                    //System.out.println("Login: ");
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
                        //TO FIX implement password policy
                        String pass = Input.readStringSafe(scanner, "Please enter your password: ");
                        if (USERS.authenticate(login, pass) == null) {
                            System.out.println("Wrong password");
                            System.out.println("> ");
                            break;
                        }
                        System.out.println("Logged in successfully: " + u.toString());
                    }
                    currentUser = u;
                    runSecondMenu();
                    break;
                case 2:
                    if (currentUser == null) {
                        System.out.println("You are not logged in");
                        System.out.println("> ");
                    } else {
                        System.out.println("You have logged out");
                        currentUser = null;
                        System.out.println("> ");
                    }
                    break;
                case 3:
                    System.out.println("You have viewed documentation");
                    System.out.println("> ");
                    break;
                case 4:
                    System.out.println("Your are going to enter either super of ordinary administrator menu depending on the level of your access rights");
                    //check what kind of admin we have to show different menus
                    if (currentUser != null && currentUser.getSuperAdmin()) {
                        runSuperAdminMenu(); //running super admin menu
                    }
                    else if (currentUser!=null && currentUser.getAdmin()) {
                        runOrdinaryAdminMenu(); //running ordinary admin menu
                    }
                    else {
                        System.out.println("You are not administrator");
                        System.out.println("Log in as either super or ordinary administrator and try again");
                    }
                    break;
                case 5:
                    System.out.println("You have exited");
                    StorageJson.save(DATA_FILE, USERS);
                    System.out.println("Saving data to file: " + DATA_FILE.toAbsolutePath());
                    System.out.println("Bye!");
                    scanner.close();
                    return;
                default:
                    System.out.println("Invalid option, Choose  1-5");
                    System.out.println("> ");
                    break;
            }
        }
    }

    private static void runSecondMenu() {
        while (true) {
            if (!Misc.checkLogonStatus(currentUser)) break;
            ConsoleMenu.showSecondMenu();
            int option = Input.readIntSafe(scanner);
            switch (option) {
                case 1:
                    System.out.println("You are going to add income");
                    double incomeAmount = Input.readDoubleSafe(scanner, "Enter income amount");
                    String incomeTitle = Input.readStringSafe(scanner, "Enter income title");
                    currentUser.wallet.addTransaction(incomeAmount, incomeTitle, Transaction.Type.INCOME);
                    System.out.println("Income added successfully: " + incomeAmount + " (" + incomeTitle + ")");
                    break;
                case 2:
                    System.out.println("You are going to add expense");
                    double expenseAmount = Input.readDoubleSafe(scanner, "Enter expense amount");
                    String expenseTitle = Input.readStringSafe(scanner, "Enter expense title:");
                    currentUser.wallet.addTransaction(expenseAmount, expenseTitle, Transaction.Type.EXPENSE);
                    System.out.println("Expense added successfully: " + expenseAmount + " (" + expenseTitle + ")");
                    if (currentUser.wallet.getBudgets().containsKey(expenseTitle)) {
                        double rem = currentUser.wallet.getRemainingBudget(expenseTitle);
                        System.out.println("Remaining budget for " + expenseTitle + ": " + rem);
                        if (rem < 0) {
                            System.out.println("You have exceeded your budget for " + expenseTitle + " by " + (-rem));
                        }
                    }
                    break;
                case 3:
                    System.out.println("You are going to view wallet");
                    //System.out.println(currentUser.wallet.toString());
                    System.out.println("Balance: " + currentUser.wallet.getBalance());

                    //viewing transactions
                    var txs = currentUser.wallet.getTransactions();
                    if (txs.isEmpty()) {
                        System.out.println("No transactions yet");
                    } else {
                        System.out.println("Transactions:");
                        for (Transaction t : txs) {
                            System.out.println("- " + t);
                        }
                    }

                    //viewing budgets
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
                    //viewing alerts
                    var alerts = currentUser.wallet.getbudgetAlerts();
                    for (String a : alerts) {
                        System.out.println("! " + a);
                    }
                    break;
                case 4:
                    System.out.println("You are going to add budget");
                    String cat = Input.readStringSafe(scanner, "Enter category name: ");
                    double limit = Input.readDoubleSafe(scanner, "Enter budget limit: ");
                    //System.out.println("Enter budget limit: ");
                    //double limit = readDoubleSafe();
                    currentUser.wallet.setBudget(cat, limit);
                    System.out.println("Budget added successfully: " + limit + " (" + cat + ")");

                    //showing current spent and the remaining budget
                    double spent = currentUser.wallet.getSpentByCategory(cat);
                    double rem = currentUser.wallet.getRemainingBudget(cat);
                    System.out.println("Spent in: " + cat + ": " + spent + ", remaining: " + rem);
                    break;
                case 5:
                    System.out.println("You are going to view statistics");
                    double totalIncome = currentUser.wallet.sumIncome();
                    double totalExpense = currentUser.wallet.sumExpense();
                    double balance = currentUser.wallet.getBalance();

                    System.out.println("==========================");
                    System.out.println("Wallet statistics");
                    System.out.println("Total income: " + totalIncome);
                    System.out.println("Total expense: " + totalExpense);
                    System.out.println("Balance: " + balance);

                    var incMap = currentUser.wallet.incomesByCategory();
                    if (incMap.isEmpty()) {
                        System.out.println("No incomes yet");
                    } else {
                        System.out.println("Incomes by category:");
                        for (var e : incMap.entrySet()) {
                            System.out.println("- " + e.getKey() + ": " + e.getValue());

                        }
                    }
                    var expMap = currentUser.wallet.expensesByCategory();
                    if (expMap.isEmpty()) {
                        System.out.println("No expenses yet");
                    } else {
                        System.out.println("Expenses by category:");
                        for (var e : expMap.entrySet()) {
                            System.out.println("- " + e.getKey() + ": " + e.getValue());
                        }
                    }
                    System.out.println("==========================");
                    break;
                case 6:
                    String toLogin = Input.readStringSafe(scanner, "Enter login of user to transfer money to: ");
                    //System.out.println("Enter amount to transfer:  ");
                    //double amount = readDoubleSafe();
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
                    String sure = Input.readStringSafe(scanner, "Type YES to confirm account deletion: ");
                    if (!"YES".equalsIgnoreCase(sure)) {
                        System.out.println("Wrong input, try again.");
                        break;
                    }
                    String pass = Input.readStringSafe(scanner, "Enter your password to confirm deletion: ");
                    boolean result = USERS.deleteUser(currentUser.login, pass);
                    if (result) {
                        System.out.println("Account deleted successfully");
                        currentUser = null;
                    } else {
                        System.out.println("Account deletion failed");
                    }
                    break;
                case 8:
                    System.out.println("You are going to return to main menu");
                    //showFirstMenu();
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
            switch (option) {
                case 1:
                    System.out.println("You are going to view all users");
                    USERS.listAllUsers();
                case 2:
                    break;
                case 3:
                    System.out.println("You are now going to add ordinary administrator account...");
                    List<User> allUsers = USERS.listAll();
                    List <User> adminUsers = new ArrayList<>();
                    List <User> otherUsers =  new  ArrayList<>();
                    //TO FIX find ou how to create list of objects and introduce two types of objects - admins and not admins
                    System.out.println("The current administrators are: ");
                    for (User u : allUsers) {
                        if (u.getAdmin()) {
                            adminUsers.add(u);
                            System.out.println(u.name + " " + u.surname);
                        }
                    }
                    System.out.println("All other users are: ");
                    for (User u : allUsers) {
                        if (!(u.getAdmin())) {
                            otherUsers.add(u);
                            System.out.println(u.name + " " + u.surname);
                        }
                    }
                    String sure = Input.readStringSafe(scanner, "Type YES to confirm adding new administrator account: ");
                    if (!"YES".equalsIgnoreCase(sure)) {
                        System.out.println("Wrong input, try again.");
                        break;
                    }
                    String pass = Input.readStringSafe(scanner, "Enter your password to confirm adding new administrator account: ");
                    String newAdminLogin = Input.readStringSafe(scanner, "Enter new ordinary administrator login: ");


                    boolean result = USERS.changeAdmin(currentUser.login, pass, newAdminLogin);
                    if (result) {
                        System.out.println("Admin account added successfully");
                    } else {
                        System.out.println("Admin account addition failed");
                    }
                    break;
                case 4:
                    System.out.println("You are now going to remove administrator account...");
                    break;
                case 5:
                    System.out.println("You are now going to remove all saved data...");
                    break;
                case 6:
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
                break;
            case 2:
                System.out.println("Case2");
                break;
            case 3:
                System.out.println("You are going to return to main menu");
                return;
            default:
                System.out.println("Invalid option, Choose  1-3");
        }
    }
}