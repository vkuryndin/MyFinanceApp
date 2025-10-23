package org.example.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.mindrot.jbcrypt.BCrypt;

public class User {
    public final long id;
    public final String login;
    public String name;
    public String surname;

    private String passwordHash;  //no serialization for password
    public final Wallet wallet = new Wallet();


    public User(long id, String login, String name, String surname, String rawPassword) {
        this.id = id;
        this.login = login;
        this.name = name;
        this.surname = surname;
        setPassword(rawPassword);
    }
    @Override
    public String toString() {
        return "Users{" +
                "id=" + id +
                ", login='" + login + '\'' +
                ", name='" + name + '\'' +
                ", surname='" + surname + '\'' +
                '}';
    }
    public void setPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Password cannot be null or blank");
        }
        //this.passwordHash= sha256(rawPassword); //previous implementation
        this.passwordHash = BCrypt.hashpw(rawPassword, BCrypt.gensalt(12));
        System.out.println("Password hash: " + this.passwordHash);

    }
    // using hash function to hash password
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

}
