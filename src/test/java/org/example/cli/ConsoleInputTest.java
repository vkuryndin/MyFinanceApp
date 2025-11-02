package org.example.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for ConsoleInput methods. Uses explicit UTF-8 encoding to avoid SpotBugs
 * DM_DEFAULT_ENCODING warnings.
 */
public class ConsoleInputTest {

  /** Helper method to create Scanner with explicit UTF-8 encoding. */
  private static Scanner scannerUtf8(String input) {
    return new Scanner(
        new InputStreamReader(
            new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
            StandardCharsets.UTF_8));
  }

  @Test
  @DisplayName("readStringSafe() should trim spaces and return valid string")
  void readStringSafe_basic() {
    try (Scanner sc = scannerUtf8("   Hello World   \n")) {
      String result = ConsoleInput.readStringSafe(sc, "Enter text:");
      assertEquals("Hello World", result);
    }
  }

  @Test
  @DisplayName("readIntSafe() should skip invalid input and return first valid integer")
  void readIntSafe_skipsInvalidAndReadsInteger() {
    try (Scanner sc = scannerUtf8("xx\n-5\n10\n")) {
      int result = ConsoleInput.readIntSafe(sc);
      assertEquals(-5, result); // method takes the first valid integer (even if negative)
    }
  }

  @Test
  @DisplayName("readDoubleSafe() should read decimal values properly")
  void readDoubleSafe_readsDecimal() {
    try (Scanner sc = scannerUtf8("oops\n3.5\n")) {
      double result = ConsoleInput.readDoubleSafe(sc, "Enter amount:");
      assertEquals(3.5, result, 1e-9);
    }
  }

  @Test
  @DisplayName("readDoubleSafe() should support comma as decimal separator")
  void readDoubleSafe_supportsComma() {
    try (Scanner sc = scannerUtf8("junk\n3,5\n")) {
      double result = ConsoleInput.readDoubleSafe(sc, "Enter amount:");
      assertEquals(3.5, result, 1e-9);
    }
  }

  @Test
  @DisplayName("readLoginSafe() should reject invalid and accept valid login")
  void readLoginSafe_validLogin() {
    try (Scanner sc = scannerUtf8("__bad\nok-user1\n")) {
      String result = ConsoleInput.readLoginSafe(sc);
      assertEquals("ok-user1", result);
    }
  }
}
