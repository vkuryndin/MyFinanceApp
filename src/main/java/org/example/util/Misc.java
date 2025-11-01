package org.example.util;

import java.util.Scanner;
import org.example.model.User;

public class Misc {
  private Misc() {}

  public static boolean checkLogonStatus(User currentUser) {
    if (!(currentUser == null)) {
      System.out.println(
          "Logged in as: " + currentUser.login + " || " + currentUser.getAdminStatus());
      return true;
    } else {
      System.out.println("Status: You are not logged in");
      return false;
    }
  }

  public static void checkLogonStatusSimnple(User currentUser) {
    if (currentUser != null) {
      System.out.println(
          "Logged in as: " + currentUser.login + " || " + currentUser.getAdminStatus());
    } else {
      System.out.println("Status: You are not logged in");
    }
  }

  public static void displayStatitics(User u) {
    double totalIncome = u.wallet.sumIncome();
    double totalExpense = u.wallet.sumExpense();
    double balance = u.wallet.getBalance();

    System.out.println("==========================");
    System.out.println("Wallet statistics");
    System.out.println("Total income: " + totalIncome);
    System.out.println("Total expense: " + totalExpense);
    System.out.println("Balance: " + balance);

    var incMap = u.wallet.incomesByCategory();
    if (incMap.isEmpty()) {
      System.out.println("No incomes yet");
    } else {
      System.out.println("Incomes by category:");
      for (var e : incMap.entrySet()) {
        System.out.println("- " + e.getKey() + ": " + e.getValue());
      }
    }
    var expMap = u.wallet.expensesByCategory();
    if (expMap.isEmpty()) {
      System.out.println("No expenses yet");
    } else {
      System.out.println("Expenses by category:");
      for (var e : expMap.entrySet()) {
        System.out.println("- " + e.getKey() + ": " + e.getValue());
      }
    }
    System.out.println("==========================");

    //  TO FIX we need to add some budgets statistics here also
    // double spent = u.wallet.getSpentByCategory(cat);
    // double rem = u.wallet.getRemainingBudget(cat);
    // System.out.println("Spent in: " + cat + ": " + spent + ", remaining: " + rem);
  }

  public static boolean confirmAction(Scanner scanner) {
    String sure = Input.readStringSafe(scanner, "Type YES to confirm account deletion: ");
    if (!"YES".equalsIgnoreCase(sure)) {
      System.out.println("Wrong input, try again.");
      return false;
    }
    return true;
  }
}
