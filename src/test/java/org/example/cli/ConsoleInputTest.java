package org.example.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ConsoleInputTest {

    @Test
    @DisplayName("readLoginSafe: сначала плохой, потом валидный логин")
    void readLoginSafe() {
        String input = "1bad\nab\nvalid_user-1\n";
        Scanner sc = new Scanner(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        String login = ConsoleInput.readLoginSafe(sc);
        assertEquals("valid_user-1", login);
    }

    @Test
    @DisplayName("readStringSafe: пустые/пробельные значения пропускаются")
    void readStringSafe() {
        String input = "\n   \nHello\n";
        Scanner sc = new Scanner(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        String s = ConsoleInput.readStringSafe(sc, "prompt");
        assertEquals("Hello", s);
    }

    @Test
    @DisplayName("readIntSafe: пропускает мусор и берёт первое целое (включая отрицательное)")
    void readIntSafe() {
        String input = "xx\n-5\n10\n";
        Scanner sc = new Scanner(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        int v = ConsoleInput.readIntSafe(sc);
        assertEquals(-5, v); // метод берёт первое валидное число
    }

    @Test
    @DisplayName("readDoubleSafe: пропускает мусор и читает число")
    void readDoubleSafe() {
        String input = "abc\n0\n12.5\n";
        Scanner sc = new Scanner(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        double v = ConsoleInput.readDoubleSafe(sc, "enter double");
        // если у тебя запрещён 0 — цикл дойдёт до 12.5
        assertEquals(12.5, v, 1e-9);
    }
}
