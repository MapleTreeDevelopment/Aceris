package dev.mapletree.aceris;

import java.util.List;

sealed interface TomlValueNode permits TomlValueNode.Scalar, TomlValueNode.ArrayValue, TomlValueNode.InlineTable {
    record Scalar(Object value) implements TomlValueNode {
    }

    record ArrayValue(List<TomlValueNode> values) implements TomlValueNode {
    }

    record InlineTable(List<TomlAst.KeyValue> entries) implements TomlValueNode {
    }
}
