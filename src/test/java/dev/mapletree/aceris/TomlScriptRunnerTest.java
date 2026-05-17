package dev.mapletree.aceris;

import dev.mapletree.aceris.script.TomlScript;
import dev.mapletree.aceris.script.TomlScriptExecutionException;
import dev.mapletree.aceris.script.TomlScriptRunner;
import java.util.Map;

public final class TomlScriptRunnerTest {
    public static void main(String[] args) {
        runAll();
        TestSupport.pass(TomlScriptRunnerTest.class.getSimpleName());
    }

    static void runAll() {
        TestSupport.run("loads script definitions from TOML", TomlScriptRunnerTest::loadsScriptDefinitionsFromToml);
        TestSupport.run("executes only registered engines", TomlScriptRunnerTest::executesOnlyRegisteredEngines);
        TestSupport.run("fails safely for missing engines and malformed scripts", TomlScriptRunnerTest::failsSafelyForMissingEnginesAndMalformedScripts);
    }

    private static void loadsScriptDefinitionsFromToml() {
        TomlDocument document = Toml.parse("""
            [scripts.hello]
            engine = "echo"
            source = "hello"
            options.cwd = "/tmp"
            """);

        TomlScript script = new TomlScriptRunner().scripts(document).get(0);
        TestSupport.assertEquals("hello", script.name());
        TestSupport.assertEquals("echo", script.engine());
        TestSupport.assertEquals("hello", script.source());
        TestSupport.assertEquals("/tmp", script.options().get("cwd"));
    }

    private static void executesOnlyRegisteredEngines() {
        TomlDocument document = Toml.parse("""
            [scripts.hello]
            engine = "echo"
            source = "hello"
            """);
        TomlScriptRunner runner = new TomlScriptRunner()
            .register("echo", (script, context) -> script.source() + " " + context.get("name"));

        TestSupport.assertEquals("hello Aceris", runner.execute(document, "hello", Map.of("name", "Aceris")));
    }

    private static void failsSafelyForMissingEnginesAndMalformedScripts() {
        TomlDocument missingEngine = Toml.parse("""
            [scripts.hello]
            engine = "shell"
            source = "echo hello"
            """);
        TomlDocument malformed = Toml.parse("""
            [scripts.bad]
            source = "missing engine"
            """);

        TomlScriptRunner runner = new TomlScriptRunner();
        TestSupport.assertThrows(TomlScriptExecutionException.class, () -> runner.execute(missingEngine, "hello"));
        TestSupport.assertThrows(TomlScriptExecutionException.class, () -> runner.scripts(malformed));
    }
}
