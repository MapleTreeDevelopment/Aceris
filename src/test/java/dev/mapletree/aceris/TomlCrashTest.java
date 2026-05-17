package dev.mapletree.aceris;

import java.util.Random;

public final class TomlCrashTest {
    private static final String[] FRAGMENTS = {
        "",
        "key",
        "key = ",
        "key = 1",
        "key = \"value\"",
        "key = [1, 2,",
        "[table]",
        "[[array]]",
        "\"quoted key\" = \"value\"",
        "bad = \"\\UFFFFFFFF\"",
        "inline = { a = 1, b = [true, false] }",
        "nested = [[[1]]]"
    };

    public static void main(String[] args) {
        runAll();
        TestSupport.pass(TomlCrashTest.class.getSimpleName());
    }

    static void runAll() {
        TestSupport.run("parses or rejects deterministic random inputs without raw crashes", TomlCrashTest::parsesOrRejectsDeterministicRandomInputsWithoutRawCrashes);
    }

    private static void parsesOrRejectsDeterministicRandomInputsWithoutRawCrashes() {
        Random random = new Random(667L);
        TomlOptions options = new TomlOptions(4096, 16, 256, 256, 2048, 2048);

        for (int iteration = 0; iteration < 500; iteration++) {
            String input = randomInput(random);
            try {
                Toml.parse(input, "crash-" + iteration + ".toml", options);
            } catch (TomlParseException | TomlConversionException | IllegalArgumentException expected) {
                // Expected safe failure modes for arbitrary malformed input.
            } catch (RuntimeException unexpected) {
                throw new AssertionError("Unexpected raw crash for input:\n" + input, unexpected);
            }
        }
    }

    private static String randomInput(Random random) {
        StringBuilder builder = new StringBuilder();
        int lines = 1 + random.nextInt(12);
        for (int line = 0; line < lines; line++) {
            if (random.nextBoolean()) {
                builder.append(FRAGMENTS[random.nextInt(FRAGMENTS.length)]);
            } else {
                builder.append(randomKey(random)).append(" = ").append(randomValue(random));
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    private static String randomKey(Random random) {
        return switch (random.nextInt(4)) {
            case 0 -> "k" + random.nextInt(10);
            case 1 -> "\"quoted " + random.nextInt(10) + "\"";
            case 2 -> "section.value";
            default -> "bad..key";
        };
    }

    private static String randomValue(Random random) {
        return switch (random.nextInt(8)) {
            case 0 -> String.valueOf(random.nextInt(1_000));
            case 1 -> "\"" + random.nextInt(1_000) + "\"";
            case 2 -> random.nextBoolean() ? "true" : "false";
            case 3 -> "[" + random.nextInt(10) + ", " + random.nextInt(10) + "]";
            case 4 -> "{ a = " + random.nextInt(10) + " }";
            case 5 -> "2026-05-17";
            case 6 -> "\"\\u0041\"";
            default -> "\"\\UFFFFFFFF\"";
        };
    }
}
