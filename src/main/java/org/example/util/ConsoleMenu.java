package org.example.util;

//class to display all my console menus,
//stores all menu in one place, removes clutter from the main.java

import org.example.model.User;

public class ConsoleMenu {
    private ConsoleMenu() {}

    public static void showFirstMenu() {
        System.out.println("You are now in the main menu. Please select an option:");
        System.out.println("1. Log in");
        System.out.println("2. Log out");
        System.out.println("3. View documentation");
        System.out.println("4. Administrator actions");
        System.out.println("5. Exit");
        System.out.println("> ");
    }
    public static void showSecondMenu() {
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
    public static void showSuperAdminMenu() {
        System.out.println("==========================");
        System.out.println("You are now in the Super Administrator menu. Please select an option:");
        System.out.println("1. View all users");
        System.out.println("2. Delete all users");
        System.out.println("3. Add administrator");
        System.out.println("4. Remove administrator");
        System.out.println("5. Remove saved data");
        System.out.println("6. Return to main menu");
        System.out.println("> ");
    }
    public static void showOrdinaryAdminMenu() {
        System.out.println("==========================");
        System.out.println("You are now in the Administrator menu. Please select an option:");
        System.out.println("1. View all users");
        System.out.println("2. View statistics for all users");
        System.out.println("3. Return to main menu");
        System.out.println("> ");
    }

}

