package org.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class StorageJson {
    private StorageJson() {

    }
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    public static void save(Path file, UsersRepo usersRepo) {
        try {
            if (file.getParent() != null) Files.createDirectories(file.getParent());
            try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(usersRepo, w);
            }
        } catch (IOException e) {
            System.err.println("Error saving users repository to file " + file.toString() + ": " + e.getMessage());
        }
    }
    public static UsersRepo loadOrNew (Path file) {
        if (!Files.exists(file)) return new UsersRepo();
        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)){
            UsersRepo usersRepo = GSON.fromJson(r, UsersRepo.class);
            return (usersRepo !=null)? usersRepo: new UsersRepo();

        } catch (Exception e) {
            System.err.println("Error loading users repository from file " + file.toString() + ": " + e.getMessage() + "starting afresh...");
            return new UsersRepo();

        }

    }

}
