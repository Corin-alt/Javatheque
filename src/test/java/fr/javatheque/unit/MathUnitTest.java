package fr.javatheque.unit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MathUnitTest {
    @Test
    public void testAddition() {
        int result = 2 + 3;
        assertEquals(5, result);
    }
}
