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
    /*private static void showFirstMenu() {
        if (currentUser !=null) {
            System.out.println("Logged in as: " + currentUser.login);
        }else {
            System.out.println("Status: You are not logged in");
        }
        System.out.println("You are now in the main menu. Please select an option:");
        System.out.println("1. Log in");
        System.out.println("2. Log out");
        System.out.println("3. View documentation");
        System.out.println("4. Log in as administrator");
        System.out.println("5. Exit");
        System.out.println("> ");
    }
    */


    private static void runFirstMenu() {

        while (true) {
            //showFirstMenu();
            ConsoleMenu.showFirstMenu(currentUser);
            int option = Input.readIntSafe(scanner);
            switch (option) {
                case 1:
                    //System.out.println("Login: ");
                    String login = readLoginSafe();
                    User u = USERS.find(login);
                    if (u == null) {
                        System.out.println("User not found. Creating new user...");
                        String name = Input.readStringSafe(scanner, "Please enter your name: ", true);
                        //System.out.println("Name: ");  //TO FIX revert to readStringSafe method
                        //String name = scanner.nextLine();
                        String surname = Input.readStringSafe(scanner, "Please enter your surname: ", true);
                        //System.out.println("Surname: "); //TO FIX revert to readStringSafe method
                        //String surname = scanner.nextLine();
                        String pass = Input.readStringSafe(scanner, "Please enter your password: ");
                        //System.out.println("Password: "); //TO FIX revert to readStringSafe method
                        //String pass = scanner.nextLine();
                        u = USERS.register(login, name, surname, pass);
                        System.out.println("User created successfully: " + u.toString());
                    } else {
                        //System.out.println("Password: ");
                        //String pass = scanner.nextLine();
                        String pass = Input.readStringSafe(scanner, "Please enter your password: ");
                        if (USERS.authenticate(login, pass) == null) {
                            System.out.println("Wrong password");
                            System.out.println("> ");
                            break;
                        }
                        System.out.println("Logged in successfully: " + u.toString());

                    }
                    currentUser = u;
                    //showSecondMenu();
                    runSecondMenu();
                    //showFirstMenu();
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
                    USERS.listAllUsers();
                    System.out.println("> ");
                    break;
                case 4:
                    System.out.println("Your are going to enter administrator menu");
                    //TO FIX implement admin authorization
                    if (currentUser != null && currentUser.getAdmin()) {
                        runAdminMenu();
                    }
                    else {
                        System.out.println("You are not administrator");
                        System.out.println("Log in as administrator and try again");
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
    private static void showSecondMenu() {
        System.out.println("==========================");
        System.out.println("You are now in the Actions menu. Please select an option:");
        System.out.println("1. Add income");
        System.out.println("2. Add expense");
        System.out.println("3. View wallet");
        System.out.println("4. Add budget");
        System.out.println("5. View statistics");
        System.out.println("6. Transfer money to other user");
        System.out.println("7. Delete my user account");
        System.out.println("8. Return to main menu");
        System.out.println("> ");
    }
    private static void runSecondMenu() {
        while (true) {
            if (!(currentUser == null)) {
                System.out.println("Logged in as: " + currentUser.login);
            }
            else {
                System.out.println("Status: You are not logged in");
                break;
            }
            showSecondMenu();
            int option = Input.readIntSafe(scanner);
            switch (option) {
                case 1:
                    System.out.println("You are going to add income");
                    //System.out.println("Enter income amount:");
                    //double incomeAmount = readDoubleSafe();
                    double incomeAmount = Input.readDoubleSafe(scanner, "Enter income amount");
                    String incomeTitle = Input.readStringSafe(scanner, "Enter income title");
                    currentUser.wallet.addTransaction(incomeAmount, incomeTitle, Transaction.Type.INCOME);
                    System.out.println("Income added successfully: " + incomeAmount + " (" + incomeTitle + ")");
                    break;
                case 2:
                    System.out.println("You are going to add expense");
                    //System.out.println("Enter expense amount:");
                    //double expenseAmount = readDoubleSafe();
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
    private static void showAdminMenu() {
        System.out.println("==========================");
        System.out.println("You are now in the Administrator menu. Please select an option:");
        System.out.println("1. View all users");
        System.out.println("2. Delete all users");
        System.out.println("3. Add administrator");
        System.out.println("4. Remove administrator");
        System.out.println("5. Remove saved data");
        System.out.println("6. Return to main menu");
        System.out.println("> ");
    }
    private static void runAdminMenu() {
        while (true) {
            showAdminMenu();
            int option = Input.readIntSafe(scanner);
            switch (option) {
                case 1:
                    break;
                case 2:
                    break;
                case 3:
                    System.out.println("You are now going to add administrator account...");
                    List<User> allUsers = USERS.listAll();
                    //List <User> adminUsers = List.of();
                    //List <User> otherUsers = List.of();
                    //TO FIX find ou how to create list of objects and inroduce two types of objects - admins and not admins
                    System.out.println("The current administrators are: ");
                    for (User u : allUsers) {
                        if (u.getAdmin()) {
                            //adminUsers.add(u);
                            System.out.println(u.name + " " + u.surname);
                        }
                    }
                    System.out.println("All other users are: ");
                    for (User u : allUsers) {
                        if (!(u.getAdmin())) {
                            System.out.println(u.name + " " + u.surname);
                        }
                    }
                    String sure = Input.readStringSafe(scanner, "Type YES to confirm changing administrator: ");
                    if (!"YES".equalsIgnoreCase(sure)) {
                        System.out.println("Wrong input, try again.");
                        break;
                    }
                    String pass = Input.readStringSafe(scanner, "Enter your password to confirm changing administrator: ");
                    String newAdminLogin = Input.readStringSafe(scanner, "Enter new administrator login: ");


                    boolean result = USERS.changeAdmin(currentUser.login, pass, newAdminLogin);
                    if (result) {
                        System.out.println("Admin changed successfully");
                    } else {
                        System.out.println("Admin change failed");
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
                    System.out.println("Invalid option, Choose  1-4");
                    System.out.println("> ");
                    break;
            }
        }
    }


    //TO FIX switch to the Input class.
    /*private static int readIntSafe() {
        while(!scanner.hasNextInt()) {
            scanner.nextLine();
            System.out.println("Please enter a valid number: ");
        }
        int v = scanner.nextInt();
        scanner.nextLine();
        return v;
    }
    */


    /*
    private static double readDoubleSafe() {
        while(true) {
            String s = scanner.nextLine().trim();
            try {
                double v = Double.parseDouble(s.replace(',', '.'));
                if (Double.isNaN(v) || Double.isInfinite(v)) throw new NumberFormatException();
                return v;

            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number: ");
            }
        }
    }
    /*
    private static String readStringSafe(String prompt) {
        while (true) {
            System.out.println(prompt);
            String s = scanner.nextLine();
            if (s != null) {
                s = s.trim();
                if (!s.isEmpty()) return s;
            }
            System.out.println("Input cannot be empty. Try again. ");
        }
    }
    */

    private static String readLoginSafe(){
        while (true) {
            System.out.println("Please, enter login (3-32, start with letter, letters/digits/._-): ");
            String s = scanner.nextLine().trim().toLowerCase();// normalazing to lower case
            if (s.matches("^[a-z][a-z0-9._-]{2,31}$")) {
                return s;
            }
            System.out.println("Login must be between 3 and 32 characters long and contain only letters, digits, dots, dashes and underscores. Try again. ");
        }
    }
}