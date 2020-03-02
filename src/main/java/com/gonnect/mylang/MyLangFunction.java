package com.gonnect.mylang;

import mylang.antlr4.MyLangParser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;
import java.util.Map;

public class MyLangFunction {

    private List<TerminalNode> params;
    private ParseTree block;

    MyLangFunction(List<TerminalNode> params, ParseTree block) {
        this.params = params;
        this.block = block;
    }
    
    public MyLangValue invoke(List<MyLangParser.ExpressionContext> params, Map<String, MyLangFunction> functions, MyLangScope MyLangScope) {
        if (params.size() != this.params.size()) {
            throw new RuntimeException("Illegal MyLangFunction call");
        }
        MyLangScope MyLangScopeNext = new MyLangScope(null); // create function MyLangScope

        MyLangEvalVisitor MyLangEvalVisitor = new MyLangEvalVisitor(MyLangScope, functions);
        for (int i = 0; i < this.params.size(); i++) {
            MyLangValue value = MyLangEvalVisitor.visit(params.get(i));
            MyLangScopeNext.assignParam(this.params.get(i).getText(), value);
        }
        MyLangEvalVisitor evalVistorNext = new MyLangEvalVisitor(MyLangScopeNext,functions);
        
        MyLangValue ret = MyLangValue.VOID;
        try {
        	evalVistorNext.visit(this.block);
        } catch (MyLangReturnValue MyLangReturnValue) {
        	ret = MyLangReturnValue.value;
        }
        return ret;
    }
}
