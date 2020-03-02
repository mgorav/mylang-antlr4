package com.gonnect.mylang;

import org.antlr.v4.runtime.ParserRuleContext;

public class MyLangEvalException extends RuntimeException {
    public MyLangEvalException(ParserRuleContext ctx) {
        this("Illegal expression: " + ctx.getText(), ctx);
    }
    
    public MyLangEvalException(String msg, ParserRuleContext ctx) {
        super(msg + " line:" + ctx.start.getLine());
    }
}
