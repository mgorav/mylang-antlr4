package com.gonnect.mylang;

import mylang.antlr4.MyLangLexer;
import mylang.antlr4.MyLangParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.HashMap;
import java.util.Map;

public class MyLangExecutor {
    public static void main(String[] args) {
        try {
            MyLangLexer lexer = new MyLangLexer(CharStreams.fromFileName("src/main/mylang/MyLangTest.mylang"));
            MyLangParser parser = new MyLangParser(new CommonTokenStream(lexer));
            parser.setBuildParseTree(true);
            ParseTree tree = parser.parse();

            MyLangScope MyLangScope = new MyLangScope();
            Map<String, MyLangFunction> functions = new HashMap<>();
            MyLangSymbolVisitor myLangSymbolVisitor = new MyLangSymbolVisitor(functions);
            myLangSymbolVisitor.visit(tree);
            MyLangEvalVisitor visitor = new MyLangEvalVisitor(MyLangScope, functions);
            visitor.visit(tree);
        } catch (Exception e) {
            if (e.getMessage() != null) {
                System.err.println(e.getMessage());
            } else {
                e.printStackTrace();
            }
        }
    }
}