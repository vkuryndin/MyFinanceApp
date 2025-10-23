package org.example;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class Main {

   private static final Scanner scanner = new Scanner(System.in);
   private static UsersRepo USERS = new org.example.UsersRepo();
   private static org.example.User currentUser = null;
   private static final Path DATA_FILE = Paths.get("data", "finance-data.json");

    public static void main(String[] args) {
        System.out.println("Welcome to my finance app");
        System.out.println("==========================");
        //showFirstMenu();
        USERS = StorageJson.loadOrNew(DATA_FILE);
        runFirstMenu();
    }
    private static void showFirstMenu() {
        if (currentUser !=null) {
            System.out.println("Logged in as: " + currentUser.login);
        }else {
            System.out.println("Status: You are not logged in");
        }
        System.out.println("You are now in the main menu. Please select an option:");
        System.out.println("1. Log in");
        System.out.println("2. Log out");
        System.out.println("3. View documentation");
        System.out.println("4. Exit");
        System.out.println("> ");
    }
    private static void runFirstMenu() {

        while (true) {
            showFirstMenu();
            int option = readIntSafe();
            switch (option) {
                case 1:
                    System.out.println("Login: ");
                    String login = scanner.nextLine();
                    org.example.User u = USERS.find(login);
                    if (u == null) {
                        System.out.println("User not found. Creating new user...");
                        System.out.println("Name: ");  //TO FIX revert to readStringSafe method
                        String name = scanner.nextLine();
                        System.out.println("Surname: "); //TO FIX revert to readStringSafe method
                        String surname = scanner.nextLine();
                        System.out.println("Password: "); //TO FIX revert to readStringSafe method
                        String pass = scanner.nextLine();
                        u = USERS.register(login, name, surname, pass);
                        System.out.println("User created successfully: " + u.toString());
                    } else {
                        System.out.println("Password: ");
                        String pass = scanner.nextLine();
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
                    }else {
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
                    System.out.println("You have exited");
                    StorageJson.save(DATA_FILE, USERS);
                    scanner.close();
                    return;
                default:
                    System.out.println("Invalid option, Choose  1-4");
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
        System.out.println("6. Return to main menu");
        System.out.println("> ");
    }
    private static void runSecondMenu() {
        while (true) {
            if (!(currentUser == null)) {System.out.println("Logged in as: " + currentUser.login);}
            showSecondMenu();
            int option = readIntSafe();
            switch (option) {
                case 1:
                    System.out.println("You are going to add income");
                    System.out.println("Enter income amount:");
                    double incomeAmount = readDoubleSafe();
                    String incomeTitle = readStringSafe("Enter income title");
                    currentUser.wallet.addTransaction(incomeAmount, incomeTitle, Transaction.Type.INCOME);
                    System.out.println("Income added successfully: " + incomeAmount + " (" + incomeTitle + ")");
                    break;
                case 2:
                    System.out.println("You are going to add expense");
                    System.out.println("Enter expense amount:");
                    double expenseAmount = readDoubleSafe();
                    String expenseTitle = readStringSafe("Enter expense title:");
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

                    //transactions
                    var txs = currentUser.wallet.getTransactions();
                    if (txs.isEmpty()) {System.out.println("No transactions yet");}
                    else {
                        System.out.println("Transactions:");
                        for (Transaction t : txs) {
                            System.out.println("- " + t);
                        }
                    }

                    //budgets
                    var budgets = currentUser.wallet.getBudgets();
                    if (budgets.isEmpty()) {System.out.println("No budgets yet");}
                    else {
                        System.out.println("Budgets:");
                        for (var e : budgets.entrySet()) {
                            String cat = e.getKey();
                            double limit = e.getValue();
                            double spent = currentUser.wallet.getSpentByCategory(cat);
                            double rem = currentUser.wallet.getRemainingBudget(cat);
                            System.out.println("- " + cat + ": " + limit + ", spent: " + spent + ", remaining: " + rem);
                        }
                    }

                    //alerts
                    var alerts = currentUser.wallet.getbudgetAlerts();
                    for (String a : alerts) {System.out.println("! " +a);}
                    break;
                case 4:
                    System.out.println("You are going to add budget");
                    String cat = readStringSafe("Enter category name: ");
                    System.out.println("Enter budget limit: ");
                    double limit = readDoubleSafe();
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
                    if (incMap.isEmpty()) {System.out.println("No incomes yet");}
                    else {
                        System.out.println("Incomes by category:");
                        for (var e : incMap.entrySet()) {
                            System.out.println("- " + e.getKey() + ": " + e.getValue());

                        }
                    }
                    var expMap = currentUser.wallet.expensesByCategory();
                    if (expMap.isEmpty()) {System.out.println("No expenses yet");}
                    else {
                        System.out.println("Expenses by category:");
                        for (var e : expMap.entrySet()) {
                            System.out.println("- " + e.getKey() + ": " + e.getValue());
                        }
                    }
                    System.out.println("==========================");
                    break;
                case 6:
                    System.out.println("You are going to return to main menu");
                    //showFirstMenu();
                    return;
                default:
                    System.out.println("Invalid option, Choose  1-6");
                    System.out.println("> ");
                    break;
            }
        }
    }
    private static int readIntSafe() {
        while(!scanner.hasNextInt()) {
            scanner.nextLine();
            System.out.println("Please enter a valid number: ");
        }
        int v = scanner.nextInt();
        scanner.nextLine();
        return v;
    }
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
}