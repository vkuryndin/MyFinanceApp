package org.example.repo;

import org.example.model.Transaction;
import org.example.model.User;

import java.util.*;

public class UsersRepo {
    private final Map<String, User> byLogin = new HashMap<>();
    private final Map<Long, User> byId = new HashMap<>();
    private long nextId = 1L;
    //private static int firstUserCounter = 1;

    private boolean isPreviousDataExists = false;  //special flag to decide whether we load UserRepo from file or not

    public User register (String login, String name, String surname, String rawPassword) {
        login = normalizeLogin(login);
        if (!isValidLogin(login)) {throw new IllegalArgumentException("Invalid login format");}
        User u = byLogin.get(login);
        if (u ==null) {
            u = new User(nextId++, login, name, surname, rawPassword, isPreviousDataExists);
            byLogin.put(login, u);
            byId.put(u.id, u);
        }
        return u;

    }
    public User authenticate(String login, String rawPassword) {
        login = normalizeLogin(login);
        User u = byLogin.get(login);
        return (u !=null && u.checkPassword(rawPassword)) ? u : null;
    }

    public User find (String login) { return byLogin.get(normalizeLogin(login));}
    public List<User> listAll () { return new ArrayList<>(byLogin.values());}

    private static String normalizeLogin(String login) {
        return login == null ? null : login.trim().toLowerCase();
    }
    private static boolean isValidLogin (String login) {
        return login !=null && login.matches("^[a-z][a-z0-9._-]{2,31}$");
    }

    // transfer money from one user to another
    public boolean transfer (String fromLogin, String toLogin, double amount, String title) {
        if (fromLogin == null || toLogin == null ) throw new IllegalArgumentException("Login cannot be null");
        if (fromLogin.equals(toLogin)) throw new IllegalArgumentException("Cannot transfer money to self");
        if (amount <=0 || Double.isNaN(amount) || Double.isInfinite(amount))
            throw new IllegalArgumentException("Amount must be positive and finite number");
        User from = byLogin.get(fromLogin);
        User to = byLogin.get(toLogin);
        if (from == null) throw new IllegalArgumentException("Sender not found: " + fromLogin);
        if (to == null) throw new IllegalArgumentException("Recipient not found: " + toLogin);

        // naming operations
        String noteOut = (title == null || title.isBlank())
                ? "transfer to "  +toLogin
                : "transfer to " + toLogin + " | " + title;
        String noteIn = (title==null || title.isBlank())
                ? "transfer from " + fromLogin
                : "transfer from " + fromLogin + " | " + title;

        from.wallet.addTransaction(amount, noteOut, Transaction.Type.EXPENSE);
        to.wallet.addTransaction(amount, noteIn, Transaction.Type.INCOME);
        return true;

    }
    public boolean deleteUser (String login) {
        User u = byLogin.remove(normalizeLogin(login));
        if (u !=null) {
            byLogin.remove(u.login);
            byId.remove(u.id);
            return true;
        }
        return false;
    }
    public boolean deleteUser (String login, String pass) {
        if (login == null || pass == null) {
            //throw new IllegalArgumentException("Login and password cannot be null");
            return false;
        }
        User u = byLogin.get(normalizeLogin(login));
        if (u ==null || !u.checkPassword(pass)){
            //throw new IllegalArgumentException("Invalid login or password");
            return false;
        }
        byLogin.remove(login);
        byId.remove(u.id);
        return true;
    }

    public boolean changeAdmin (String login, String pass, String newAdminLogin) {
        if (login == null || pass == null) {
            System.out.println("login or pass cannot be null");
            return false;
        }
        User u = byLogin.get(normalizeLogin(login));
        if (u ==null || !u.checkPassword(pass)) {
            System.out.println("Wrong password: " + pass +", or user not found");
            return false;
        }
        User newAdmin = byLogin.get(normalizeLogin(newAdminLogin));
        if (newAdmin==null) {
            System.out.println("Wrong login: " + newAdminLogin);
            return false;
        }
        if (newAdmin.getAdmin())
        {
            System.out.println("This user is already an admin " + newAdminLogin);
            return false;
        }
        //TO FIX: decide whether my app will have only one admin!
        //right now we will allow to have multiply admins
        newAdmin.setAdmin(true);
        //u.setAdmin(false);
        return true;
    }
    public void listAllUsers () {
        System.out.println("List of all users: ");
        for (User u : byLogin.values()) {
            System.out.println(u);
        }
    }

    // setters and getters for previous data exists
    public void setIsPreviousDataExists(boolean value) {
        this.isPreviousDataExists = value;
    }
    public boolean getIsPreviousDataExists() {return isPreviousDataExists;}

}
