package dev.mapletree.aceris;

import java.util.Locale;
import java.util.Map;

public final class TomlBenchmark {
    private static final int WARMUP_ITERATIONS = Integer.getInteger("aceris.benchmark.warmup", 250);
    private static final int MEASURE_ITERATIONS = Integer.getInteger("aceris.benchmark.iterations", 1_000);

    public static void main(String[] args) {
        System.out.println("Aceris dependency-free benchmark");
        System.out.println("warmup=" + WARMUP_ITERATIONS + ", iterations=" + MEASURE_ITERATIONS);
        System.out.println();

        run("parse small document", TomlBenchmark::parseSmallDocument);
        run("parse and map small document", TomlBenchmark::parseAndMapSmallDocument);
        run("parse large array", TomlBenchmark::parseLargeArray);
        run("parse many tables", TomlBenchmark::parseManyTables);
        run("map many table objects", TomlBenchmark::mapManyTableObjects);
    }

    private static void run(String name, BenchmarkCase benchmarkCase) {
        long checksum = 0;
        for (int iteration = 0; iteration < WARMUP_ITERATIONS; iteration++) {
            checksum ^= benchmarkCase.execute();
        }

        long start = System.nanoTime();
        for (int iteration = 0; iteration < MEASURE_ITERATIONS; iteration++) {
            checksum ^= benchmarkCase.execute();
        }
        long elapsedNanos = System.nanoTime() - start;

        double totalMillis = elapsedNanos / 1_000_000.0;
        double averageMicros = elapsedNanos / 1_000.0 / MEASURE_ITERATIONS;
        double opsPerSecond = MEASURE_ITERATIONS / (elapsedNanos / 1_000_000_000.0);

        System.out.printf(
            Locale.ROOT,
            "%-28s total=%8.2f ms  avg=%8.2f us  ops/s=%10.2f  checksum=%d%n",
            name,
            totalMillis,
            averageMicros,
            opsPerSecond,
            checksum
        );
    }

    private static long parseSmallDocument() {
        TomlDocument document = Toml.parse(Samples.SMALL);
        return document.getLong("server.port").orElseThrow();
    }

    private static long parseAndMapSmallDocument() {
        TomlDocument document = Toml.parse(Samples.SMALL);
        BenchServer server = document.getAs("server", BenchServer.class).orElseThrow();
        return server.port() + server.host().length();
    }

    private static long parseLargeArray() {
        TomlDocument document = Toml.parse(Samples.LARGE_ARRAY);
        return document.getArray("values").orElseThrow().size();
    }

    private static long parseManyTables() {
        TomlDocument document = Toml.parse(Samples.MANY_TABLES);
        return document.getTable("services").orElseThrow().size();
    }

    private static long mapManyTableObjects() {
        TomlDocument document = Toml.parse(Samples.MANY_TABLES);
        Map<String, BenchService> services = document.getAs("services", new TomlType<Map<String, BenchService>>() {
        }).orElseThrow();
        return services.values().stream().mapToLong(BenchService::port).sum();
    }

    private record BenchServer(String host, int port, boolean enabled) {
    }

    private record BenchService(String host, int port, boolean enabled) {
    }

    @FunctionalInterface
    private interface BenchmarkCase {
        long execute();
    }

    private static final class Samples {
        private static final String SMALL = """
            title = "Benchmark"

            [server]
            host = "127.0.0.1"
            port = 8080
            enabled = true

            [database]
            url = "postgres://localhost/app"
            pool.max = 20
            """;

        private static final String LARGE_ARRAY = largeArray();
        private static final String MANY_TABLES = manyTables();

        private static String largeArray() {
            StringBuilder builder = new StringBuilder("values = [\n");
            for (int index = 0; index < 10_000; index++) {
                builder.append(index);
                if (index < 9_999) {
                    builder.append(',');
                }
                builder.append('\n');
            }
            builder.append("]\n");
            return builder.toString();
        }

        private static String manyTables() {
            StringBuilder builder = new StringBuilder();
            for (int index = 0; index < 1_000; index++) {
                builder.append("[services.service_").append(index).append("]\n")
                    .append("host = \"10.0.0.").append(index % 255).append("\"\n")
                    .append("port = ").append(8_000 + index).append('\n')
                    .append("enabled = ").append(index % 2 == 0 ? "true" : "false").append("\n\n");
            }
            return builder.toString();
        }
    }
}
