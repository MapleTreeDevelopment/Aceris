package dev.mapletree.aceris;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class TomlInterpreter {
    private TomlInterpreter() {
    }

    static TomlDocument interpret(TomlAst.Document ast, String sourceText, String filename) {
        SourceText source = new SourceText(sourceText, filename);
        TableValue root = new TableValue(false, false);
        TableValue current = root;

        for (TomlAst.Statement statement : ast.statements()) {
            if (statement instanceof TomlAst.Table table) {
                current = ensureTable(root, table.path(), source, table.span(), true);
            } else if (statement instanceof TomlAst.ArrayTable arrayTable) {
                current = appendArrayTable(root, arrayTable.path(), source, arrayTable.span());
            } else if (statement instanceof TomlAst.KeyValue keyValue) {
                assign(current, keyValue.path(), materialize(keyValue.value(), source, keyValue.span()), source, keyValue.span());
            }
        }

        return new TomlDocument(root.toImmutableMap());
    }

    private static Object materialize(TomlValueNode node, SourceText source, SourceSpan span) {
        if (node instanceof TomlValueNode.Scalar scalar) {
            return scalar.value();
        }
        if (node instanceof TomlValueNode.ArrayValue array) {
            return array.values().stream()
                .map(value -> materialize(value, source, span))
                .toList();
        }
        if (node instanceof TomlValueNode.InlineTable inlineTable) {
            TableValue table = new TableValue(false, true);
            for (TomlAst.KeyValue entry : inlineTable.entries()) {
                assign(table, entry.path(), materialize(entry.value(), source, entry.span()), source, entry.span());
            }
            return table.toImmutableMap();
        }
        throw source.error("Unsupported TOML value node", span);
    }

    private static TableValue ensureTable(TableValue root, List<String> path, SourceText source, SourceSpan span, boolean explicit) {
        TableValue cursor = root;
        for (int index = 0; index < path.size(); index++) {
            String part = path.get(index);
            boolean finalSegment = index == path.size() - 1;
            Object existing = cursor.values.get(part);

            if (existing == null) {
                TableValue created = new TableValue(false, false);
                cursor.values.put(part, created);
                existing = created;
            }

            if (existing instanceof List<?> list) {
                if (list.isEmpty() || !(list.get(list.size() - 1) instanceof TableValue latest)) {
                    throw source.error("Array table '" + part + "' has no active item", span);
                }
                cursor = latest;
                continue;
            }

            if (!(existing instanceof TableValue table)) {
                throw source.error("Cannot redefine scalar '" + String.join(".", path.subList(0, index + 1)) + "' as table", span);
            }
            if (table.locked) {
                throw source.error("Cannot add keys to inline table '" + String.join(".", path.subList(0, index + 1)) + "'", span);
            }
            if (finalSegment && explicit) {
                if (table.explicit) {
                    throw source.error("Table '" + String.join(".", path) + "' is already defined", span);
                }
                table.explicit = true;
            }
            cursor = table;
        }
        return cursor;
    }

    @SuppressWarnings("unchecked")
    private static TableValue appendArrayTable(TableValue root, List<String> path, SourceText source, SourceSpan span) {
        TableValue parent = ensureTable(root, path.subList(0, path.size() - 1), source, span, false);
        String key = path.get(path.size() - 1);
        Object existing = parent.values.get(key);

        if (existing != null && !(existing instanceof List<?>)) {
            throw source.error("Cannot redefine '" + String.join(".", path) + "' as an array table", span);
        }

        List<TableValue> array;
        if (existing == null) {
            array = new ArrayList<>();
            parent.values.put(key, array);
        } else {
            array = (List<TableValue>) existing;
        }

        TableValue table = new TableValue(true, false);
        array.add(table);
        return table;
    }

    private static void assign(TableValue table, List<String> path, Object value, SourceText source, SourceSpan span) {
        TableValue cursor = table;

        for (int index = 0; index < path.size() - 1; index++) {
            String part = path.get(index);
            Object existing = cursor.values.get(part);
            if (existing == null) {
                existing = new TableValue(false, false);
                cursor.values.put(part, existing);
            }
            if (!(existing instanceof TableValue next)) {
                throw source.error("Cannot assign through non-table key '" + String.join(".", path.subList(0, index + 1)) + "'", span);
            }
            if (next.locked) {
                throw source.error("Cannot add keys to inline table '" + String.join(".", path.subList(0, index + 1)) + "'", span);
            }
            cursor = next;
        }

        String key = path.get(path.size() - 1);
        if (cursor.values.containsKey(key)) {
            throw source.error("Key '" + String.join(".", path) + "' is already defined", span);
        }
        cursor.values.put(key, value);
    }

    private static final class TableValue {
        private final Map<String, Object> values = new LinkedHashMap<>();
        private boolean explicit;
        private final boolean locked;

        private TableValue(boolean explicit, boolean locked) {
            this.explicit = explicit;
            this.locked = locked;
        }

        private Map<String, Object> toImmutableMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            values.forEach((key, value) -> result.put(key, immutable(value)));
            return Map.copyOf(result);
        }

        private Object immutable(Object value) {
            if (value instanceof TableValue table) {
                return table.toImmutableMap();
            }
            if (value instanceof List<?> list) {
                return List.copyOf(list.stream().map(this::immutable).toList());
            }
            return value;
        }
    }
}
