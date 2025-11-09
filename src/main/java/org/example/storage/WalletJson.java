package org.example.storage;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.MalformedJsonException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.example.model.Transaction;
import org.example.model.User;

public final class WalletJson {

  private WalletJson() {
    throw new AssertionError("No instances allowed");
  }

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  public static final Path DEFAULT_FILE = Paths.get("data", "wallet.json");

  /* ========================
  Public API
  ======================== */

  /** Export wallet to default file (data/wallet.json). */
  public static void save(User user) {
    save(DEFAULT_FILE, user);
  }

  /** Import wallet from default file (data/wallet.json). */
  public static void loadInto(User user) {
    Objects.requireNonNull(user, "user");
    if (!Files.exists(DEFAULT_FILE)) {
      System.out.println("No wallet backup found: " + DEFAULT_FILE.toAbsolutePath());
      return;
    }
    loadInto(DEFAULT_FILE, user);
  }

  /* ========================
  Save to custom path
  ======================== */

  public static void save(Path file, User user) {
    Objects.requireNonNull(file, "file");
    Objects.requireNonNull(user, "user");
    try {
      Path parent = file.getParent();
      if (parent != null && Files.exists(parent) && !Files.isDirectory(parent)) {
        System.err.println("Error saving wallet to " + file + ": parent is not a directory.");
        return;
      }
      if (parent != null && !Files.exists(parent)) {
        Files.createDirectories(parent);
      }

      JsonObject root = new JsonObject();

      // Transactions
      JsonArray txArr = new JsonArray();
      for (var t : user.wallet.getTransactions()) {
        JsonObject jt = new JsonObject();
        jt.addProperty("id", t.getId());
        jt.addProperty("date", t.getDateIso());
        jt.addProperty("type", t.getType().name());
        jt.addProperty("title", t.getTitle());
        jt.addProperty("amount", t.getAmount());
        txArr.add(jt);
      }
      root.add("transactions", txArr);

      // Budgets
      JsonObject jb = new JsonObject();
      for (Map.Entry<String, Double> e : user.wallet.getBudgets().entrySet()) {
        jb.addProperty(e.getKey(), e.getValue());
      }
      root.add("budgets", jb);

      try (BufferedWriter w =
          Files.newBufferedWriter(
              file,
              StandardCharsets.UTF_8,
              StandardOpenOption.CREATE,
              StandardOpenOption.TRUNCATE_EXISTING)) {
        GSON.toJson(root, w);
      }
      System.out.println("Saved wallet to " + file.toAbsolutePath());
    } catch (IOException e) {
      System.err.println("Error saving wallet to file " + file + ": " + e.getMessage());
      e.printStackTrace();
    }
  }

  /* ========================
  Load & Import
  ======================== */

  public static void loadInto(Path file, User user) {
    Objects.requireNonNull(file, "file");
    Objects.requireNonNull(user, "user");

    if (!Files.exists(file)) {
      System.out.println("No wallet backup found: " + file.toAbsolutePath());
      return;
    }

    int imported = 0;
    int skippedDup = 0;
    int budgetsSet = 0;

    // Prepare existing transaction IDs
    Set<String> existingIds = new HashSet<>();
    for (var tx : user.wallet.getTransactions()) {
      existingIds.add(tx.getId());
    }

    // Fallback signatures for old transactions (no ID)
    Set<String> existingSignatures = new HashSet<>();
    for (var tx : user.wallet.getTransactions()) {
      existingSignatures.add(
          signature(tx.getDateIso(), tx.getType().name(), tx.getTitle(), tx.getAmount()));
    }

    try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
      // Strict JSON-parser — immediately catch all problems
      JsonReader jr = new JsonReader(r);
      jr.setLenient(false);

      JsonElement root = JsonParser.parseReader(jr);

      if (root.isJsonObject()) {
        JsonObject obj = root.getAsJsonObject();

        // --- Import transactions ---
        if (obj.has("transactions") && obj.get("transactions").isJsonArray()) {
          ImportStat s =
              importTxArray(
                  obj.getAsJsonArray("transactions"), user, existingIds, existingSignatures);
          imported += s.imported;
          skippedDup += s.skippedDup;
        }

        // --- Import budgets ---
        if (obj.has("budgets") && obj.get("budgets").isJsonObject()) {
          JsonObject b = obj.getAsJsonObject("budgets");
          for (Map.Entry<String, JsonElement> e : b.entrySet()) {
            JsonElement v = e.getValue();
            if (v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isNumber()) {
              double limit = v.getAsDouble();
              if (limit >= 0 && Double.isFinite(limit)) {
                user.wallet.setBudget(e.getKey(), limit);
                budgetsSet++;
              }
            }
          }
        }
      }

      System.out.println(
          "Loaded from "
              + file.toAbsolutePath()
              + " | transactions: +"
              + imported
              + ", duplicates skipped: "
              + skippedDup
              + ", budgets updated: "
              + budgetsSet);

    } catch (JsonSyntaxException | MalformedJsonException e) {
      // Message where JSON is broken
      System.err.println(
          "Wallet JSON is malformed ("
              + prettyJsonErrorLocation(e)
              + ") in file: "
              + file.toAbsolutePath());
    } catch (Exception e) {
      System.err.println("Error loading wallet from file " + file + ": " + e.getMessage());
      e.printStackTrace();
    }
  }

  /* ========================
  Internal helpers
  ======================== */

  private static final class ImportStat {
    final int imported;
    final int skippedDup;

    ImportStat(int imported, int skippedDup) {
      this.imported = imported;
      this.skippedDup = skippedDup;
    }
  }

  private static ImportStat importTxArray(
      JsonArray arr, User u, Set<String> existingIds, Set<String> existingSignatures) {
    int ok = 0;
    int dup = 0;

    for (JsonElement el : arr) {
      if (!el.isJsonObject()) continue;
      JsonObject t = el.getAsJsonObject();

      String id = getAsString(t, "id");
      String dateIso = getAsString(t, "date");
      if (dateIso == null || dateIso.isBlank()) dateIso = getAsString(t, "dateIso");
      String typeUpper = safeUpper(getAsString(t, "type"));
      String title = getAsString(t, "title");
      Double amount = getAsDouble(t, "amount");

      if (typeUpper == null || title == null || amount == null) continue;
      if (!(amount > 0) || !Double.isFinite(amount)) continue;

      Transaction.Type tt;
      if ("INCOME".equals(typeUpper)) tt = Transaction.Type.INCOME;
      else if ("EXPENSE".equals(typeUpper)) tt = Transaction.Type.EXPENSE;
      else continue;

      // --- Duplicate check ---
      if (id != null && existingIds.contains(id)) {
        dup++;
        continue;
      }

      String sig = signature((dateIso == null ? "" : dateIso), typeUpper, title, amount);
      if (id == null && existingSignatures.contains(sig)) {
        dup++;
        continue;
      }

      try {
        Transaction tx =
            (id != null && !id.isBlank())
                ? new Transaction(id, amount, title, tt, dateIso)
                : new Transaction(amount, title, tt, dateIso);

        u.wallet.addTransaction(tx);
        existingIds.add(tx.getId());
        existingSignatures.add(sig);
        ok++;

      } catch (Exception ignore) {
      }
    }
    return new ImportStat(ok, dup);
  }

  private static String signature(String dateIso, String typeUpper, String title, double amount) {
    String d = (dateIso == null) ? "" : dateIso.trim();
    String t = (typeUpper == null) ? "" : typeUpper.trim().toUpperCase(Locale.ROOT);
    String ti = (title == null) ? "" : title.trim();
    String a = String.format(Locale.ROOT, "%.2f", amount);
    return d + "|" + t + "|" + ti + "|" + a;
  }

  private static String getAsString(JsonObject o, String key) {
    if (!o.has(key)) return null;
    JsonElement v = o.get(key);
    if (v == null || v.isJsonNull()) return null;
    if (!v.isJsonPrimitive() || !v.getAsJsonPrimitive().isString()) return null;
    String s = v.getAsString();
    return (s == null) ? null : s.trim();
  }

  private static Double getAsDouble(JsonObject o, String key) {
    if (!o.has(key)) return null;
    JsonElement v = o.get(key);
    if (v == null || v.isJsonNull()) return null;
    if (v.isJsonPrimitive() && v.getAsJsonPrimitive().isNumber()) return v.getAsDouble();
    if (v.isJsonPrimitive() && v.getAsJsonPrimitive().isString()) {
      try {
        return Double.valueOf(v.getAsString().trim());
      } catch (Exception ignore) {
      }
    }
    return null;
  }

  private static String safeUpper(String s) {
    return (s == null) ? null : s.trim().toUpperCase(Locale.ROOT);
  }

  private static String prettyJsonErrorLocation(Throwable e) {
    // Example of message form Gson: "Expected ':' at line 12 column 7 path $.transactions[3].title"
    String msg = (e == null || e.getMessage() == null) ? "" : e.getMessage();
    if (msg.isEmpty()) return "unknown location";

    String line = null, col = null, path = null;

    Matcher mLineCol =
        Pattern.compile("line\\s+(\\d+)\\s+column\\s+(\\d+)", Pattern.CASE_INSENSITIVE)
            .matcher(msg);
    if (mLineCol.find()) {
      line = mLineCol.group(1);
      col = mLineCol.group(2);
    }
    Matcher mPath = Pattern.compile("path\\s+([^\\s]+)", Pattern.CASE_INSENSITIVE).matcher(msg);
    if (mPath.find()) {
      path = mPath.group(1);
    }

    StringBuilder sb = new StringBuilder();
    if (line != null && col != null)
      sb.append("line ").append(line).append(", column ").append(col);
    if (path != null) {
      if (sb.length() > 0) sb.append(", ");
      sb.append("path ").append(path);
    }
    if (sb.length() == 0) sb.append(msg);
    return sb.toString();
  }
}
