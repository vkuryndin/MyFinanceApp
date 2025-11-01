package org.example.model;

public class Transaction {
  public enum Type {
    INCOME,
    EXPENSE
  } // enum for income and expense types

  public final double amount;
  public final String title;
  public final Type type;

  // constructor
  public Transaction(double amount, String title, Type type) {
    this.amount = amount;
    this.title = title;
    this.type = type;
  }

  // override toString method for printing transactions
  @Override
  public String toString() {
    return "Transaction{"
        + "amount="
        + amount
        + ", title='"
        + title
        + '\''
        + ", type="
        + type
        + '}';
  }
}
