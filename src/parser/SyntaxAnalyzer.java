package parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import compiler.*; //inside this folder we can find the package with ILexicalAnalyzer and all necesary dataTypes in html format


public class SyntaxAnalyzer implements ISyntaxAnalyzer {

    private Token token;
    private LexicalAnalyzer lexicalAnalyzer;
    private HashMap<String, DataType> symbolTable;
    private List<String> code;
    private String compile;
    private int label, elseCond, out;

    public SyntaxAnalyzer(LexicalAnalyzer lex) {
        this.lexicalAnalyzer = lex;
        this.token = this.lexicalAnalyzer.getToken();
        this.symbolTable = new HashMap<String, DataType>();
        this.code = new ArrayList<String>();
        this.compile = "";
        this.label = -1;
    }

    //Returns the intermediate code generated during parsing as a string.
    public String intermediateCode() {
        String code = "";

        for (String instruction : this.code) {
            code = code + instruction + "\n";
        }

        return code;
    }

    //Returns a string representation of the symbol table, listing all identifiers and their types.
    public String symbolTable() {
        String symbols = "";

        Set<Map.Entry<String, DataType>> s = this.symbolTable.entrySet();

        for (Map.Entry<String, DataType> m : s) {
            symbols = symbols + "<'" + m.getKey() + "', "
                    + m.getValue().toString() + "> \n";
        }

        return symbols;
    }

    //Starts the parsing process by calling program(). Returns true if no errors were found, otherwise false
    public boolean compile() {
        program();
        return (this.compile.length() == 0) ? true : false;
    }

    private void match(String tokenName) {
        if (this.token.getName().equals(tokenName)) {
            this.token = this.lexicalAnalyzer.getToken();
        } else {
            this.compile = this.compile + "\nError at line "
                    + this.lexicalAnalyzer.getLine() + ", "
                    + this.lexicalAnalyzer.getLexeme(tokenName)
                    + " expected";
        }
    }

    //Returns accumulated error messages from the parsing process.
    @Override
    public String output() {
        return this.compile;
    }

    //Generates a new label for control flow instructions.
    private int newLabel() {
        this.label++;
        return this.label;
    } 

    //Entry point for parsing. Expects a specific program structure (void main { ... }).
    private void program() {
        match("void");
        match("main");
        match("open_curly_bracket");
        declarations();
        instructions();
        match("closed_curly_bracket");
        this.code.add("halt");
    }

    //Handle variable declarations, types, and optional assignments.
    private void declarations() {
        String tokenName = this.token.getName();
        if (tokenName.equals("int") || tokenName.equals("float") || tokenName.equals("boolean")) {
            declaration();
            declarations();
        }
    }

    //handles variable declaration
    private void declaration() {
        identifiers(type());
        match("semicolon");
    }

    //returns the data type of the current token
    private String type() {
        String type = this.token.getName();

        if (type.equals("int")) {
            match("int");

        } else if (type.equals("float")) {
            match("float");

        } else if (type.equals("boolean")) {
            match("boolean");
        }
        return type;
    }

    //handles variable identifiers
    private void identifiers(String type) {
        if (this.token.getName().equals("id")) {
            Identifier id = (Identifier) this.token;

            //if the id.lexeme already exists in the symbol table 
            if (this.symbolTable.get(id.getLexeme()) == null) {
                this.symbolTable.put(id.getLexeme(), new PrimitiveType(type));
            } else {
                this.compile = this.compile + "\nError at line " + this.lexicalAnalyzer.getLine() + " identifier " + id.getLexeme() + " is already declared";
            }
            match("id");
            optionalAssignment(id);
            moreIdentifiers(type);
        }
    }

    //handles more variable identifiers
    private void moreIdentifiers(String type) {
        if (this.token.getName().equals("comma")) {
            match("comma");
            Identifier id = (Identifier) this.token;

            if (this.symbolTable.get(id.getLexeme()) == null) {
                this.symbolTable.put(id.getLexeme(), new PrimitiveType(type));
            } else {
                this.compile = this.compile + "\nError at line " + this.lexicalAnalyzer.getLine() + " identifier " + id.getLexeme() + " is already declared";
            }
            match("id");
            optionalAssignment(id);
            moreIdentifiers(type);
        }
    }

    //handles optional variable assignment
    private void optionalAssignment(Identifier id) {
        if (this.token.getName().equals("assignment")) {
            match("assignment");
            this.code.add("lvalue " + id.getLexeme());
            logicExpression();
            this.code.add("=");
        }
    }

    //Parse statements such as assignments, control flow (if, while, do-while), print statements, and blocks
    private void instructions() {
        String tokenName = this.token.getName();
        if (tokenName.equals("int") || tokenName.equals("float") || tokenName.equals("boolean") || tokenName.equals("id") || tokenName.equals("if") || tokenName.equals("while") || tokenName.equals("do") || tokenName.equals("print") || tokenName.equals("open_curly_brackets")) {
            instruction();
            instructions();
        }
    }

    //Handles individual instructions
    private void instruction() {
        String tokenName = this.token.getName();

        if (tokenName.equals("id")) {
            Identifier id = (Identifier) this.token;

            if (this.symbolTable.get(id.getLexeme()) == null) {
                this.compile = this.compile + "\nError at line " + this.lexicalAnalyzer.getLine() + " identifier " + id.getLexeme() + " is not declared";
            }

            this.code.add("lvalue " + id.getLexeme());
            match("id");
            match("assignment");
            logicExpression();
            this.code.add("=");
            match("semicolon");

        } else if (tokenName.equals("if")) {
            match("if");
            match("open_parenthesis");
            logicExpression();
            match("closed_parenthesis");

            this.elseCond = newLabel();
            String elseCondition = ("gofalse label_" + this.elseCond + ":");
            this.code.add(elseCondition);

            instruction();
            optionalElse();

            this.out = newLabel();
            String outCondition = ("goto label_" + this.out + ":");
            this.code.add(outCondition);

        } else if (tokenName.equals("while")) {
            match("while");

            int test = newLabel();
            String labelT = ("label_" + test + ":");
            this.code.add(labelT);

            match("open_parenthesis");
            logicExpression();
            match("closed_parenthesis");

            int out1 = newLabel();
            String goFalse = ("gofalse label_" + out1);
            this.code.add(goFalse);

            instruction();

            String goToT = ("goto label_" + test);
            this.code.add(goToT);

            String label1 = ("label_" + out1 + ":");
            this.code.add(label1);

        } else if (tokenName.equals("do")) {
            match("do");

            int test1 = newLabel();
            String labelT1 = ("label " + test1 + ":");
            this.code.add(labelT1);

            instruction();

            match("while");
            match("open_parenthesis");
            logicExpression();
            match("closed_parenthesis");

            int out2 = newLabel();
            String goFalse1 = ("gofalse label_" + out2);
            this.code.add(goFalse1);

            String goTo = ("goto label_" + test1);
            this.code.add(goTo);

            String label2 = ("label_" + out2 + ":");
            this.code.add(label2);

            match("semicolon");

        } else if (tokenName.equals("print")) {
            match("print");
            match("open_parenthesis");

            Identifier id = (Identifier) token;
            this.code.add("print " + id.getLexeme());

            match("id");
            match("closed_parenthesis");
            match("semicolon");

        } else if (tokenName.equals("open_curly_bracket")) {
            match("open_curly_bracket");
            instructions();
            match("closed_curly_bracket");

        } else if (tokenName.equals("int") || tokenName.equals("float") || tokenName.equals("boolean")) {
            declaration();
        }
    }

    //Handles optional else clauses in if statements
    private void optionalElse() {
        String tokenName = this.token.getName();
        if (tokenName.equals("else")) {
            String label = ("label" + this.elseCond);
            this.code.add(label);

            match("else");
            instruction();

            String labelOut = ("label" + this.out);
            this.code.add(labelOut);
        }
    }

    //Parse logical expressions (and, or, not, relational operators).
    private void logicExpression() {
        logicTerm();
        moreLogicTerm();
    }

    private void moreLogicTerm() {
        String tokenName = this.token.getName();
        if (tokenName.equals("or")) {
            match("or");
            logicTerm();
            this.code.add("||");
            moreLogicTerm();
        }
    }

    private void logicTerm() {
        logicFactor();
        moreLogicFactor();
    }

    private void moreLogicFactor() {
        String tokenName = this.token.getName();
        if (tokenName.equals("and")) {
            match("and");
            logicFactor();
            this.code.add("&&");
            moreLogicFactor();
        }
    }

    private void logicFactor() {
        String tokenName = this.token.getName();
        if (tokenName.equals("not")) {
            match("not");
            this.code.add("!");
            logicFactor();
        } else if (tokenName.equals("true")) {
            match("true");
        } else if (tokenName.equals("false")) {
            match("false");
        } else {
            relationalExpression();
        }
    }

    //Parse relational expressions (less than, greater than, equals, etc.)
    private void relationalExpression() {
        expression();
        moreRelationalExpression();
    }

    private void moreRelationalExpression() {
        String tokenName = this.token.getName();
        if (tokenName.equals("greater_than") || tokenName.equals("less_than") || tokenName.equals("less_equals") || tokenName.equals("greater_equals") || tokenName.equals("equals") || tokenName.equals("not_equals")) {

            String operator = this.lexicalAnalyzer.getLexeme(tokenName);

            relationalOperator();

            expression();

            this.code.add(operator);
        }
    }

    private void relationalOperator() {
        String operator = this.token.getName();
        if (operator.equals("less_than")) {
            match("less_than");

        } else if (operator.equals("less_equals")) {
            match("less_equals");

        } else if (operator.equals("greater_than")) {
            match("greater_than");

        } else if (operator.equals("greater_equals")) {
            match("greater_equals");

        } else if (operator.equals("equals")) {
            match("equals");

        } else if (operator.equals("not_equals")) {
            match("not_equals");
        }
    }

    //Parse expressions (terms and operators)
    private void expression() {
        term();
        moreTerms();
    }

    private void term() {
        factor();
        moreFactors();
    }

    private void moreTerms() {
        if (this.token.getName().equals("add")) {
            match("add");
            term();
            this.code.add("+");
            moreTerms();
        } else if (this.token.getName().equals("subtract")) {
            match("subtract");
            term();
            this.code.add("-");
            moreTerms();
        }
    }

    //Handles atomic expressions: numbers, identifiers, and parenthesized expressions.
    private void factor() {
        if (this.token.getName().equals("open_parenthesis")) {
            match("open_parenthesis");
            expression();
            match("closed_parenthesis");
        } else if (this.token.getName().equals("int")) {
            IntegerNumber number = (IntegerNumber) this.token;
            this.code.add("push " + number.getValue());
            match("int");
        } else if (this.token.getName().equals("float")) {
            RealNumber number = (RealNumber) this.token;
            this.code.add("push " + number.getValue());
            match("float");
        } else if (this.token.getName().equals("id")) {
            Identifier id = (Identifier) this.token;
            this.code.add("rvalue " + id.getLexeme());
            match("id");
        } else {
            this.compile = this.compile + "Error at line " + this.lexicalAnalyzer.getLine() + ", parenthesis, number or id expected \n";
        }
    }

    private void moreFactors() {
        if (this.token.getName().equals("multiply")) {
            match("multiply");
            factor();
            this.code.add("*");
            moreFactors();
        } else if (this.token.getName().equals("divide")) {
            match("divide");
            factor();
            this.code.add("/");
            moreFactors();
        } else if (this.token.getName().equals("remainder")) {
            match("remainder");
            factor();
            this.code.add("%");
            moreFactors();
        }
    }

}
