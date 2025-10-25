package org.example.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.mindrot.jbcrypt.BCrypt; //importing special crypto library to deal with password hashing

public class User {
    public final long id;
    public final String login;
    public String name;
    public String surname;
    private boolean isSuperAdmin;  //Super Admin can be only one. it will be the first user to enter to the program.
    private boolean isAdmin;  // ordinary admin.

    private String passwordHash;
    public final Wallet wallet = new Wallet();


    public User(long id, String login, String name, String surname, String rawPassword, boolean isDataExists)  {
        this.id = id;
        this.login = login;
        this.name = name;
        this.surname = surname;
        setPassword(rawPassword);
        if (id ==1) this.isSuperAdmin = true;
       // I decided to create superadmin here and other admins.
        // if (id ==1 && !isDataExists) this.isAdmin = true;  // we check here , whether this is the first user and we have no previous data
        //
    }
    @Override
    public String toString() {
        return "Users{" +
                "id=" + id +
                ", login='" + login + '\'' +
                ", name='" + name + '\'' +
                ", surname='" + surname + '\'' +
                ", is administrator='" + isAdmin + '\'' +
        '}';
    }
    public void setPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Password cannot be null or blank");
        }
        //this.passwordHash= sha256(rawPassword); //previous implementation
        this.passwordHash = BCrypt.hashpw(rawPassword, BCrypt.gensalt(12));  //12 is the number of rounds of hashing (simple enought for our purposes)
        System.out.println("Password hash: " + this.passwordHash);

    }
    // using hash function to hash password, previous implementation - not used now.
    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    public boolean checkPassword(String rawPassword) {
        System.out.println("Checking password for user " + this.login);
        System.out.println("Raw password: " + rawPassword);
        System.out.println("Password hash: " + this.passwordHash);
        //return this.passwordHash !=null && this.passwordHash.equals(sha256(rawPassword)); //previous implementation
        return this.passwordHash !=null && BCrypt.checkpw(rawPassword, this.passwordHash);
    }

    public void setAdmin(boolean value) {
        isAdmin = value;
    }
    public boolean getAdmin () {return isAdmin;}

    public boolean getSuperAdmin () {return isSuperAdmin;}

    public String getAdminStatus () {
        String s;
        if (isAdmin) {
            s = "Ordinary Administrator";
        }
        else if (isSuperAdmin) {
            s= "Super Administrator";
        }
        else {
            s = "None";
        }
        return "Administrator status: " + s;
    }

}
