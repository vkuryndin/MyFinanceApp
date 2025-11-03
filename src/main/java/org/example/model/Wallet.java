package org.example.model;

import java.time.LocalDate;
import java.util.*;

public class Wallet {

  // transactions
  public final List<Transaction> transactions = new ArrayList<>();

  // budgets and categories
  private final Map<String, Double> budgets = new LinkedHashMap<>();
  private final Map<String, Double> spentByCat = new HashMap<>();

  public void addTransaction(double amount, String title, Transaction.Type type) {
    transactions.add(new Transaction(amount, title, type));
    if (type == Transaction.Type.EXPENSE) {
      spentByCat.merge(title, amount, Double::sum);
    }
  }

  // Удобная перегрузка: если хочешь сам создать Transaction с датой
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

  // -------------------- НОВОЕ для критерия 11: периоды + мультикатегории --------------------

  /**
   * Универсальный фильтр транзакций.
   *
   * @param from включительно, может быть null
   * @param to включительно, может быть null
   * @param categories множество категорий (сравнение по title); null/пусто = все
   * @param type тип транзакции или null = оба
   */
  public List<Transaction> findTransactions(
      LocalDate from, LocalDate to, Set<String> categories, Transaction.Type type) {
    final LocalDate f = from;
    final LocalDate t = to;
    final Set<String> cats = normalizeCatSet(categories);

    List<Transaction> out = new ArrayList<>();
    for (Transaction tx : transactions) {
      if (type != null && tx.getType() != type) continue;

      // Дата берётся из Transaction#getDate() (мы добавили в Transaction dateIso и getDate()).
      LocalDate d = tx.getDate();
      if (f != null && d.isBefore(f)) continue;
      if (t != null && d.isAfter(t)) continue;

      if (!cats.isEmpty()) {
        if (!cats.contains(normalizeCat(tx.getTitle()))) continue;
      }
      out.add(tx);
    }
    // сортировка по дате (по желанию)
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

  // -------------------- (Опционально) для критерия 12: редактирование бюджетов
  // --------------------

  /** Список категорий, для которых задан бюджет. */
  public Set<String> listBudgetCategories() {
    return new LinkedHashSet<>(budgets.keySet());
  }

  /** Обновить лимит бюджета; true если категория существовала. */
  public boolean updateBudgetLimit(String category, double newLimit) {
    if (!budgets.containsKey(category)) return false;
    budgets.put(category, newLimit);
    return true;
  }

  /** Удалить бюджет категории; true если удалено. */
  public boolean removeBudget(String category) {
    return budgets.remove(category) != null;
  }

  /**
   * Переименовать категорию бюджета (и перенос сумм трат по категории внутри spentByCat).
   * Возвращает true, если oldName существовала и перенос выполнен.
   */
  public boolean renameCategory(String oldName, String newName) {
    if (oldName == null || newName == null) return false;
    String oldN = normalizeCat(oldName);
    String newN = normalizeCat(newName);
    if (oldN.isEmpty() || newN.isEmpty() || oldN.equals(newN)) return false;

    boolean changed = false;

    // перенос лимита бюджета
    Double lim = budgets.remove(oldN);
    if (lim != null) {
      // если в целевой уже есть лимит — перезаписываем (можешь поменять логику при желании)
      budgets.put(newN, lim);
      changed = true;
    }

    // перенос накопленных трат в spentByCat
    Double spent = spentByCat.remove(oldN);
    if (spent != null) {
      spentByCat.merge(newN, spent, Double::sum);
      changed = true;
    }

    return changed;
  }
}
