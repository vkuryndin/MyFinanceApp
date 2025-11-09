package org.example.model;

import java.time.LocalDate;
import java.util.*;

public class Wallet {

  // transactions
  public final List<Transaction> transactions = new ArrayList<>();

  // budgets and categories
  private final Map<String, Double> budgets = new LinkedHashMap<>();
  private final Map<String, Double> spentByCat = new HashMap<>();

  //the main method for adding transaction
  public void addTransaction(double amount, String title, Transaction.Type type) {
    transactions.add(new Transaction(amount, title, type));
    if (type == Transaction.Type.EXPENSE) {
      spentByCat.merge(title, amount, Double::sum);
    }
  }

  // overload add Transaction method - create Transaction with date
  public void addTransaction(Transaction tx) {
    if (tx == null) return;
    transactions.add(tx);
    if (tx.getType() == Transaction.Type.EXPENSE) {
      spentByCat.merge(tx.getTitle(), tx.getAmount(), Double::sum);
    }
  }

  public List<Transaction> getTransactions() {
    return List.copyOf(transactions); // fixing spotbugs error EL_EXSPOSE_REP
  }

  public double getBalance() {
    double sum = 0;
    for (Transaction t : transactions) {
      if (t.getType() == Transaction.Type.INCOME) {
        sum += t.getAmount();
      } else {
        sum -= t.getAmount();
      }
    }
    return sum;
  }

  // budgets
  public void setBudget(String category, double limit) {
    budgets.put(category, limit);
  }

  public Map<String, Double> getBudgets() {
    return Map.copyOf(budgets); // fixing spotbugs error EL_EXSPOSE_REP
  }

  public double getSpentByCategory(String category) {
    return spentByCat.getOrDefault(category, 0.0);
  }

  public double getRemainingBudget(String category) {
    double limit = budgets.getOrDefault(category, 0.0);
    return limit - getSpentByCategory(category);
  }

  //working with alerts
  public List<String> getBudgetAlerts() {
    List<String> alerts = new ArrayList<>();
    for (var e : budgets.entrySet()) {
      String cat = e.getKey();
      double limit = e.getValue();
      double spent = getSpentByCategory(cat);
      if (limit <= 0) continue;

      double used = spent / limit; // доля использования
      if (used >= 1.0) {
        alerts.add("Budget exceeded: " + cat + " by " + (spent - limit));
      } else if (used >= 0.9) {
        alerts.add("Budget warning (≥90%): " + cat + " used " + Math.round(used * 100) + "%");
      } else if (used >= 0.8) {
        alerts.add("Budget warning (≥80%): " + cat + " used " + Math.round(used * 100) + "%");
      }
    }
    return alerts;
  }

  //to string override
  @Override
  public String toString() {
    return "Wallet{"
        + "transactions="
        + transactions
        + ", budgets="
        + budgets
        + ", spentByCat="
        + spentByCat
        + '}';
  }

  // counting all incomes
  public double sumIncome() {
    double sum = 0;
    for (Transaction t : transactions) {
      if (t.getType() == Transaction.Type.INCOME) {
        sum += t.getAmount();
      }
    }
    return sum;
  }

  // counting all expenses
  public double sumExpense() {
    double sum = 0;
    for (Transaction t : transactions) {
      if (t.getType() == Transaction.Type.EXPENSE) {
        sum += t.getAmount();
      }
    }
    return sum;
  }

  public Map<String, Double> incomesByCategory() {
    Map<String, Double> m = new LinkedHashMap<>();
    for (Transaction t : transactions) {
      if (t.getType() == Transaction.Type.INCOME) {
        m.merge(t.getTitle(), t.getAmount(), Double::sum);
      }
    }
    return m;
  }

  public Map<String, Double> expensesByCategory() {
    Map<String, Double> m = new LinkedHashMap<>();
    for (Transaction t : transactions) {
      if (t.getType() == Transaction.Type.EXPENSE) {
        m.merge(t.getTitle(), t.getAmount(), Double::sum);
      }
    }
    return m;
  }

  // -------------------- New for advanced statistics feature  --------------------

  /**
   * Universal transaction filter.
   *
   * @param from included, can be null
   * @param to включительно, can be null
   * @param categories a quantity of categories ( using title for camparison); null/пусто = all
   * @param type transaction type or null = all
   */
  public List<Transaction> findTransactions(
      LocalDate from, LocalDate to, Set<String> categories, Transaction.Type type) {
    final LocalDate f = from;
    final LocalDate t = to;
    final Set<String> cats = normalizeCatSet(categories);

    List<Transaction> out = new ArrayList<>();
    for (Transaction tx : transactions) {
      if (type != null && tx.getType() != type) continue;

      // Date is taken from Transaction#getDate() (we have added dateIso anf getDate() methods to the Transaction class).
      LocalDate d = tx.getDate();
      if (f != null && d.isBefore(f)) continue;
      if (t != null && d.isAfter(t)) continue;

      if (!cats.isEmpty()) {
        if (!cats.contains(normalizeCat(tx.getTitle()))) continue;
      }
      out.add(tx);
    }
    // sorting by date
    out.sort(Comparator.comparing(Transaction::getDate));
    return out;
  }

  public double sumExpense(LocalDate from, LocalDate to, Set<String> categories) {
    double sum = 0;
    for (Transaction tx : findTransactions(from, to, categories, Transaction.Type.EXPENSE)) {
      sum += tx.getAmount();
    }
    return sum;
  }

  public double sumIncome(LocalDate from, LocalDate to, Set<String> categories) {
    double sum = 0;
    for (Transaction tx : findTransactions(from, to, categories, Transaction.Type.INCOME)) {
      sum += tx.getAmount();
    }
    return sum;
  }

  public Map<String, Double> expensesByCategory(
      LocalDate from, LocalDate to, Set<String> categories) {
    Map<String, Double> m = new LinkedHashMap<>();
    for (Transaction tx : findTransactions(from, to, categories, Transaction.Type.EXPENSE)) {
      m.merge(normalizeCat(tx.getTitle()), tx.getAmount(), Double::sum);
    }
    return m;
  }

  public Map<String, Double> incomesByCategory(
      LocalDate from, LocalDate to, Set<String> categories) {
    Map<String, Double> m = new LinkedHashMap<>();
    for (Transaction tx : findTransactions(from, to, categories, Transaction.Type.INCOME)) {
      m.merge(normalizeCat(tx.getTitle()), tx.getAmount(), Double::sum);
    }
    return m;
  }

  private static String normalizeCat(String s) {
    return s == null ? "" : s.trim();
  }

  private static Set<String> normalizeCatSet(Set<String> in) {
    if (in == null || in.isEmpty()) return Collections.emptySet();
    Set<String> out = new HashSet<>();
    for (String s : in) {
      if (s == null) continue;
      String v = s.trim();
      if (!v.isEmpty()) out.add(v);
    }
    return out;
  }

  // -------------------- Budgets editing
  // --------------------

  /** Categories list for which budget has been created. */
  public Set<String> listBudgetCategories() {
    return new LinkedHashSet<>(budgets.keySet());
  }

  /** ОUpdate budget limit; true if category existed. */
  public boolean updateBudgetLimit(String category, double newLimit) {
    if (!budgets.containsKey(category)) return false;
    budgets.put(category, newLimit);
    return true;
  }

  /** Delete budget category, true if deleted. */
  public boolean removeBudget(String category) {
    return budgets.remove(category) != null;
  }

  /**
   * Rename budget category (and move expenses sum for category inside spentByCat).
   * Returns true, if oldName existed and transition is done .
   */
  public boolean renameCategory(String oldName, String newName) {
    if (oldName == null || newName == null) return false;
    String oldN = normalizeCat(oldName);
    String newN = normalizeCat(newName);
    if (oldN.isEmpty() || newN.isEmpty() || oldN.equals(newN)) return false;

    boolean changed = false;

    // move budgets limit
    Double lim = budgets.remove(oldN);
    if (lim != null) {
      // if the target category already has a limit - overwrite
      budgets.put(newN, lim);
      changed = true;
    }

    // move summed expenses to spentByCat
    Double spent = spentByCat.remove(oldN);
    if (spent != null) {
      spentByCat.merge(newN, spent, Double::sum);
      changed = true;
    }

    return changed;
  }
}
