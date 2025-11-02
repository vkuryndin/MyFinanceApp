package org.example.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class UserTest {

    @Test
    @DisplayName("Password is checked using checkPassword method; roles can be added/removed")
    void passwordAndRoles() {
        User u = new User(1L, "admin", "Ivan", "Petrov", "secret", false);

        assertTrue(u.checkPassword("secret"));
        assertFalse(u.checkPassword("wrong"));

        assertFalse(u.hasRole(User.Role.ADMIN));
        u.addRole(User.Role.ADMIN);
        assertTrue(u.hasRole(User.Role.ADMIN));

        u.removeRole(User.Role.ADMIN);
        assertFalse(u.hasRole(User.Role.ADMIN));
    }
}
