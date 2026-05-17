package dev.mapletree.aceris;

import java.util.List;

/**
 * Immutable TOML syntax tree.
 */
public final class TomlAst {
    private TomlAst() {
    }

    public sealed interface Statement permits Table, ArrayTable, KeyValue {
        SourceSpan span();
    }

    public record Document(List<Statement> statements) {
        public Document {
            statements = List.copyOf(statements);
        }
    }

    public record Table(List<String> path, SourceSpan span) implements Statement {
        public Table {
            path = List.copyOf(path);
        }
    }

    public record ArrayTable(List<String> path, SourceSpan span) implements Statement {
        public ArrayTable {
            path = List.copyOf(path);
        }
    }

    public record KeyValue(List<String> path, TomlValueNode value, SourceSpan span) implements Statement {
        public KeyValue {
            path = List.copyOf(path);
        }
    }
}
