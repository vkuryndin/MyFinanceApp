package org.example;

import java.util.*;

public class UsersRepo {
    private final Map<String, User> byLogin = new HashMap<>();
    private final Map<Long, User> byId = new HashMap<>();
    private long nextId = 1L;

    public User register (String login, String name, String surname, String rawPassword) {
        User u = byLogin.get(login);
        if (u ==null) {
            u = new User(nextId++, login, name, surname, rawPassword);
            byLogin.put(login, u);
            byId.put(u.id, u);
        }
        return u;

    }
    public User authenticate(String login, String rawPassword) {
        User u = byLogin.get(login);
        return (u !=null && u.checkPassword(rawPassword)) ? u : null;
    }

    public User find (String login) { return byLogin.get(login);}
    public List<User> listAll () { return new ArrayList<>(byLogin.values());}



}
