package dev.mapletree.aceris;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public final class TomlArrayCollectionTest {
    public static void main(String[] args) {
        runAll();
        TestSupport.pass(TomlArrayCollectionTest.class.getSimpleName());
    }

    static void runAll() {
        TestSupport.run("parses arrays", TomlArrayCollectionTest::parsesArrays);
        TestSupport.run("allows mixed arrays per TOML 1.0", TomlArrayCollectionTest::allowsMixedArraysPerToml10);
        TestSupport.run("converts collection types", TomlArrayCollectionTest::convertsCollectionTypes);
        TestSupport.run("converts primitive arrays", TomlArrayCollectionTest::convertsPrimitiveArrays);
        TestSupport.run("converts nested generic arrays of tables", TomlArrayCollectionTest::convertsNestedGenericArraysOfTables);
    }

    private static void allowsMixedArraysPerToml10() {
        TomlDocument document = Toml.parse("mixed = [1, \"two\", true]");
        TestSupport.assertEquals(List.of(1L, "two", true), document.getArray("mixed").orElseThrow());
    }

    private static void parsesArrays() {
        TomlDocument document = Toml.parse("""
            ports = [
              8001,
              8002,
            ]
            mixed_strings = ["a", "b"]
            """);

        TestSupport.assertEquals(List.of(8001L, 8002L), document.getArray("ports").orElseThrow());
        TestSupport.assertEquals(List.of("a", "b"), document.getArray("mixed_strings").orElseThrow());
    }

    private static void convertsCollectionTypes() {
        TomlDocument document = Toml.parse("""
            numbers = [3, 1, 2, 2]
            modes = ["ALPHA", "BETA"]
            """);

        TestSupport.assertEquals(List.of(3, 1, 2, 2), document.getList("numbers", Integer.class).orElseThrow());
        TestSupport.assertEquals(Set.of(1, 2, 3), document.getSet("numbers", Integer.class).orElseThrow());
        TestSupport.assertEquals(new TreeSet<>(List.of(1, 2, 3)), document.getSortedSet("numbers", Integer.class).orElseThrow());
        TestSupport.assertEquals(EnumSet.of(TestModels.Mode.ALPHA, TestModels.Mode.BETA), document.getEnumSet("modes", TestModels.Mode.class).orElseThrow());
    }

    private static void convertsPrimitiveArrays() {
        TomlDocument document = Toml.parse("""
            ints = [3, 1, 2, 2]
            doubles = [1.5, 2.25]
            """);

        TestSupport.assertIntArrayEquals(new int[] {3, 1, 2, 2}, document.getAs("ints", int[].class).orElseThrow());
        TestSupport.assertLongArrayEquals(new long[] {3L, 1L, 2L, 2L}, document.getAs("ints", long[].class).orElseThrow());
        TestSupport.assertDoubleArrayEquals(new double[] {1.5d, 2.25d}, document.getAs("doubles", double[].class).orElseThrow());
        TestSupport.assertEquals(List.of(3, 1, 2, 2), Arrays.asList(document.getArray("ints", Integer.class).orElseThrow()));
    }

    private static void convertsNestedGenericArraysOfTables() {
        TomlDocument document = Toml.parse("""
            [[servers]]
            host = "alpha"
            port = 8001
            mode = "ALPHA"

            [[servers]]
            host = "beta"
            port = 8002
            mode = "BETA"
            """);

        List<TestModels.ServerConfig> servers = document.getAs("servers", new TomlType<List<TestModels.ServerConfig>>() {
        }).orElseThrow();
        TestSupport.assertEquals(2, servers.size());
        TestSupport.assertEquals("alpha", servers.get(0).host());
        TestSupport.assertEquals(TestModels.Mode.BETA, servers.get(1).mode());
    }
}
