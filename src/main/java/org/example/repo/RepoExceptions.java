package org.example.repo;

// special exceptions for the repo
public class RepoExceptions {
  public static class NotFound extends RuntimeException {
    public NotFound(String message) {
      super(message);
    }
  }

  public static class Conflict extends RuntimeException {
    public Conflict(String message) {
      super(message);
    }
  }

  public static class Forbidden extends RuntimeException {
    public Forbidden(String message) {
      super(message);
    }
  }

  public static class Invalid extends RuntimeException {
    public Invalid(String message) {
      super(message);
    }
  }
}
