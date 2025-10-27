package org.example.model;

import java.util.*;

public class Wallet {

    //transactions
    public final List<Transaction> transactions = new ArrayList<>();

    //budgets and categories
    private final Map<String, Double> budgets = new LinkedHashMap<>();
    private final Map<String, Double> spentByCat = new HashMap<>();


    public void addTransaction(double amount, String title, Transaction.Type type) {
        transactions.add(new Transaction(amount, title, type));
        if (type == Transaction.Type.EXPENSE) {
            spentByCat.merge(title, amount, Double::sum);
        }
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public double getBalance() {
        double sum = 0;
        for (Transaction t : transactions) {
            if (t.type == Transaction.Type.INCOME) {
                sum += t.amount;
            } else {
                sum -= t.amount;
            }
        }
        return sum;
    }

    //budgets
    public void setBudget(String category, double limit) {
        budgets.put(category, limit);
    }

    public Map<String, Double> getBudgets() {
        return budgets;
    }

    public double getSpentByCategory(String category) {
        return spentByCat.getOrDefault(category, 0.0);
    }

    public double getRemainingBudget(String category) {
        double limit = budgets.getOrDefault(category, 0.0);
        return limit - getSpentByCategory(category);
    }

    public List<String> getbudgetAlerts() {
        List<String> alerts = new ArrayList<>();
        for (var e : budgets.entrySet()) {
            String cat = e.getKey();
            double remaining = getRemainingBudget(cat);
            if (remaining < 0) {
                alerts.add("Budget exceeded: " + cat + "by" + (-remaining));
            }
        }
        return alerts;
    }

    @Override
    public String toString() {
        return "Wallet{" +
                "transactions=" + transactions +
                ", budgets=" + budgets +
                ", spentByCat=" + spentByCat +
                '}';
    }

    //counting all incomes
    public double sumIncome() {
        double sum = 0;
        for (Transaction t : transactions) {
            if (t.type == Transaction.Type.INCOME) {
                sum += t.amount;
            }
        }
        return sum;
    }

    //counting all expenses
    public double sumExpense() {
        double sum = 0;
        for (Transaction t : transactions) {
            if (t.type == Transaction.Type.EXPENSE) {
                sum += t.amount;
            }
        }
        return sum;
    }
    public Map<String, Double> incomesByCategory() {
        Map<String, Double> m = new LinkedHashMap<>();
        for (Transaction t : transactions) {
            if (t.type == Transaction.Type.INCOME) {
                m.merge(t.title, t.amount, Double::sum);
            }
        }
        return m;
    }

    public Map<String, Double> expensesByCategory() {
        Map<String, Double> m = new LinkedHashMap<>();
        for (Transaction t : transactions) {
            if (t.type == Transaction.Type.EXPENSE) {
                m.merge(t.title, t.amount, Double::sum);
            }
        }
        return m;
    }

}
