package org.example.util;

//class to display all my console menus,
//stores all menu in one place, removes clutter from the main.java

import org.example.model.User;

public class ConsoleMenu {
    private ConsoleMenu() {}

    //main actions menu
    public static void showMainActionsMenu() {
        System.out.println("==========================");
        System.out.println("You are now in the Main Actions menu. Please select an option:");
        System.out.println("1. Add income");
        System.out.println("2. Add expense");
        System.out.println("3. View wallet");
        System.out.println("4. Add budget");
        System.out.println("5. View statistics");
        System.out.println("6. Transfer money to other user");
        System.out.println("7. Delete my user account");
        System.out.println("8. Return to the previous menu");
        System.out.println("> ");
    }
    //super admin menu
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
    //ordinary admin menu
    public static void showOrdinaryAdminMenu() {
        System.out.println("==========================");
        System.out.println("You are now in the Administrator menu. Please select an option:");
        System.out.println("1. View all users");
        System.out.println("2. View statistics for all users");
        System.out.println("3. Delete a user account");
        System.out.println("4. Return to the previous menu");
        System.out.println("> ");
    }
    //LogIn menu
    public static void showLogInMenu () {
        System.out.println("You are now in the LogIn menu. Please select an option:");
        System.out.println("1. Log in");
        System.out.println("2. View documentation");
        System.out.println("3. Exit");
        System.out.println("> ");
    }
    //actions menu
    public static void showActionsMenu () {
        System.out.println("You are now in the Actions menu. Please select an option:");
        System.out.println("1. Main actions");
        System.out.println("2. Administrator actions");
        System.out.println("3. Log out");
        System.out.println("4. Exit");
        System.out.println("> ");
    }

}

