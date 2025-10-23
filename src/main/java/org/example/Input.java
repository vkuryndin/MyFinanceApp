package org.example;

import java.util.Scanner;

public class Input {
    private Input () {}

    // reading string safely from console
    public static String readStringSafe(Scanner scanner, String prompt) {
        while (true) {
            System.out.println(prompt);
            String s = scanner.nextLine();
            if (s != null) {
                s = s.trim();
                if (!s.isEmpty()) return s;
            }
            System.out.println("Input cannot be empty. Try again. ");
        }
    }
    public static double readDoubleSafe(Scanner scanner, String prompt) {
        while(true) {
            System.out.println(prompt);
            String s = scanner.nextLine().trim();
            try {
                double v = Double.parseDouble(s.replace(',', '.'));
                if (v > 0 && !Double.isInfinite(v) && !Double.isNaN(v)) return v;

            } catch (NumberFormatException ignored) {
                System.out.println("Please enter a valid number: ");
            }
        }
    }
    public static int readIntSafe(Scanner scanner, String prompt) {
        while(true) {
            System.out.println(prompt);
            String s = scanner.nextLine().trim();
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number: ");
            }
        }
    }

}
