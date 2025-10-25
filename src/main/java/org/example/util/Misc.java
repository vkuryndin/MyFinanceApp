package org.example.util;

import org.example.model.User;

public class Misc {
    private Misc() {}

    public static boolean checkLogonStatus(User currentUser) {
        if (!(currentUser == null)) {
            System.out.println("Logged in as: " + currentUser.login + " || " + currentUser.getAdminStatus());
            return true;
        }
        else {
            System.out.println("Status: You are not logged in");
            return false;
        }
    }
    public static void checkLogonStatusSimnple (User currentUser){
        if (currentUser !=null) {
            System.out.println("Logged in as: " + currentUser.login + " || " + currentUser.getAdminStatus());
        }else {
            System.out.println("Status: You are not logged in");
        }
    }
}
