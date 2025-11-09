package org.example.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link ConsoleInput} utility methods.
 *
 * <p>These tests verify how user input is parsed and validated across multiple data types, ensuring
 * predictable behavior regardless of platform defaults or unexpected user input.
 *
 * <p><b>Covered behaviors:</b>
 *
 * <ul>
 *   <li>Trimming leading/trailing whitespace and rejecting empty input
 *   <li>Skipping invalid tokens until a valid value is provided
 *   <li>Handling integer parsing, including negative values when allowed
 *   <li>Supporting both dot and comma as decimal separators
 *   <li>Validating login format and rejecting malformed logins
 * </ul>
 *
 * <p><b>Encoding:</b> UTF-8 is used explicitly to guarantee consistent behavior across environments
 * and to avoid warnings related to default system charset usage.
 *
 * @see org.example.cli.ConsoleInput
 */
public class ConsoleInputTest {

  /**
   * Creates a {@link Scanner} instance that reads the provided input string using UTF-8 encoding.
   *
   * @param input simulated console input text
   * @return scanner configured to read in UTF-8
   */
  private static Scanner scannerUtf8(String input) {
    return new Scanner(
        new InputStreamReader(
            new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
            StandardCharsets.UTF_8));
  }

  /**
   * Verifies that {@link ConsoleInput#readStringSafe(Scanner, String)} trims leading/trailing
   * whitespace and returns the non-empty string as entered by the user.
   */
  @Test
  @DisplayName("readStringSafe() should trim spaces and return valid string")
  void readStringSafe_basic() {
    try (Scanner sc = scannerUtf8("   Hello World   \n")) {
      String result = ConsoleInput.readStringSafe(sc, "Enter text:");
      assertEquals("Hello World", result);
    }
  }

  /**
   * Verifies that {@link ConsoleInput#readIntSafe(Scanner)} skips invalid tokens until the first
   * valid integer is found. Negative integers are accepted if allowed by implementation.
   */
  @Test
  @DisplayName("readIntSafe() should skip invalid input and return first valid integer")
  void readIntSafe_skipsInvalidAndReadsInteger() {
    try (Scanner sc = scannerUtf8("xx\n-5\n10\n")) {
      int result = ConsoleInput.readIntSafe(sc);
      assertEquals(-5, result); // method takes the first valid integer (even if negative)
    }
  }

  /**
   * Verifies that {@link ConsoleInput#readDoubleSafe(Scanner, String)} reads decimal numbers using
   * a dot as a decimal separator and ignores preceding invalid tokens.
   */
  @Test
  @DisplayName("readDoubleSafe() should read decimal values properly")
  void readDoubleSafe_readsDecimal() {
    try (Scanner sc = scannerUtf8("oops\n3.5\n")) {
      double result = ConsoleInput.readDoubleSafe(sc, "Enter amount:");
      assertEquals(3.5, result, 1e-9);
    }
  }

  /**
   * Verifies that {@link ConsoleInput#readDoubleSafe(Scanner, String)} supports a comma as a
   * decimal separator in addition to a dot, allowing for locale-like inputs.
   */
  @Test
  @DisplayName("readDoubleSafe() should support comma as decimal separator")
  void readDoubleSafe_supportsComma() {
    try (Scanner sc = scannerUtf8("junk\n3,5\n")) {
      double result = ConsoleInput.readDoubleSafe(sc, "Enter amount:");
      assertEquals(3.5, result, 1e-9);
    }
  }

  /**
   * Verifies that {@link ConsoleInput#readLoginSafe(Scanner)} continues prompting until an invalid
   * login is rejected and a valid login string is provided.
   *
   * <p>In this scenario, {@code "__bad"} is rejected and {@code "ok-user1"} is accepted.
   */
  @Test
  @DisplayName("readLoginSafe() should reject invalid and accept valid login")
  void readLoginSafe_validLogin() {
    try (Scanner sc = scannerUtf8("__bad\nok-user1\n")) {
      String result = ConsoleInput.readLoginSafe(sc);
      assertEquals("ok-user1", result);
    }
  }
}
