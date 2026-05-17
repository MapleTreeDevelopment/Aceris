package dev.mapletree.aceris;

import java.util.List;

public final class TomlAstTest {
    public static void main(String[] args) {
        runAll();
        TestSupport.pass(TomlAstTest.class.getSimpleName());
    }

    static void runAll() {
        TestSupport.run("exposes key value AST", TomlAstTest::exposesKeyValueAst);
        TestSupport.run("exposes table AST", TomlAstTest::exposesTableAst);
        TestSupport.run("exposes array table AST", TomlAstTest::exposesArrayTableAst);
    }

    private static void exposesKeyValueAst() {
        TomlAst.Document ast = Toml.parseAst("answer = 42");
        TestSupport.assertEquals(1, ast.statements().size());
        TomlAst.KeyValue keyValue = (TomlAst.KeyValue) ast.statements().get(0);
        TestSupport.assertEquals(List.of("answer"), keyValue.path());
    }

    private static void exposesTableAst() {
        TomlAst.Document ast = Toml.parseAst("[server]\nport = 8080");
        TestSupport.assertEquals(TomlAst.Table.class, ast.statements().get(0).getClass());
        TestSupport.assertEquals(List.of("server"), ((TomlAst.Table) ast.statements().get(0)).path());
    }

    private static void exposesArrayTableAst() {
        TomlAst.Document ast = Toml.parseAst("[[plugins]]\nname = \"audit\"");
        TestSupport.assertEquals(TomlAst.ArrayTable.class, ast.statements().get(0).getClass());
        TestSupport.assertEquals(List.of("plugins"), ((TomlAst.ArrayTable) ast.statements().get(0)).path());
    }
}
