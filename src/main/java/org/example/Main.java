package org.example;

import java.util.Scanner;

public class Main {

   private static final Scanner scanner = new Scanner(System.in);
   private static final UsersRepo USERS = new org.example.UsersRepo();
   private static org.example.User currentUser = null;

    public static void main(String[] args) {
        System.out.println("Welcome to my finance app");
        System.out.println("==========================");
        //showFirstMenu();
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
                        System.out.println("Name: ");
                        String name = scanner.nextLine();
                        System.out.println("Surname: ");
                        String surname = scanner.nextLine();
                        System.out.println("Password: ");
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
                    showSecondMenu();
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
            int option = readIntSafe();
            switch (option) {
                case 1:
                    System.out.println("You are going to add income");
                    break;
                case 2:
                    System.out.println("You are going to add expense");
                    break;
                case 3:
                    System.out.println("You are going to view wallet");
                    break;
                case 4:
                    System.out.println("You are going to add budget");
                    break;
                case 5:
                    System.out.println("You are going to view statistics");
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
}