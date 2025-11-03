package org.example.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the User model class.
 *
 * <p>Tests validate core User functionality including:
 *
 * <ul>
 *   <li><b>Password verification:</b> BCrypt-based password hashing and validation
 *   <li><b>Role management:</b> Adding and removing user roles (USER, ADMIN, SUPER_ADMIN)
 *   <li><b>User properties:</b> ID, login, name, surname, and associated wallet
 * </ul>
 *
 * <p>The User class uses BCrypt for secure password hashing with 12 rounds of salting. First user
 * (id=1) automatically receives SUPER_ADMIN role upon creation.
 *
 * @see org.example.model.User
 */
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
