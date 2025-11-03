package org.example.cli;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.Scanner;
import java.util.Set;

public class ConsoleInput {
  private ConsoleInput() {}

  private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

  // reading string safely from the console
  // this is used for entering text to the app
  public static String readStringSafe(Scanner scanner, String prompt) {
    while (true) {
      System.out.println(prompt);
      System.out.println("> ");
      String s = scanner.nextLine();
      if (s != null) {
        s = s.trim();
        if (!s.isEmpty()) return s;
      }
      System.out.println("Input cannot be empty. Try again. ");
    }
  }

  public static String readStringSafe(Scanner scanner, String prompt, boolean disallowDigits) {
    while (true) {
      System.out.println(prompt);
      System.out.println("> ");
      String s = scanner.nextLine();
      if (s == null) s = "";
      s = s.trim();

      if (s.isEmpty()) {
        System.out.println("Input cannot be empty. Try again. ");
        continue;
      }
      if (disallowDigits) {
        if (!s.matches("(?U)^[\\p{L}][\\p{L} '\\-]*$")) {
          System.out.println("Only letters, spaces, '-' and apostrophes are allowed. Try again. ");
          continue;
        }
      }
      return s;
    }
  }

  // reading double safely from the console
  // this is used for entering numerics to the app
  public static double readDoubleSafe(Scanner scanner, String prompt) {
    while (true) {
      System.out.println(prompt);
      System.out.println("> ");
      String s = scanner.nextLine().trim();
      try {
        double v = Double.parseDouble(s.replace(',', '.'));
        if (v > 0 && !Double.isInfinite(v) && !Double.isNaN(v)) return v;
        System.out.println("Please enter a positive number (> 0): ");
      } catch (NumberFormatException ignored) {
        System.out.println("Please enter a valid number: ");
      }
    }
  }

  // reading int safely from the console
  // this is used for the menu options
  public static int readIntSafe(Scanner scanner, String prompt) {
    while (true) {
      System.out.println(prompt);
      System.out.println("> ");
      String s = scanner.nextLine().trim();
      try {
        return Integer.parseInt(s);
      } catch (NumberFormatException e) {
        System.out.println("Please enter a valid number: ");
      }
    }
  }

  public static int readIntSafe(Scanner scanner) {
    while (true) {
      String s = scanner.nextLine().trim();
      try {
        return Integer.parseInt(s);
      } catch (NumberFormatException e) {
        System.out.println("Please enter a valid number: ");
      }
    }
  }

  public static String readLoginSafe(Scanner scanner) {
    while (true) {
      System.out.println("Please, enter login (3-32, start with letter, letters/digits/._-): ");
      String s = scanner.nextLine().trim().toLowerCase(); // normalazing to lower case
      if (s.matches("^[a-z][a-z0-9._-]{2,31}$")) {
        return s;
      }
      System.out.println(
          "Login must be between 3 and 32 characters long and contain only "
              + "letters, digits, dots, dashes and underscores. Try again. ");
    }
  }

  // NEW: допускает 0 (для лимитов бюджета)
  public static double readNonNegativeDouble(Scanner scanner, String prompt) {
    while (true) {
      System.out.println(prompt);
      System.out.println("> ");
      String s = scanner.nextLine().trim();
      try {
        double v = Double.parseDouble(s.replace(',', '.'));
        if (v >= 0 && !Double.isInfinite(v) && !Double.isNaN(v)) return v;
        System.out.println("Please enter a non-negative number (>= 0).");
      } catch (NumberFormatException ignored) {
        System.out.println("Please enter a valid number: ");
      }
    }
  }

  // NEW: дата ISO или сегодня по умолчанию
  public static String readDateIsoOrToday(Scanner scanner, String prompt) {
    System.out.println(prompt);
    System.out.println("> ");
    String s = scanner.nextLine().trim();
    if (s.isEmpty()) return LocalDate.now().format(ISO);
    try {
      LocalDate.parse(s, ISO);
      return s;
    } catch (DateTimeParseException e) {
      System.out.println("Invalid date format, expected yyyy-MM-dd. Using today.");
      return LocalDate.now().format(ISO);
    }
  }

  public static LocalDate readDateOrNull(Scanner scanner, String prompt) {
    System.out.println(prompt);
    System.out.println("> ");
    String s = scanner.nextLine().trim();
    if (s.isEmpty()) return null;
    try {
      return LocalDate.parse(s, ISO);
    } catch (DateTimeParseException e) {
      System.out.println("Invalid date format. Expected yyyy-MM-dd. Try again.");
      return readDateOrNull(scanner, prompt);
    }
  }

  // NEW: парсинг множества категорий "cat1, cat2, cat3"
  public static Set<String> readCategoriesSet(Scanner scanner, String prompt) {
    System.out.println(prompt);
    System.out.println("> ");
    String line = scanner.nextLine();
    if (line == null || line.trim().isEmpty()) return java.util.Collections.emptySet();
    String[] parts = line.split(",");
    Set<String> set = new LinkedHashSet<>();
    for (String p : parts) {
      String v = p.trim();
      if (!v.isEmpty()) set.add(v);
    }
    return set;
  }
}
