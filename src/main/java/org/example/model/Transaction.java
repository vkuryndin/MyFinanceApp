package org.example.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.UUID;

public final class Transaction {
  public enum Type {
    INCOME,
    EXPENSE
  }

  private final String id; // 🔹 Уникальный GUID каждой транзакции
  private final double amount;
  private final String title;
  private final Type type;
  private final String dateIso; // yyyy-MM-dd

  private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

  // === Constructors ===

  /** Ordinary constructor — generates new GUID */
  public Transaction(double amount, String title, Type type) {
    this(UUID.randomUUID().toString(), amount, title, type, LocalDate.now().format(ISO));
  }

  /** With date (generates new GUID) */
  public Transaction(double amount, String title, Type type, LocalDate date) {
    this(
        UUID.randomUUID().toString(),
        amount,
        title,
        type,
        (date != null) ? ISO.format(date) : LocalDate.now().format(ISO));
  }

  /** with ISO-date (generates new GUID) */
  public Transaction(double amount, String title, Type type, String dateIso) {
    this(UUID.randomUUID().toString(), amount, title, type, dateIso);
  }

  /** New constructor: used when importing fron JSON, if GUID is present */
  public Transaction(String id, double amount, String title, Type type, String dateIso) {
    if (!Double.isFinite(amount) || amount <= 0.0) {
      throw new IllegalArgumentException("amount must be a positive finite number");
    }
    if (title == null || title.trim().isEmpty()) {
      throw new IllegalArgumentException("title must not be blank");
    }
    if (type == null) {
      throw new IllegalArgumentException("type must not be null");
    }

    this.id = (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
    this.amount = amount;
    this.title = title.trim();
    this.type = type;
    this.dateIso = normalizeIso(dateIso);
  }

  // === Helpers ===

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

  // === Getters ===

  public String getId() {
    return id;
  }

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

  // === Equals anf HashCode by GUID ===

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Transaction)) return false;
    Transaction that = (Transaction) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  // === toString ===

  @Override
  public String toString() {
    return "Transaction{"
        + "id='"
        + id
        + '\''
        + ", amount="
        + amount
        + ", title='"
        + title
        + '\''
        + ", type="
        + type
        + ", dateIso='"
        + dateIso
        + '\''
        + '}';
  }
}
