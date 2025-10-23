package org.example;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        showFirstMenu();
        runFirstMenu();
    }
    private static void showFirstMenu() {
        System.out.println("Welcome to my finance app");
        System.out.println("You are now in the main menu. Please select an option:");
        System.out.println("1. Log in");
        System.out.println("2. Log out");
        System.out.println("3. View documentation");
        System.out.println("4. Exit");
        System.out.println("> ");
    }
    private static void runFirstMenu() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            int option = scanner.nextInt();
            switch (option) {
                case 1:
                    System.out.println("You have logged in");
                    showSecondMenu();
                    runSecondMenu(scanner);
                    showFirstMenu();
                    break;
                case 2:
                    System.out.println("You have logged out");
                    break;
                case 3:
                    System.out.println("You have viewed documentation");
                    break;
                case 4:
                    System.out.println("You have exited");
                    //scanner.close();
                    return;
            }
        }
    }
    private static void showSecondMenu() {
        System.out.println("You are now in the Actions menu. Please select an option:");
        System.out.println("1. Add income");
        System.out.println("2. Add expense");
        System.out.println("3. View wallet");
        System.out.println("4. Add budget");
        System.out.println("5. View statistics");
        System.out.println("6. Return to main menu");
        System.out.println("> ");
    }
    private static void runSecondMenu(Scanner scanner) {
        while (true) {
            int option = scanner.nextInt();
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
            }
        }
    }
}