package org.example.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Objects;
import org.example.repo.UsersRepo;

public final class StorageJson {

  // constructor is private to prevent instantiation
  private StorageJson() {
    throw new AssertionError("No instances allowed");
  }

  // using GSON to serialize and deserialize objects to and from JSON format (to save all our data
  // to the file)
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  // saving and loading users' repository to and from file
  public static void save(Path file, UsersRepo usersRepo) {
    Objects.requireNonNull(file, "file");
    Objects.requireNonNull(usersRepo, "usersRepo");
    try {
      Path parent = file.getParent();

      // 1) Если родитель существует, но это НЕ директория → логируем и выходим (файл не создаём)
      if (parent != null && Files.exists(parent) && !Files.isDirectory(parent)) {
        System.err.println("Error saving users repository to file " + file + ": " + parent);
        return; // <— КЛЮЧЕВОЕ: ничего дальше не делаем
      }

      // 2) Создаём директории только если их нет
      if (parent != null && !Files.exists(parent)) {
        Files.createDirectories(parent);
      }

      // 3) Пишем JSON (создаст файл сам при необходимости)
      try (BufferedWriter w =
          Files.newBufferedWriter(
              file,
              StandardCharsets.UTF_8,
              StandardOpenOption.CREATE,
              StandardOpenOption.TRUNCATE_EXISTING)) {
        GSON.toJson(usersRepo, w);
      }

      // 4) Лог об успешном сохранении (опционально)
      System.out.println("Saved users repository to " + file.toAbsolutePath());

    } catch (IOException e) {
      // Любые IOException — логируем и НЕ пробрасываем (по контракту теста)
      System.err.println("Error saving users repository to file " + file + ": " + e.getMessage());
      e.printStackTrace();
    }
  }

  // loading users' repository from file or creating new one if file does not exist
  public static UsersRepo loadOrNew(Path file) {
    if (!Files.exists(file)) return new UsersRepo();
    try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
      UsersRepo usersRepo = GSON.fromJson(r, UsersRepo.class);
      // return (usersRepo !=null)? usersRepo: new UsersRepo(); //previous implementations
      if (usersRepo != null) {
        usersRepo.setIsPreviousDataExists(true);
        return usersRepo;
      } else {
        return new UsersRepo();
      }

    } catch (Exception e) {
      System.err.println(
          "Error loading users repository from file "
              + file
              + ": "
              + e.getMessage()
              + "starting afresh...");
      return new UsersRepo();
    }
  }
}
