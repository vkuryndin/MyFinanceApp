package org.example.util;

import java.util.Scanner;

public class Input {
    private Input () {}

    // reading string safely from the console
    //this is used for entering text to the app
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

    //reading double safely from the console
    //this is used for entering numerics to the app
    public static double readDoubleSafe(Scanner scanner, String prompt) {
        while(true) {
            System.out.println(prompt);
            System.out.println("> ");
            String s = scanner.nextLine().trim();
            try {
                double v = Double.parseDouble(s.replace(',', '.'));
                if (v > 0 && !Double.isInfinite(v) && !Double.isNaN(v)) return v;

            } catch (NumberFormatException ignored) {
                System.out.println("Please enter a valid number: ");
            }
        }
    }

    //reading int safely from the console
    //this is used for the menu options
    public static int readIntSafe(Scanner scanner, String prompt) {
        while(true) {
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
        while(true) {
            String s = scanner.nextLine().trim();
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number: ");
            }
        }
    }

}
