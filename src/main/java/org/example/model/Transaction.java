package org.example.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class Transaction {
  public enum Type {
    INCOME,
    EXPENSE
  } // enum for income and expense types

  public final double amount;
  public final String title;
  public final Type type;

  // добавляем дату как ISO-строку для совместимости с Gson без адаптеров

  private final String dateIso; // yyyy-MM-dd
  private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

  // constructor
  public Transaction(double amount, String title, Type type) {
    this.amount = amount;
    this.title = title;
    this.type = type;
    this.dateIso = LocalDate.now().format(ISO);
  }

  // Overload with LocalDate
  public Transaction(double amount, String title, Type type, LocalDate date) {
    this(amount, title, type, (date != null) ? ISO.format(date) : LocalDate.now().format(ISO));
  }

  // Overload with ISO-string date
  public Transaction(double amount, String title, Type type, String dateIso) {
    if (!Double.isFinite(amount) || amount <= 0.0) {
      throw new IllegalArgumentException("amount must be a positive finite number");
    }
    if (title == null || title.trim().isEmpty()) {
      throw new IllegalArgumentException("title must not be blank");
    }
    if (type == null) {
      throw new IllegalArgumentException("type must not be null");
    }
    this.amount = amount;
    this.title = title.trim();
    this.type = type;
    this.dateIso = normalizeIso(dateIso);
  }

  private static String normalizeIso(String iso) {
    if (iso == null || iso.isBlank()) {
      return LocalDate.now().format(ISO);
    }
    String s = iso.trim();
    try {
      LocalDate.parse(s, ISO);
      return s;
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException("Invalid date format, expected yyyy-MM-dd", e);
    }
  }

  // gettesrs to enable consistency with the other code
  public String getDateIso() {
    return dateIso;
  }

  public LocalDate getDate() {
    return LocalDate.parse(dateIso, ISO);
  }

  public String getTitle() {
    return title;
  }

  public double getAmount() {
    return amount;
  }

  public Type getType() {
    return type;
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
