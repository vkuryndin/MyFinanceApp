package org.example;

public class Transaction {
    public enum Type {INCOME, EXPENSE}

    public final double amount;
    public final String title;
    public final Type type;

    public Transaction(double amount, String title, Type type) {
        this.amount = amount;
        this.title = title;
        this.type = type;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "amount=" + amount +
                ", title='" + title + '\'' +
                ", type=" + type +
                '}';
    }
}
