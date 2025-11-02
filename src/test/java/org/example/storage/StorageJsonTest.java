package org.example.storage;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import org.example.repo.UsersRepo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class StorageJsonTest {

  @TempDir Path tmp;

  @Test
  @DisplayName("loadOrNew: отсутствующий файл → новый UsersRepo (previousData=false)")
  void loadOrNew_noFile_returnsNewRepo() {
    Path file = tmp.resolve("finance-data.json");
    UsersRepo repo = StorageJson.loadOrNew(file);
    assertNotNull(repo);
    assertFalse(repo.getIsPreviousDataExists());
    assertNull(repo.find("nobody"));
  }

  @Test
  @DisplayName("save → loadOrNew: корректный round-trip + проставляется previousData=true")
  void roundTrip_save_then_loadOrNew_restoresData_and_setsFlag() {
    Path file = tmp.resolve("finance-data.json");

    UsersRepo repo = new UsersRepo();
    repo.register("alice", "A", "A", "p1");
    repo.register("bob", "B", "B", "p2");
    repo.transfer("alice", "bob", 100.0, "gift");

    StorageJson.save(file, repo);
    assertTrue(Files.exists(file));

    UsersRepo loaded = StorageJson.loadOrNew(file);
    assertTrue(loaded.getIsPreviousDataExists(), "Flag must be true after successful load");
    assertNotNull(loaded.find("alice"));
    assertNotNull(loaded.find("bob"));
    assertEquals(100.0, loaded.find("alice").wallet.sumExpense(), 1e-9);
    assertEquals(100.0, loaded.find("bob").wallet.sumIncome(), 1e-9);
  }

  @Test
  @DisplayName("loadOrNew: пустой файл → новый UsersRepo (ветка catch)")
  void loadOrNew_emptyFile_returnsNewRepo() throws IOException {
    Path file = tmp.resolve("empty.json");
    Files.write(file, new byte[0]);
    UsersRepo repo = StorageJson.loadOrNew(file);
    assertNotNull(repo);
    assertFalse(repo.getIsPreviousDataExists());
  }

  @Test
  @DisplayName("loadOrNew: битый JSON → новый UsersRepo (ветка catch)")
  void loadOrNew_malformedJson_returnsNewRepo() throws IOException {
    Path file = tmp.resolve("broken.json");
    Files.writeString(file, "{ not a valid json", StandardCharsets.UTF_8);
    UsersRepo repo = StorageJson.loadOrNew(file);
    assertNotNull(repo);
    assertFalse(repo.getIsPreviousDataExists());
  }

  @Test
  @DisplayName("save: NPE на null аргументах (Objects.requireNonNull)")
  void save_nullArgs_throwNpe() {
    Path file = tmp.resolve("x.json");
    UsersRepo repo = new UsersRepo();
    assertThrows(NullPointerException.class, () -> StorageJson.save(null, repo));
    assertThrows(NullPointerException.class, () -> StorageJson.save(file, null));
  }

  @Test
  @DisplayName("save: IOException внутри (родитель = файл) → метод не бросает, файл не создаётся")
  void save_parentIsFile_ioException_isSwallowed_andNoFile() throws IOException {
    // Родитель — обычный файл, а мы попытаемся сохранить в его «подпуть» → createDirectories бросит
    Path parentAsFile = tmp.resolve("dir-as-file");
    Files.writeString(parentAsFile, "i am a file");
    Path impossible = parentAsFile.resolve("finance-data.json");

    UsersRepo repo = new UsersRepo();
    repo.register("userok", "U", "U", "p");

    // save НЕ бросает, он логирует ошибку
    StorageJson.save(impossible, repo);

    // Файл создать не смогли
    assertFalse(Files.exists(impossible));
  }

  @Test
  @DisplayName("loadOrNew: null путь → NPE из Files.exists(...) (фиксируем контракт)")
  void loadOrNew_null_throwsNpe() {
    assertThrows(NullPointerException.class, () -> StorageJson.loadOrNew(null));
  }
}
