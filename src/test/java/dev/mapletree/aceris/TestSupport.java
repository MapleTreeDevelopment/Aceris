package dev.mapletree.aceris;

import java.util.Arrays;
import java.util.Objects;

final class TestSupport {
    private TestSupport() {
    }

    static void run(String name, Runnable test) {
        try {
            test.run();
        } catch (Throwable failure) {
            throw new AssertionError("Test failed: " + name, failure);
        }
    }

    static void pass(String className) {
        System.out.println(className + " passed.");
    }

    static void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected <" + expected + "> but got <" + actual + ">");
        }
    }

    static void assertTrue(boolean condition) {
        if (!condition) {
            throw new AssertionError("Expected condition to be true");
        }
    }

    static void assertIntArrayEquals(int[] expected, int[] actual) {
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError("Expected <" + Arrays.toString(expected) + "> but got <" + Arrays.toString(actual) + ">");
        }
    }

    static void assertLongArrayEquals(long[] expected, long[] actual) {
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError("Expected <" + Arrays.toString(expected) + "> but got <" + Arrays.toString(actual) + ">");
        }
    }

    static void assertDoubleArrayEquals(double[] expected, double[] actual) {
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError("Expected <" + Arrays.toString(expected) + "> but got <" + Arrays.toString(actual) + ">");
        }
    }

    static <T extends Throwable> void assertThrows(Class<T> expectedType, Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable expected) {
            if (expectedType.isInstance(expected)) {
                return;
            }
            throw new AssertionError("Expected " + expectedType.getName() + " but got " + expected.getClass().getName(), expected);
        }
        throw new AssertionError("Expected " + expectedType.getName());
    }
}
