package dev.mapletree.aceris;

public final class TomlTestSuite {
    public static void main(String[] args) {
        TomlIntegerTypeTest.runAll();
        TomlFloatTypeTest.runAll();
        TomlStringTypeTest.runAll();
        TomlTemporalTypeTest.runAll();
        TomlArrayCollectionTest.runAll();
        TomlTableScenarioTest.runAll();
        TomlObjectMappingTest.runAll();
        TomlWriterTest.runAll();
        TomlConfigTest.runAll();
        TomlScriptRunnerTest.runAll();
        TomlConformanceTest.runAll();
        TomlStabilityTest.runAll();
        TomlAstTest.runAll();
        TomlCrashTest.runAll();
        System.out.println("All Aceris tests passed.");
    }
}
