package com.gonnect.mylang;

import mylang.antlr4.MyLangBaseVisitor;
import mylang.antlr4.MyLangLexer;
import mylang.antlr4.MyLangParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class MyLangEvalVisitor extends MyLangBaseVisitor<MyLangValue> {
	private static MyLangReturnValue MyLangReturnValue = new MyLangReturnValue();
    private MyLangScope MyLangScope;
    private Map<String, MyLangFunction> functions;
    
    MyLangEvalVisitor(MyLangScope MyLangScope, Map<String, MyLangFunction> functions) {
        this.MyLangScope = MyLangScope;
        this.functions = functions;
    }

    // functionDecl
    @Override
    public MyLangValue visitFunctionDecl(MyLangParser.FunctionDeclContext ctx) {
        return MyLangValue.VOID;
    }
    
    // list: '[' exprList? ']'
    @Override
    public MyLangValue visitList(MyLangParser.ListContext ctx) {
        List<MyLangValue> list = new ArrayList<>();
        if (ctx.exprList() != null) {
	        for(MyLangParser.ExpressionContext ex: ctx.exprList().expression()) {
	            list.add(this.visit(ex));
	        }
        }
        return new MyLangValue(list);
    }
    
    
    // '-' expression                           #unaryMinusExpression
    @Override
    public MyLangValue visitUnaryMinusExpression(MyLangParser.UnaryMinusExpressionContext ctx) {
    	MyLangValue v = this.visit(ctx.expression());
    	if (!v.isNumber()) {
    	    throw new MyLangEvalException(ctx);
        }
    	return new MyLangValue(-1 * v.asDouble());
    }

    // '!' expression                           #notExpression
    @Override
    public MyLangValue visitNotExpression(MyLangParser.NotExpressionContext ctx) {
    	MyLangValue v = this.visit(ctx.expression());
    	if(!v.isBoolean()) {
    	    throw new MyLangEvalException(ctx);
        }
    	return new MyLangValue(!v.asBoolean());
    }

    // expression '^' expression                #powerExpression
    @Override
    public MyLangValue visitPowerExpression(MyLangParser.PowerExpressionContext ctx) {
    	MyLangValue lhs = this.visit(ctx.expression(0));
    	MyLangValue rhs = this.visit(ctx.expression(1));
    	if (lhs.isNumber() && rhs.isNumber()) {
    		return new MyLangValue(Math.pow(lhs.asDouble(), rhs.asDouble()));
    	}
    	throw new MyLangEvalException(ctx);
    }

    // expression op=( '*' | '/' | '%' ) expression         #multExpression
    @Override
    public MyLangValue visitMultExpression(MyLangParser.MultExpressionContext ctx) {
        switch (ctx.op.getType()) {
            case MyLangLexer.Multiply:
                return multiply(ctx);
            case MyLangLexer.Divide:
                return divide(ctx);
            case MyLangLexer.Modulus:
                return modulus(ctx);
            default:
                throw new RuntimeException("unknown operator type: " + ctx.op.getType());
        }
    }

    // expression op=( '+' | '-' ) expression               #addExpression
    @Override
    public MyLangValue visitAddExpression(MyLangParser.AddExpressionContext ctx) {
        switch (ctx.op.getType()) {
            case MyLangLexer.Add:
                return add(ctx);
            case MyLangLexer.Subtract:
                return subtract(ctx);
            default:
                throw new RuntimeException("unknown operator type: " + ctx.op.getType());
        }
    }

    // expression op=( '>=' | '<=' | '>' | '<' ) expression #compExpression
    @Override
    public MyLangValue visitCompExpression(MyLangParser.CompExpressionContext ctx) {
        switch (ctx.op.getType()) {
            case MyLangLexer.LT:
                return lt(ctx);
            case MyLangLexer.LTEquals:
                return ltEq(ctx);
            case MyLangLexer.GT:
                return gt(ctx);
            case MyLangLexer.GTEquals:
                return gtEq(ctx);
            default:
                throw new RuntimeException("unknown operator type: " + ctx.op.getType());
        }
    }

    // expression op=( '==' | '!=' ) expression             #eqExpression
    @Override
    public MyLangValue visitEqExpression(MyLangParser.EqExpressionContext ctx) {
        switch (ctx.op.getType()) {
            case MyLangLexer.Equals:
                return eq(ctx);
            case MyLangLexer.NEquals:
                return nEq(ctx);
            default:
                throw new RuntimeException("unknown operator type: " + ctx.op.getType());
        }
    }
    
    public MyLangValue multiply(MyLangParser.MultExpressionContext ctx) {
    	MyLangValue lhs = this.visit(ctx.expression(0));
    	MyLangValue rhs = this.visit(ctx.expression(1));
    	if(lhs == null || rhs == null) {
    		System.err.println("lhs "+ lhs+ " rhs "+rhs);
    	    throw new MyLangEvalException(ctx);
    	}
    	
    	// number * number
        if(lhs.isNumber() && rhs.isNumber()) {
            return new MyLangValue(lhs.asDouble() * rhs.asDouble());
        }

        // string * number
        if(lhs.isString() && rhs.isNumber()) {
            StringBuilder str = new StringBuilder();
            int stop = rhs.asDouble().intValue();
            for(int i = 0; i < stop; i++) {
                str.append(lhs.asString());
            }
            return new MyLangValue(str.toString());
        }

        // list * number
        if(lhs.isList() && rhs.isNumber()) {
            List<MyLangValue> total = new ArrayList<>();
            int stop = rhs.asDouble().intValue();
            for(int i = 0; i < stop; i++) {
                total.addAll(lhs.asList());
            }
            return new MyLangValue(total);
        }   
         	
    	throw new MyLangEvalException(ctx);
    }
    
    private MyLangValue divide(MyLangParser.MultExpressionContext ctx) {
    	MyLangValue lhs = this.visit(ctx.expression(0));
    	MyLangValue rhs = this.visit(ctx.expression(1));
    	if (lhs.isNumber() && rhs.isNumber()) {
    		return new MyLangValue(lhs.asDouble() / rhs.asDouble());
    	}
    	throw new MyLangEvalException(ctx);
    }

	private MyLangValue modulus(MyLangParser.MultExpressionContext ctx) {
		MyLangValue lhs = this.visit(ctx.expression(0));
    	MyLangValue rhs = this.visit(ctx.expression(1));
    	if (lhs.isNumber() && rhs.isNumber()) {
    		return new MyLangValue(lhs.asDouble() % rhs.asDouble());
    	}
    	throw new MyLangEvalException(ctx);
	}

    private MyLangValue add(MyLangParser.AddExpressionContext ctx) {
        MyLangValue lhs = this.visit(ctx.expression(0));
        MyLangValue rhs = this.visit(ctx.expression(1));
        
        if(lhs == null || rhs == null) {
            throw new MyLangEvalException(ctx);
        }
        
        // number + number
        if(lhs.isNumber() && rhs.isNumber()) {
            return new MyLangValue(lhs.asDouble() + rhs.asDouble());
        }
        
        // list + any
        if(lhs.isList()) {
            List<MyLangValue> list = lhs.asList();
            list.add(rhs);
            return new MyLangValue(list);
        }

        // string + any
        if(lhs.isString()) {
            return new MyLangValue(lhs.asString() + "" + rhs.toString());
        }

        // any + string
        if(rhs.isString()) {
            return new MyLangValue(lhs.toString() + "" + rhs.asString());
        }
        
        return new MyLangValue(lhs.toString() + rhs.toString());
    }

    private MyLangValue subtract(MyLangParser.AddExpressionContext ctx) {
    	MyLangValue lhs = this.visit(ctx.expression(0));
    	MyLangValue rhs = this.visit(ctx.expression(1));
    	if (lhs.isNumber() && rhs.isNumber()) {
    		return new MyLangValue(lhs.asDouble() - rhs.asDouble());
    	}
    	if (lhs.isList()) {
            List<MyLangValue> list = lhs.asList();
            list.remove(rhs);
            return new MyLangValue(list);
        }
    	throw new MyLangEvalException(ctx);
    }

    private MyLangValue gtEq(MyLangParser.CompExpressionContext ctx) {
    	MyLangValue lhs = this.visit(ctx.expression(0));
    	MyLangValue rhs = this.visit(ctx.expression(1));
    	if (lhs.isNumber() && rhs.isNumber()) {
    		return new MyLangValue(lhs.asDouble() >= rhs.asDouble());
    	}
    	if(lhs.isString() && rhs.isString()) {
            return new MyLangValue(lhs.asString().compareTo(rhs.asString()) >= 0);
        }
    	throw new MyLangEvalException(ctx);
    }

    private MyLangValue ltEq(MyLangParser.CompExpressionContext ctx) {
    	MyLangValue lhs = this.visit(ctx.expression(0));
    	MyLangValue rhs = this.visit(ctx.expression(1));
    	if (lhs.isNumber() && rhs.isNumber()) {
    		return new MyLangValue(lhs.asDouble() <= rhs.asDouble());
    	}
    	if(lhs.isString() && rhs.isString()) {
            return new MyLangValue(lhs.asString().compareTo(rhs.asString()) <= 0);
        }
    	throw new MyLangEvalException(ctx);
    }

    private MyLangValue gt(MyLangParser.CompExpressionContext ctx) {
    	MyLangValue lhs = this.visit(ctx.expression(0));
    	MyLangValue rhs = this.visit(ctx.expression(1));
    	if (lhs.isNumber() && rhs.isNumber()) {
    		return new MyLangValue(lhs.asDouble() > rhs.asDouble());
    	}
    	if(lhs.isString() && rhs.isString()) {
            return new MyLangValue(lhs.asString().compareTo(rhs.asString()) > 0);
        }
    	throw new MyLangEvalException(ctx);
    }

    private MyLangValue lt(MyLangParser.CompExpressionContext ctx) {
    	MyLangValue lhs = this.visit(ctx.expression(0));
    	MyLangValue rhs = this.visit(ctx.expression(1));
    	if (lhs.isNumber() && rhs.isNumber()) {
    		return new MyLangValue(lhs.asDouble() < rhs.asDouble());
    	}
    	if(lhs.isString() && rhs.isString()) {
            return new MyLangValue(lhs.asString().compareTo(rhs.asString()) < 0);
        }
    	throw new MyLangEvalException(ctx);
    }

    private MyLangValue eq(MyLangParser.EqExpressionContext ctx) {
        MyLangValue lhs = this.visit(ctx.expression(0));
        MyLangValue rhs = this.visit(ctx.expression(1));
        if (lhs == null) {
        	throw new MyLangEvalException(ctx);
        }
        return new MyLangValue(lhs.equals(rhs));
    }

    private MyLangValue nEq(MyLangParser.EqExpressionContext ctx) {
        MyLangValue lhs = this.visit(ctx.expression(0));
        MyLangValue rhs = this.visit(ctx.expression(1));
        return new MyLangValue(!lhs.equals(rhs));
    }

    // expression '&&' expression               #andExpression
    @Override
    public MyLangValue visitAndExpression(MyLangParser.AndExpressionContext ctx) {
    	MyLangValue lhs = this.visit(ctx.expression(0));
    	MyLangValue rhs = this.visit(ctx.expression(1));
    	
    	if(!lhs.isBoolean() || !rhs.isBoolean()) {
    	    throw new MyLangEvalException(ctx);
        }
		return new MyLangValue(lhs.asBoolean() && rhs.asBoolean());
    }

    // expression '||' expression               #orExpression
    @Override
    public MyLangValue visitOrExpression(MyLangParser.OrExpressionContext ctx) {
    	MyLangValue lhs = this.visit(ctx.expression(0));
    	MyLangValue rhs = this.visit(ctx.expression(1));
    	
    	if(!lhs.isBoolean() || !rhs.isBoolean()) {
    	    throw new MyLangEvalException(ctx);
        }
		return new MyLangValue(lhs.asBoolean() || rhs.asBoolean());
    }

    // expression '?' expression ':' expression #ternaryExpression
    @Override
    public MyLangValue visitTernaryExpression(MyLangParser.TernaryExpressionContext ctx) {
    	MyLangValue condition = this.visit(ctx.expression(0));
    	if (condition.asBoolean()) {
    		return new MyLangValue(this.visit(ctx.expression(1)));
    	} else {
    		return new MyLangValue(this.visit(ctx.expression(2)));
    	}
    }

    // expression In expression                 #inExpression
	@Override
	public MyLangValue visitInExpression(MyLangParser.InExpressionContext ctx) {
		MyLangValue lhs = this.visit(ctx.expression(0));
    	MyLangValue rhs = this.visit(ctx.expression(1));
    	
    	if (rhs.isList()) {
    		for(MyLangValue val: rhs.asList()) {
    			if (val.equals(lhs)) {
    				return new MyLangValue(true);
    			}
    		}
    		return new MyLangValue(false);
    	}
    	throw new MyLangEvalException(ctx);
	}
	
    // Number                                   #numberExpression
    @Override
    public MyLangValue visitNumberExpression(MyLangParser.NumberExpressionContext ctx) {
        return new MyLangValue(Double.valueOf(ctx.getText()));
    }

    // Bool                                     #boolExpression
    @Override
    public MyLangValue visitBoolExpression(MyLangParser.BoolExpressionContext ctx) {
        return new MyLangValue(Boolean.valueOf(ctx.getText()));
    }

    // Null                                     #nullExpression
    @Override
    public MyLangValue visitNullExpression(MyLangParser.NullExpressionContext ctx) {
        return MyLangValue.NULL;
    }

    private MyLangValue resolveIndexes(MyLangValue val, List<MyLangParser.ExpressionContext> indexes) {
    	for (MyLangParser.ExpressionContext ec: indexes) {
    		MyLangValue idx = this.visit(ec);
    		if (!idx.isNumber() || (!val.isList() && !val.isString()) ) {
        		throw new MyLangEvalException("Problem resolving indexes on "+val+" at "+idx, ec);
    		}
    		int i = idx.asDouble().intValue();
    		if (val.isString()) {
    			val = new MyLangValue(val.asString().substring(i, i+1));
    		} else {
    			val = val.asList().get(i);
    		}
    	}
    	return val;
    }
    
    private void setAtIndex(ParserRuleContext ctx, List<MyLangParser.ExpressionContext> indexes, MyLangValue val, MyLangValue newVal) {
    	if (!val.isList()) {
    		throw new MyLangEvalException(ctx);
    	}
    	for (int i = 0; i < indexes.size() - 1; i++) {
    		MyLangValue idx = this.visit(indexes.get(i));
    		if (!idx.isNumber()) {
        		throw new MyLangEvalException(ctx);
    		}
    		val = val.asList().get(idx.asDouble().intValue());
    	}
    	MyLangValue idx = this.visit(indexes.get(indexes.size() - 1));
		if (!idx.isNumber()) {
    		throw new MyLangEvalException(ctx);
		}
    	val.asList().set(idx.asDouble().intValue(), newVal);
    }
    
    // functionCall indexes?                    #functionCallExpression
    @Override
    public MyLangValue visitFunctionCallExpression(MyLangParser.FunctionCallExpressionContext ctx) {
    	MyLangValue val = this.visit(ctx.functionCall());
    	if (ctx.indexes() != null) {
        	List<MyLangParser.ExpressionContext> exps = ctx.indexes().expression();
        	val = resolveIndexes(val, exps);
        }
    	return val;
    }

    // list indexes?                            #listExpression
    @Override
    public MyLangValue visitListExpression(MyLangParser.ListExpressionContext ctx) {
    	MyLangValue val = this.visit(ctx.list());
    	if (ctx.indexes() != null) {
        	List<MyLangParser.ExpressionContext> exps = ctx.indexes().expression();
        	val = resolveIndexes(val, exps);
        }
    	return val;
    }

    // Identifier indexes?                      #identifierExpression
    @Override
    public MyLangValue visitIdentifierExpression(MyLangParser.IdentifierExpressionContext ctx) {
        String id = ctx.Identifier().getText();
        MyLangValue val = MyLangScope.resolve(id);
        
        if (ctx.indexes() != null) {
        	List<MyLangParser.ExpressionContext> exps = ctx.indexes().expression();
        	val = resolveIndexes(val, exps);
        }
        return val;
    }

    // String indexes?                          #stringExpression
    @Override
    public MyLangValue visitStringExpression(MyLangParser.StringExpressionContext ctx) {
        String text = ctx.getText();
        text = text.substring(1, text.length() - 1).replaceAll("\\\\(.)", "$1");
        MyLangValue val = new MyLangValue(text);
        if (ctx.indexes() != null) {
        	List<MyLangParser.ExpressionContext> exps = ctx.indexes().expression();
        	val = resolveIndexes(val, exps);
        }
        return val;
    }

    // '(' expression ')' indexes?              #expressionExpression
    @Override
    public MyLangValue visitExpressionExpression(MyLangParser.ExpressionExpressionContext ctx) {
        MyLangValue val = this.visit(ctx.expression());
        if (ctx.indexes() != null) {
        	List<MyLangParser.ExpressionContext> exps = ctx.indexes().expression();
        	val = resolveIndexes(val, exps);
        }
        return val;
    }

    // Input '(' String? ')'                    #inputExpression
    @Override
    public MyLangValue visitInputExpression(MyLangParser.InputExpressionContext ctx) {
    	TerminalNode inputString = ctx.String();
		try {
			if (inputString != null) {
				String text = inputString.getText();
		        text = text.substring(1, text.length() - 1).replaceAll("\\\\(.)", "$1");
				return new MyLangValue(new String(Files.readAllBytes(Paths.get(text))));
			} else {
				BufferedReader buffer = new BufferedReader(new InputStreamReader(System.in));
				return new MyLangValue(buffer.readLine());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
    }

    
    // assignment
    // : Identifier indexes? '=' expression
    // ;
    @Override
    public MyLangValue visitAssignment(MyLangParser.AssignmentContext ctx) {
        MyLangValue newVal = this.visit(ctx.expression());
        if (ctx.indexes() != null) {
        	MyLangValue val = MyLangScope.resolve(ctx.Identifier().getText());
        	List<MyLangParser.ExpressionContext
                    > exps = ctx.indexes().expression();
        	setAtIndex(ctx, exps, val, newVal);
        } else {
        	String id = ctx.Identifier().getText();        	
        	MyLangScope.assign(id, newVal);
        }
        return MyLangValue.VOID;
    }

    // Identifier '(' exprList? ')' #identifierFunctionCall
    @Override
    public MyLangValue visitIdentifierFunctionCall(MyLangParser.IdentifierFunctionCallContext ctx) {
        List<MyLangParser.ExpressionContext> params = ctx.exprList() != null ? ctx.exprList().expression() : new ArrayList<MyLangParser.ExpressionContext>();
        String id = ctx.Identifier().getText() + params.size();
        MyLangFunction MyLangFunction;
        if ((MyLangFunction = functions.get(id)) != null) {
            return MyLangFunction.invoke(params, functions, MyLangScope);
        }
        throw new MyLangEvalException(ctx);
    }

    // Println '(' expression? ')'  #printlnFunctionCall
    @Override
    public MyLangValue visitPrintlnFunctionCall(MyLangParser.PrintlnFunctionCallContext ctx) {
        System.out.println(this.visit(ctx.expression()));
        return MyLangValue.VOID;
    }

    // Print '(' expression ')'     #printFunctionCall
    @Override
    public MyLangValue visitPrintFunctionCall(MyLangParser.PrintFunctionCallContext ctx) {
        System.out.print(this.visit(ctx.expression()));
        return MyLangValue.VOID;
    }

    // Assert '(' expression ')'    #assertFunctionCall
    @Override
    public MyLangValue visitAssertFunctionCall(MyLangParser.AssertFunctionCallContext ctx) {
    	MyLangValue value = this.visit(ctx.expression());

        if(!value.isBoolean()) {
            throw new MyLangEvalException(ctx);
        }

        if(!value.asBoolean()) {
            throw new AssertionError("Failed Assertion "+ctx.expression().getText()+" line:"+ctx.start.getLine());
        }

        return MyLangValue.VOID;
    }

    // Size '(' expression ')'      #sizeFunctionCall
    @Override
    public MyLangValue visitSizeFunctionCall(MyLangParser.SizeFunctionCallContext ctx) {
    	MyLangValue value = this.visit(ctx.expression());

        if(value.isString()) {
            return new MyLangValue(value.asString().length());
        }

        if(value.isList()) {
            return new MyLangValue(value.asList().size());
        }

        throw new MyLangEvalException(ctx);
    }

    @Override
    public MyLangValue visitIfStatement(MyLangParser.IfStatementContext ctx) {

        // if ...
        if(this.visit(ctx.ifStat().expression()).asBoolean()) {
            return this.visit(ctx.ifStat().block());
        }

        // else if ...
        for(int i = 0; i < ctx.elseIfStat().size(); i++) {
            if(this.visit(ctx.elseIfStat(i).expression()).asBoolean()) {
                return this.visit(ctx.elseIfStat(i).block());
            }
        }

        // else ...
        if(ctx.elseStat() != null) {
            return this.visit(ctx.elseStat().block());
        }

        return MyLangValue.VOID;
    }
    
    // block
    // : (statement | functionDecl)* (Return expression)?
    // ;
    @Override
    public MyLangValue visitBlock(MyLangParser.BlockContext
                                              ctx) {
    		
    	MyLangScope = new MyLangScope(MyLangScope); // create new local MyLangScope
        for (MyLangParser.StatementContext sx: ctx.statement()) {
            this.visit(sx);
        }
        MyLangParser.ExpressionContext ex;
        if ((ex = ctx.expression()) != null) {
        	MyLangReturnValue.value = this.visit(ex);
        	MyLangScope = MyLangScope.parent();
        	throw MyLangReturnValue;
        }
        MyLangScope = MyLangScope.parent();
        return MyLangValue.VOID;
    }
    
    // forStatement
    // : For Identifier '=' expression To expression OBrace block CBrace
    // ;
    @Override
    public MyLangValue visitForStatement(MyLangParser.ForStatementContext ctx) {
        int start = this.visit(ctx.expression(0)).asDouble().intValue();
        int stop = this.visit(ctx.expression(1)).asDouble().intValue();
        for(int i = start; i <= stop; i++) {
            MyLangScope.assign(ctx.Identifier().getText(), new MyLangValue(i));
            MyLangValue returnValue = this.visit(ctx.block());
            if(returnValue != MyLangValue.VOID) {
                return returnValue;
            }
        }
        return MyLangValue.VOID;
    }
    
    // whileStatement
    // : While expression OBrace block CBrace
    // ;
    @Override
    public MyLangValue visitWhileStatement(MyLangParser.WhileStatementContext ctx) {
        while( this.visit(ctx.expression()).asBoolean() ) {
            MyLangValue returnValue = this.visit(ctx.block());
            if (returnValue != MyLangValue.VOID) {
                return returnValue;
            }
        }
        return MyLangValue.VOID;
    }
    
}
