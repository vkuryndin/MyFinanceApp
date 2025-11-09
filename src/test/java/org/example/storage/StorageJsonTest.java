package org.example.storage;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import org.example.repo.UsersRepo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for the StorageJson persistence layer.
 *
 * <p>Tests validate JSON serialization/deserialization of UsersRepo including:
 *
 * <ul>
 *   <li><b>Load operations:</b> Loading from non-existent, empty, and malformed JSON files
 *   <li><b>Save operations:</b> Persisting repository to JSON with proper directory creation
 *   <li><b>Round-trip persistence:</b> Verifying data integrity after save/load cycle
 *   <li><b>State flag management:</b> Tracking {@code isPreviousDataExists} flag for first-run
 *       detection
 *   <li><b>Error handling:</b> Graceful handling of IOException and null arguments
 * </ul>
 *
 * <p>The {@code isPreviousDataExists} flag is used to determine if the first user should become
 * SUPER_ADMIN. When loading from a file successfully, this flag is set to {@code true}; for new
 * repositories, it remains {@code false}.
 *
 * <p>Uses JUnit's {@code @TempDir} to provide isolated temporary directories for each test. The
 * implementation uses GSON for JSON serialization with pretty-printing enabled.
 *
 * @see org.example.storage.StorageJson
 * @see org.example.repo.UsersRepo
 */
public class StorageJsonTest {

  @TempDir Path tmp;

  /**
   * <b>Intent:</b> When the target JSON file does not exist, {@code loadOrNew} must return a fresh
   * {@link UsersRepo} with {@code isPreviousDataExists == false}.
   *
   * <p><b>Preconditions:</b> Path points to a non-existent file under the temp directory.
   *
   * <p><b>Asserts:</b> Repo is non-null; flag is {@code false}; lookup for an arbitrary user
   * returns {@code null}.
   */
  @Test
  @DisplayName("loadOrNew: non existent file → new UsersRepo (previousData=false)")
  void loadOrNew_noFile_returnsNewRepo() {
    Path file = tmp.resolve("finance-data.json");
    UsersRepo repo = StorageJson.loadOrNew(file);
    assertNotNull(repo);
    assertFalse(repo.getIsPreviousDataExists());
    assertNull(repo.find("nobody"));
  }

  /**
   * <b>Intent:</b> Verify successful round-trip persistence and that {@code isPreviousDataExists}
   * becomes {@code true} after loading existing data.
   *
   * <p><b>Actions:</b> Create two users, perform a transfer, save to JSON, then load.
   *
   * <p><b>Asserts:</b> File exists after save; flag is {@code true} after load; both users are
   * present; wallet income/expense totals reflect the transfer (100.0).
   */
  @Test
  @DisplayName("save → loadOrNew: correct round-trip + previousData=true")
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

  /**
   * <b>Intent:</b> If the file exists but is empty, {@code loadOrNew} should not fail and must
   * return a new {@link UsersRepo} with {@code isPreviousDataExists == false}.
   *
   * <p><b>Preconditions:</b> Zero-byte file is created at the target path.
   *
   * <p><b>Asserts:</b> Non-null repo; flag remains {@code false}.
   */
  @Test
  @DisplayName("loadOrNew: empty file → нnew UsersRepo (catch branch)")
  void loadOrNew_emptyFile_returnsNewRepo() throws IOException {
    Path file = tmp.resolve("empty.json");
    Files.write(file, new byte[0]);
    UsersRepo repo = StorageJson.loadOrNew(file);
    assertNotNull(repo);
    assertFalse(repo.getIsPreviousDataExists());
  }

  /**
   * <b>Intent:</b> Malformed JSON must be handled gracefully: return a new repo and keep {@code
   * isPreviousDataExists == false}.
   *
   * <p><b>Preconditions:</b> Write invalid JSON to the file.
   *
   * <p><b>Asserts:</b> Non-null repo; flag is {@code false}.
   */
  @Test
  @DisplayName("loadOrNew: malformed JSON → new UsersRepo (catch branch)")
  void loadOrNew_malformedJson_returnsNewRepo() throws IOException {
    Path file = tmp.resolve("broken.json");
    Files.writeString(file, "{ not a valid json", StandardCharsets.UTF_8);
    UsersRepo repo = StorageJson.loadOrNew(file);
    assertNotNull(repo);
    assertFalse(repo.getIsPreviousDataExists());
  }

  /**
   * <b>Intent:</b> Document and enforce null-argument contracts in {@code save} via {@link
   * Objects#requireNonNull(Object, String)}.
   *
   * <p><b>Asserts:</b> Passing a {@code null} path or {@code null} repository causes {@link
   * NullPointerException}.
   */
  @Test
  @DisplayName("save: NPE on null argiments (Objects.requireNonNull)")
  void save_nullArgs_throwNpe() {
    Path file = tmp.resolve("x.json");
    UsersRepo repo = new UsersRepo();
    assertThrows(NullPointerException.class, () -> StorageJson.save(null, repo));
    assertThrows(NullPointerException.class, () -> StorageJson.save(file, null));
  }

  /**
   * <b>Intent:</b> When the parent component is a regular file (so creating directories fails),
   * {@code save} must swallow the {@code IOException} (log internally) and not create the target
   * file.
   *
   * <p><b>Preconditions:</b> Create a regular file as the would-be parent directory.
   *
   * <p><b>Asserts:</b> No exception thrown by {@code save}; target path still does not exist.
   */
  @Test
  @DisplayName(
      "save: IOException inside (parent = file) → the method doesn not throw метод,  file is not created")
  void save_parentIsFile_ioException_isSwallowed_andNoFile() throws IOException {
    // Parent— оis an ordinary file, and we will try to save it into iys "subpath" →
    // createDirectories throws
    Path parentAsFile = tmp.resolve("dir-as-file");
    Files.writeString(parentAsFile, "i am a file");
    Path impossible = parentAsFile.resolve("finance-data.json");

    UsersRepo repo = new UsersRepo();
    repo.register("userok", "U", "U", "p");

    // save does not throws, it is logging the error
    StorageJson.save(impossible, repo);

    // We were unable to create a file
    assertFalse(Files.exists(impossible));
  }

  /**
   * <b>Intent:</b> Calling {@code loadOrNew} with a {@code null} path must fail fast with {@link
   * NullPointerException} (e.g., due to {@code Files.exists(null)}).
   *
   * <p><b>Asserts:</b> {@code NullPointerException} is thrown.
   */
  @Test
  @DisplayName("loadOrNew: null path → NPE from Files.exists(...) (fixing the contract)")
  void loadOrNew_null_throwsNpe() {
    assertThrows(NullPointerException.class, () -> StorageJson.loadOrNew(null));
  }
}
