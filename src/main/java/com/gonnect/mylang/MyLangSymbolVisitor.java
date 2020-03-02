package com.gonnect.mylang;

import mylang.antlr4.MyLangBaseVisitor;
import mylang.antlr4.MyLangParser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MyLangSymbolVisitor extends MyLangBaseVisitor<MyLangValue> {

    private Map<String, MyLangFunction> functions;
    
    MyLangSymbolVisitor(Map<String, MyLangFunction> functions) {
        this.functions = functions;
    }
    
    @Override
    public MyLangValue visitFunctionDecl(MyLangParser.FunctionDeclContext ctx) {
        List<TerminalNode> params = ctx.idList() != null ? ctx.idList().Identifier() : new ArrayList<TerminalNode>(); 
        ParseTree block = ctx.block();
        String id = ctx.Identifier().getText() + params.size();
        functions.put(id, new MyLangFunction(params, block));
        return MyLangValue.VOID;
    }
}
