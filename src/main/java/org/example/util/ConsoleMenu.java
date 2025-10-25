package org.example.util;

//class to display all my console menus,
//stores all menu in one place, removes clutter from the main.java

import org.example.model.User;

public class ConsoleMenu {
    private ConsoleMenu() {}

    public static void showFirstMenu(User currentUser) {
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
}
