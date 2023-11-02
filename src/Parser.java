import jdk.nashorn.internal.ir.FunctionCall;

import java.util.LinkedList;
import java.util.Optional;

public class Parser {

    private TokenManager tokens;

    public Parser(LinkedList<Token> list) {
        tokens = new TokenManager(list);
    }

    private boolean acceptSeperators() {
        if (tokens.moreTokens() && tokens.peek(0).get().type == Token.TokenType.SEPARATOR) {
            while (tokens.moreTokens() && tokens.peek(0).get().type == Token.TokenType.SEPARATOR) {
                tokens.matchAndRemove(Token.TokenType.SEPARATOR);
            }
            return true;
        } else {
            return false;
        }
    }

    public ProgramNode parse() {
        ProgramNode program = new ProgramNode();
        while (tokens.moreTokens()) {
            acceptSeperators();
            if (!parseFunction(program)) {
                if (!parseAction(program)) {
                    throw new RuntimeException("failed to parse function or action");
                }
            }
        }
        return program;
    }

    private boolean parseFunction(ProgramNode program) {
        if (tokens.peek(0).get().type != Token.TokenType.FUNCTION) {
            return false;
        }
        tokens.matchAndRemove(Token.TokenType.FUNCTION);
        if (tokens.peek(0).get().type != Token.TokenType.WORD) {
            throw new RuntimeException("function name is expected after 'function' keyowrd at line " + tokens.peek(0).get().linenum);
        }
        String name = tokens.matchAndRemove(Token.TokenType.WORD).get().value;

        if (tokens.peek(0).get().type != Token.TokenType.OPEN_PAREN) {
            throw new RuntimeException("'(' must follow function declaration at line " + tokens.peek(0).get().linenum);
        }
        tokens.matchAndRemove(Token.TokenType.OPEN_PAREN);

        LinkedList<String> params = new LinkedList<>();
        if (tokens.peek(0).get().type != Token.TokenType.CLOSE_PAREN) {
            if (tokens.peek(0).get().type != Token.TokenType.WORD) {
                throw new RuntimeException("parameter identifier expected after open paren in function declaration at line " + tokens.peek(0).get().linenum);
            }
            params.add(tokens.matchAndRemove(Token.TokenType.WORD).get().value);
            while (tokens.peek(0).get().type != Token.TokenType.CLOSE_PAREN) {
                if (!tokens.matchAndRemove(Token.TokenType.COMMA).isPresent()) {
                    throw new RuntimeException("comma expected after parameter declaration in function definition at line " + tokens.peek(0).get().linenum);
                }
                acceptSeperators();
                if (tokens.peek(0).get().type != Token.TokenType.WORD) {
                    throw new RuntimeException("parameter identifier expected after comma in function declaration at line " + tokens.peek(0).get().linenum);
                }
                params.add(tokens.matchAndRemove(Token.TokenType.WORD).get().value);
            }
        }
        if (!tokens.matchAndRemove(Token.TokenType.CLOSE_PAREN).isPresent()) {
            throw new RuntimeException("no close parentheses found at the end of function declration at lime " + tokens.peek(0).get().linenum);
        }

        program.addFunction(new FunctionNode(name, parseBlock().getStatements(), params));
        return true;
    }

    private boolean parseAction(ProgramNode program) {
        if (tokens.peek(0).isPresent()) {
            if (tokens.matchAndRemove(Token.TokenType.BEGIN).isPresent()) {
                program.addStartBlock(parseBlock());
            } else if (tokens.matchAndRemove(Token.TokenType.END).isPresent()) {
                program.addEndBlock(parseBlock());
            } else {
                Optional<Node> cond = parseOperation();
                BlockNode condBlock = parseBlock();
                condBlock.addCondition(cond);
                program.addBlock(condBlock);
            }
            return true;
        }
        throw new RuntimeException("no tokens left in file when trying to parse action at line");
    }

    private BlockNode parseBlock() {
        BlockNode thisBlock = new BlockNode();
        Optional<StatementNode> currentStatement;
        if (tokens.matchAndRemove(Token.TokenType.OPEN_CURLY).isPresent()) {
            acceptSeperators();
            while(!tokens.matchAndRemove(Token.TokenType.CLOSE_CURLY).isPresent()){
                currentStatement = parseStatement();
                if(currentStatement.isPresent()){
                    thisBlock.addStatement(currentStatement.get());
                }
                else{
                    throw new RuntimeException("failed to parse statement in block");
                }
                acceptSeperators();
            }
            acceptSeperators();
        } else {
            currentStatement = parseStatement();
            if (!currentStatement.isPresent()) {
                throw new RuntimeException("invalid statement found in block");
            }
            acceptSeperators();
            thisBlock.addStatement(currentStatement.get());
        }
        return thisBlock;
    }

    public Optional<StatementNode> parseStatement() {
        if (tokens.matchAndRemove(Token.TokenType.IF).isPresent()) {
            return parseIf();
        } else if (tokens.matchAndRemove(Token.TokenType.WHILE).isPresent()) {
            return parseWhile();
        } else if (tokens.matchAndRemove(Token.TokenType.FOR).isPresent()) {
            return parseFor();
        } else if (tokens.matchAndRemove(Token.TokenType.DO).isPresent()) {
            return parseDoWhile();
        } else if (tokens.matchAndRemove(Token.TokenType.CONTINUE).isPresent()) {
            return Optional.of(new ContinueNode());
        } else if (tokens.matchAndRemove(Token.TokenType.BREAK).isPresent()) {
            return Optional.of(new BreakNode());
        } else if (tokens.matchAndRemove(Token.TokenType.DELETE).isPresent()) {
            return parseDelete();
        } else if (tokens.matchAndRemove(Token.TokenType.RETURN).isPresent()) {
            return parseReturn();
        } else {
            //operations
            Optional<Node> opStatement = parseOperation();
            if (opStatement.isPresent() && opStatement.get() instanceof StatementNode) {
                return Optional.of((StatementNode) opStatement.get());
            } else {
                throw new RuntimeException("failed to parse incoming statement");
            }
        }
    }

    private Optional<StatementNode> parseIf() {
        if (tokens.matchAndRemove(Token.TokenType.OPEN_PAREN).isPresent()) {
            Optional<Node> ifCondition = parseOperation();
            if (!ifCondition.isPresent()) {
                throw new RuntimeException("error parsing if statements condition");
            }
            if (!tokens.matchAndRemove(Token.TokenType.CLOSE_PAREN).isPresent()) {
                throw new RuntimeException("no right parenthesis found in if statement");
            }
            acceptSeperators();
            BlockNode ifBlock = parseBlock();
            acceptSeperators();
            if (tokens.moreTokens() && tokens.peek(0).get().type == Token.TokenType.ELSE) {
                return Optional.of(new IfNode(ifCondition, ifBlock, (IfNode) parseIf().get()));
            }
            return Optional.of(new IfNode(ifCondition, ifBlock));
        } else {
            if (!tokens.matchAndRemove(Token.TokenType.ELSE).isPresent()) {
                throw new RuntimeException("invalid if else chain at end of if statement");
            }
            if (tokens.matchAndRemove(Token.TokenType.IF).isPresent()) {
                return parseIf();
            }
            return Optional.of(new IfNode(Optional.empty(), parseBlock()));
        }
    }

    private Optional<StatementNode> parseWhile() {
        if (tokens.matchAndRemove(Token.TokenType.OPEN_PAREN).isPresent()) {
            Optional<Node> whileCond = parseOperation();
            if (!whileCond.isPresent()) {
                throw new RuntimeException("unable to parse while condition");
            }
            if (!tokens.matchAndRemove(Token.TokenType.CLOSE_PAREN).isPresent()) {
                throw new RuntimeException("no closing parentheses found at while loop");
            }
            acceptSeperators();
            BlockNode whileBlock = parseBlock();
            acceptSeperators();
            return Optional.of(new WhileNode(whileCond, whileBlock));
        } else {
            throw new RuntimeException("no open parenthesis found for condition at while loop");
        }
    }

    private Optional<StatementNode> parseFor() {
        if (!tokens.matchAndRemove(Token.TokenType.OPEN_PAREN).isPresent()) {
            throw new RuntimeException("no open parentheses found at for loop");
        }
        Optional<Node> forExp1 = parseOperation();
        if (!forExp1.isPresent()) {
            throw new RuntimeException("failed to parse expression in for loop condition");
        }
        if (forExp1.get() instanceof OperationNode) {
            if (((OperationNode) forExp1.get()).getOpType() == OperationNode.operationType.IN) {
                if (!tokens.matchAndRemove(Token.TokenType.CLOSE_PAREN).isPresent()) {
                    throw new RuntimeException("no close parentheses found at for each loop expression");
                }
                return Optional.of(new ForEachNode(((OperationNode) forExp1.get()), parseBlock()));
            } else {
                throw new RuntimeException("invalid expression found in for loop");
            }
        } else if (forExp1.get() instanceof AssignmentNode) {
            acceptSeperators();
            //this should only be a bool compare
            Optional<Node> forExp2 = parseOperation();
            if (!forExp2.isPresent()) {
                throw new RuntimeException("failed to parse for loop expression 2");
            }
            acceptSeperators();
            Optional<Node> forExp3 = parseOperation();
            if (!forExp3.isPresent()) {
                throw new RuntimeException("failed to parse for loop expression 3");
            }
            if (!tokens.matchAndRemove(Token.TokenType.CLOSE_PAREN).isPresent()) {
                throw new RuntimeException("no closing parentheses found at for loop");
            }
            BlockNode forBlock = parseBlock();

            return Optional.of(new ForNode(forExp1.get(), forExp2.get(), forExp3.get(), forBlock));
        } else {
            throw new RuntimeException("invalid expression at part 1 of for loop condition");
        }
    }

    private Optional<StatementNode> parseDoWhile() {
        acceptSeperators();
        BlockNode doWhileBlock = parseBlock();
        acceptSeperators();
        if (!tokens.matchAndRemove(Token.TokenType.WHILE).isPresent()) {
            throw new RuntimeException("no while keyword found after closing bracket at do while loop");
        }
        if (!tokens.matchAndRemove(Token.TokenType.OPEN_PAREN).isPresent()) {
            throw new RuntimeException("no open parentheses found after while keyword in do while loop");
        }
        Optional<Node> doWhileExpression = parseOperation();
        if (!tokens.matchAndRemove(Token.TokenType.CLOSE_PAREN).isPresent()) {
            throw new RuntimeException("no close parentehses found at end of do while expression");
        }
        return Optional.of(new DoWhileNode(doWhileExpression, doWhileBlock));
    }

    private Optional<StatementNode> parseDelete() {
        if (tokens.peek(0).get().type == Token.TokenType.WORD) {
            VariableReferenceNode arr = new VariableReferenceNode(tokens.matchAndRemove(Token.TokenType.WORD).get().value,
                    Optional.empty());
            if (tokens.matchAndRemove(Token.TokenType.OPEN_SQAURE).isPresent()) {
                LinkedList<Node> indexes = new LinkedList<>();
                Optional<Node> currentInd;
                do {
                    currentInd = parseOperation();
                    if (!currentInd.isPresent()) {
                        throw new RuntimeException("error parsing index at delete statement");
                    }
                    indexes.add(currentInd.get());
                } while (tokens.matchAndRemove(Token.TokenType.COMMA).isPresent());
                return Optional.of(new DeleteNode(arr, Optional.of(indexes)));
            }
            return Optional.of(new DeleteNode(arr, Optional.empty()));
        } else {
            throw new RuntimeException("no array referenced after delete keyword");
        }
    }

    private Optional<StatementNode> parseReturn() {
        Optional<Node> returnVal = parseOperation();
        if (returnVal.isPresent()) {
            return Optional.of(new ReturnNode(returnVal));
        } else {
            throw new RuntimeException("could not parse return value at return statement");
        }
    }


    public Optional<Node> parseOperation() {
        return parseAssignment();
    }

    private Optional<Node> parseAssignment() {
        Optional<Node> ex1 = parseTernary();
        Optional<Node> ex2;

        if (ex1.isPresent()) {
            if (tokens.matchAndRemove(Token.TokenType.EQUALS).isPresent()) {
                if (acceptSeperators()) {
                    throw new RuntimeException("invalid assignment operation across multiple lines");
                }
                ex2 = parseOperation();
                if (ex2.isPresent()) {
                    ex1 = Optional.of(new AssignmentNode(ex1.get(), ex2));
                } else {
                    throw new RuntimeException("no expression found after = at line " + tokens.peek(0).get().linenum);
                }
            } else if (tokens.matchAndRemove(Token.TokenType.PLUS_EQUALS).isPresent()) {
                if (acceptSeperators()) {
                    throw new RuntimeException("invalid assignment operation across multiple lines");
                }
                ex2 = parseOperation();
                if (ex2.isPresent()) {
                    ex1 = Optional.of(new AssignmentNode(ex1.get(),
                            Optional.of(new OperationNode(ex1.get(), ex2.get(), OperationNode.operationType.ADD))));
                } else {
                    throw new RuntimeException("no expression found after += at line " + tokens.peek(0).get().linenum);
                }
            } else if (tokens.matchAndRemove(Token.TokenType.MINUS_EQUALS).isPresent()) {
                if (acceptSeperators()) {
                    throw new RuntimeException("invalid assignment operation across multiple lines");
                }
                ex2 = parseOperation();
                if (ex2.isPresent()) {
                    ex1 = Optional.of(new AssignmentNode(ex1.get(),
                            Optional.of(new OperationNode(ex1.get(), ex2.get(), OperationNode.operationType.SUBTRACT))));
                } else {
                    throw new RuntimeException("no expression found after -= at line " + tokens.peek(0).get().linenum);
                }
            } else if (tokens.matchAndRemove(Token.TokenType.SLASH_EQUALS).isPresent()) {
                if (acceptSeperators()) {
                    throw new RuntimeException("invalid assignment operation across multiple lines");
                }
                ex2 = parseOperation();
                if (ex2.isPresent()) {
                    ex1 = Optional.of(new AssignmentNode(ex1.get(),
                            Optional.of(new OperationNode(ex1.get(), ex2.get(), OperationNode.operationType.DIVIDE))));
                } else {
                    throw new RuntimeException("no expression found after /= at line " + tokens.peek(0).get().linenum);
                }
            } else if (tokens.matchAndRemove(Token.TokenType.STAR_EQUALS).isPresent()) {
                if (acceptSeperators()) {
                    throw new RuntimeException("invalid assignment operation across multiple lines");
                }
                ex2 = parseOperation();
                if (ex2.isPresent()) {
                    ex1 = Optional.of(new AssignmentNode(ex1.get(),
                            Optional.of(new OperationNode(ex1.get(), ex2.get(), OperationNode.operationType.MULTIPLY))));
                } else {
                    throw new RuntimeException("no expression found after *= at line " + tokens.peek(0).get().linenum);
                }
            } else if (tokens.matchAndRemove(Token.TokenType.PERCENT_EQUALS).isPresent()) {
                if (acceptSeperators()) {
                    throw new RuntimeException("invalid assignment operation across multiple lines");
                }
                ex2 = parseOperation();
                if (ex2.isPresent()) {
                    ex1 = Optional.of(new AssignmentNode(ex1.get(),
                            Optional.of(new OperationNode(ex1.get(), ex2.get(), OperationNode.operationType.MODULO))));
                } else {
                    throw new RuntimeException("no expression found after %= at line " + tokens.peek(0).get().linenum);
                }
            } else if (tokens.matchAndRemove(Token.TokenType.CARROT_EQUALS).isPresent()) {
                if (acceptSeperators()) {
                    throw new RuntimeException("invalid assignment operation across multiple lines");
                }
                ex2 = parseOperation();
                if (ex2.isPresent()) {
                    ex1 = Optional.of(new AssignmentNode(ex1.get(),
                            Optional.of(new OperationNode(ex1.get(), ex2.get(), OperationNode.operationType.EXPONENT))));
                } else {
                    throw new RuntimeException("no expression found after ^= at line " + tokens.peek(0).get().linenum);
                }
            }
        }
        return ex1;
    }

    private Optional<Node> parseTernary() {
        Optional<Node> ex1 = parseAnd();
        Optional<Node> truthexp, falseexp;
        if (ex1.isPresent()) {
            if (tokens.matchAndRemove(Token.TokenType.QUESTION).isPresent()) {
                acceptSeperators();
                truthexp = parseOperation();
                if (!truthexp.isPresent()) {
                    throw new RuntimeException("invalid expression found at line " + tokens.peek(0).get().linenum);
                }
                if (!tokens.matchAndRemove(Token.TokenType.COLON).isPresent()) {
                    throw new RuntimeException("no ':' found in ternary expression at line " + tokens.peek(0).get().linenum);
                }
                falseexp = parseOperation();
                if (!falseexp.isPresent()) {
                    throw new RuntimeException("invalid expression found at line " + tokens.peek(0).get().linenum);
                }
                ex1 = Optional.of(new TernaryNode(ex1.get(), truthexp.get(), falseexp.get()));
            }
        } else {
            throw new RuntimeException("invalid expression found at line " + tokens.peek(0).get().linenum);
        }

        return ex1;
    }

    private Optional<Node> parseAnd() {
        Optional<Node> ex1 = parseOr();
        Optional<Node> ex2;
        if (ex1.isPresent()) {
            if (tokens.matchAndRemove(Token.TokenType.AND_OP).isPresent()) {
                acceptSeperators();
                ex2 = parseBoolean();
                if (!ex2.isPresent()) {
                    throw new RuntimeException("invalid expression found at line " + tokens.peek(0).get().linenum);
                }
                ex1 = Optional.of(new OperationNode(ex1.get(), ex2.get(), OperationNode.operationType.AND));
            }
        } else {
            throw new RuntimeException("invalid expression found at line " + tokens.peek(0).get().linenum);
        }
        return ex1;
    }

    private Optional<Node> parseOr() {
        Optional<Node> ex1 = parseArrayMembership();
        Optional<Node> ex2;
        if (ex1.isPresent()) {
            if (tokens.matchAndRemove(Token.TokenType.OR_OP).isPresent()) {
                acceptSeperators();
                ex2 = parseBoolean();
                if (!ex2.isPresent()) {
                    throw new RuntimeException("invaild expression found at line " + tokens.peek(0).get().linenum);
                }
                ex1 = Optional.of(new OperationNode(ex1.get(), ex2.get(), OperationNode.operationType.OR));
            }
        } else {
            throw new RuntimeException("invalid expression at line " + tokens.peek(0).get().linenum);
        }
        return ex1;
    }

    private Optional<Node> parseArrayMembership() {
        Optional<Node> ex1 = parseMatch();
        if (ex1.isPresent()) {
            if (tokens.matchAndRemove(Token.TokenType.IN).isPresent()) {
                if (acceptSeperators()) {
                    throw new RuntimeException("invalid array membership operation across multiple lines");
                }
                Optional<Node> arrayVar = parseLValue();
                if (arrayVar.isPresent()) {
                    ex1 = Optional.of(new OperationNode(ex1.get(), arrayVar.get(), OperationNode.operationType.IN));
                } else {
                    throw new RuntimeException("no array variable found on right side of array membership expression");
                }

            }
        } else {
            throw new RuntimeException("invalid expression found at line " + tokens.peek(0).get().linenum);
        }
        return ex1;
    }

    private Optional<Node> parseMatch() {
        Optional<Node> ex1 = parseBoolean();
        Optional<Node> ex2;
        if (ex1.isPresent()) {
            if (tokens.matchAndRemove(Token.TokenType.MATCH).isPresent()) {
                if (acceptSeperators()) {
                    throw new RuntimeException("invalid match operation across multiple lines");
                }
                ex2 = parseOperation();
                if (!ex2.isPresent()) {
                    throw new RuntimeException("invalid expression found on right side of ERE match operation");
                }
                ex1 = Optional.of(new OperationNode(ex1.get(), ex2.get(), OperationNode.operationType.MATCH));
            } else if (tokens.matchAndRemove(Token.TokenType.NO_MATCH).isPresent()) {
                if (acceptSeperators()) {
                    throw new RuntimeException("invalid match operation across multiple lines");
                }
                ex2 = parseOperation();
                if (!ex2.isPresent()) {
                    throw new RuntimeException("invalid expression found on right side of ERE nonmatch operation");
                }
                ex1 = Optional.of(new OperationNode(ex1.get(), ex2.get(), OperationNode.operationType.NOTMATCH));
            }
        } else {
            throw new RuntimeException("invalid expression found at line " + tokens.peek(0).get().linenum);
        }
        return ex1;
    }

    private Optional<Node> parseBoolean() {
        Optional<Node> ex1 = parseConcatination();
        Optional<Node> ex2;
        if (!ex1.isPresent()) {
            throw new RuntimeException("no valid expression found at line " + tokens.peek(0).get().linenum);
        }

        if (tokens.matchAndRemove(Token.TokenType.LESSTHAN).isPresent()) {
            if (acceptSeperators()) {
                throw new RuntimeException("invalid boolean operation across multiple lines");
            }
            ex2 = parseConcatination();
            if (ex2.isPresent()) {
                ex1 = Optional.of(new OperationNode(ex1.get(), ex2.get(), OperationNode.operationType.LT));
            } else {
                throw new RuntimeException("no/invalid expression found after less than sign at line " + tokens.peek(0).get().linenum);
            }
        } else if (tokens.matchAndRemove(Token.TokenType.LESS_THAN_EQUALTO).isPresent()) {
            if (acceptSeperators()) {
                throw new RuntimeException("invalid boolean operation across multiple lines");
            }
            ex2 = parseConcatination();
            if (ex2.isPresent()) {
                ex1 = Optional.of(new OperationNode(ex1.get(), ex2.get(), OperationNode.operationType.LE));
            } else {
                throw new RuntimeException("no/invalid expression found after less than or equal to sign at line " + tokens.peek(0).get().linenum);
            }
        } else if (tokens.matchAndRemove(Token.TokenType.NOT_EQUALS).isPresent()) {
            if (acceptSeperators()) {
                throw new RuntimeException("invalid boolean operation across multiple lines");
            }
            ex2 = parseConcatination();
            if (ex2.isPresent()) {
                ex1 = Optional.of(new OperationNode(ex1.get(), ex2.get(), OperationNode.operationType.NE));
            } else {
                throw new RuntimeException("no/invalid expression found after not equals sign at line " + tokens.peek(0).get().linenum);
            }
        } else if (tokens.matchAndRemove(Token.TokenType.DOUBLE_EQUALS).isPresent()) {
            if (acceptSeperators()) {
                throw new RuntimeException("invalid boolean operation across multiple lines");
            }
            ex2 = parseConcatination();
            if (ex2.isPresent()) {
                ex1 = Optional.of(new OperationNode(ex1.get(), ex2.get(), OperationNode.operationType.EQ));
            } else {
                throw new RuntimeException("no/invalid expression found after double equals at line " + tokens.peek(0).get().linenum);
            }
        } else if (tokens.matchAndRemove(Token.TokenType.GREATERTHAN).isPresent()) {
            if (acceptSeperators()) {
                throw new RuntimeException("invalid boolean operation across multiple lines");
            }
            ex2 = parseConcatination();
            if (ex2.isPresent()) {
                ex1 = Optional.of(new OperationNode(ex1.get(), ex2.get(), OperationNode.operationType.GT));
            } else {
                throw new RuntimeException("no/invalid expression found after greater than at line " + tokens.peek(0).get().linenum);
            }
        } else if (tokens.matchAndRemove(Token.TokenType.GREATER_THAN_EQUALTO).isPresent()) {
            if (acceptSeperators()) {
                throw new RuntimeException("invalid boolean operation across multiple lines");
            }
            ex2 = parseConcatination();
            if (ex2.isPresent()) {
                ex1 = Optional.of(new OperationNode(ex1.get(), ex2.get(), OperationNode.operationType.GE));
            } else {
                throw new RuntimeException("no/invalid expression found after greater than or equal to at line " + tokens.peek(0).get().linenum);
            }
        }
        return ex1;
    }

    private Optional<Node> parseConcatination() {
        Optional<Node> ex1 = expression();
        Optional<Node> ex2;
        if (ex1.isPresent()) {
            if (ex1.get() instanceof ConstantNode && (tokens.moreTokens() && tokens.peek(0).get().type == Token.TokenType.STRINGLITERAL)) {
                ex1 = Optional.of(new OperationNode(ex1.get(), parseBottomLevel().get(), OperationNode.operationType.CONCATENATION));
            }
        }
        return ex1;
    }

    private Optional<Node> expression() {
        Optional<Node> term1 = term();
        Optional<Node> term2;

        while (tokens.moreTokens() && (tokens.peek(0).get().type == Token.TokenType.PLUS || tokens.peek(0).get().type == Token.TokenType.MINUS)) {
            if (tokens.matchAndRemove(Token.TokenType.PLUS).isPresent()) {
                if (acceptSeperators()) {
                    throw new RuntimeException("invalid mathmatical operation across multiple lines");
                }
                term2 = term();
                if (!term2.isPresent()) {
                    throw new RuntimeException("invalid expression at line " + tokens.peek(0).get().linenum);
                }
                term1 = Optional.of(new OperationNode(term1.get(), term2.get(), OperationNode.operationType.ADD));
            } else {
                tokens.matchAndRemove(Token.TokenType.MINUS);
                if (acceptSeperators()) {
                    throw new RuntimeException("invalid mathmatical operation across multiple lines");
                }
                term2 = term();
                if (!term2.isPresent()) {
                    throw new RuntimeException("invalid expression at line " + tokens.peek(0).get().linenum);
                }
                term1 = Optional.of(new OperationNode(term1.get(), term2.get(), OperationNode.operationType.SUBTRACT));
            }
        }
        return term1;
    }

    private Optional<Node> term() {
        Optional<Node> factor1 = factor();
        Optional<Node> factor2;
        while (tokens.moreTokens() && (tokens.peek(0).get().type == Token.TokenType.STAR ||
                tokens.peek(0).get().type == Token.TokenType.SLASH ||
                tokens.peek(0).get().type == Token.TokenType.PERCENT)) {

            if (tokens.matchAndRemove(Token.TokenType.STAR).isPresent()) {
                if (acceptSeperators()) {
                    throw new RuntimeException("invalid mathmatical operation across multiple lines");
                }
                factor2 = factor();
                if (!factor2.isPresent()) {
                    throw new RuntimeException("invalid expression found at line " + tokens.peek(0).get().linenum);
                }
                factor1 = Optional.of(new OperationNode(factor1.get(), factor2.get(), OperationNode.operationType.MULTIPLY));
            } else if (tokens.matchAndRemove(Token.TokenType.SLASH).isPresent()) {
                if (acceptSeperators()) {
                    throw new RuntimeException("invalid mathmatical operation across multiple lines");
                }
                factor2 = factor();
                if (!factor2.isPresent()) {
                    throw new RuntimeException("invalid expression found at line " + tokens.peek(0).get().linenum);
                }
                factor1 = Optional.of(new OperationNode(factor1.get(), factor2.get(), OperationNode.operationType.DIVIDE));
            } else {
                tokens.matchAndRemove(Token.TokenType.PERCENT);
                if (acceptSeperators()) {
                    throw new RuntimeException("invalid mathmatical operation across multiple lines");
                }
                factor2 = factor();
                if (!factor2.isPresent()) {
                    throw new RuntimeException("invalid expression at line " + tokens.peek(0).get().linenum);
                }
                factor1 = Optional.of(new OperationNode(factor1.get(), factor2.get(), OperationNode.operationType.MODULO));
            }
        }
        return factor1;
    }

    private Optional<Node> factor() {
        return parseExponents();
    }

    private Optional<Node> parseExponents() {
        Optional<Node> ex1 = parsePostIncDec();
        Optional<Node> ex2;
        if (ex1.isPresent()) {
            if (tokens.matchAndRemove(Token.TokenType.CARROT).isPresent()) {
                if (acceptSeperators()) {
                    throw new RuntimeException("invalid mathmatical operation across multiple lines");
                }
                if (ex1.get() instanceof OperationNode && ((OperationNode) ex1.get()).getOpType() == OperationNode.operationType.NOT) {
                    throw new RuntimeException("cannot perform exponent operation on negated expression");
                }
                ex2 = expression();
                if (!ex2.isPresent()) {
                    throw new RuntimeException("invalid expression at line " + tokens.peek(0).get().linenum);
                }
                ex1 = Optional.of(new OperationNode(ex1.get(), ex2.get(), OperationNode.operationType.EXPONENT));
            }

        } else {
            throw new RuntimeException("invalid expression at line " + tokens.peek(0).get().linenum);
        }
        return ex1;
    }

    private Optional<Node> parsePostIncDec() {
        Optional<Node> ex1 = parseBottomLevel();

        if (ex1.isPresent()) {
            if (ex1.get() instanceof VariableReferenceNode) {
                if (tokens.matchAndRemove(Token.TokenType.INCREMENT).isPresent()) {
                    ex1 = Optional.of(new OperationNode(ex1.get(), OperationNode.operationType.POSTINC));
                } else if (tokens.matchAndRemove(Token.TokenType.DECREMENT).isPresent()) {
                    ex1 = Optional.of(new OperationNode(ex1.get(), OperationNode.operationType.POSTDEC));
                }
            }
        }
        return ex1;
    }

    private Optional<Node> parseBottomLevel() {
        Optional<Node> result;
        switch (tokens.peek(0).get().type) {
            case STRINGLITERAL:
            case NUMBER:
                return Optional.of(new ConstantNode(tokens.matchAndRemove(tokens.peek(0).get().type).get().value));
            case PATTERN:
                return Optional.of(new PatternNode(tokens.matchAndRemove(Token.TokenType.PATTERN).get().value));
            case OPEN_PAREN:
                tokens.matchAndRemove(Token.TokenType.OPEN_PAREN);
                if (acceptSeperators()) {
                    throw new RuntimeException("invalid () across multiple lines");
                }
                result = parseOperation();
                if (tokens.peek(0).get().type != Token.TokenType.CLOSE_PAREN) {
                    throw new RuntimeException("no close parenthes found at expression at line " + tokens.peek(0).get().linenum);
                }
                tokens.matchAndRemove(Token.TokenType.CLOSE_PAREN);
                return result;
            case NOT:
                tokens.matchAndRemove(Token.TokenType.NOT);
                if (acceptSeperators()) {
                    throw new RuntimeException("invalid negated operation across multiple lines");
                }
                result = parseOperation();
                if (result.isPresent()) {
                    return Optional.of(new OperationNode(result.get(), OperationNode.operationType.NOT));
                }
                throw new RuntimeException("could not parse not expression at line " + tokens.peek(0).get().linenum);
            case MINUS:
                tokens.matchAndRemove(Token.TokenType.MINUS);
                if (acceptSeperators()) {
                    throw new RuntimeException("invalid negative expression across multiple lines");
                }
                result = parseOperation();
                if (result.isPresent()) {
                    return Optional.of(new OperationNode(result.get(), OperationNode.operationType.UNARYNEG));
                }
                throw new RuntimeException("could not parse unary negation expression at line " + tokens.peek(0).get().linenum);
            case PLUS:
                tokens.matchAndRemove(Token.TokenType.PLUS);
                if (acceptSeperators()) {
                    throw new RuntimeException("invalid positive expression across multiple lines");
                }
                result = parseOperation();
                if (result.isPresent()) {
                    return Optional.of(new OperationNode(result.get(), OperationNode.operationType.UNARYPOS));
                }
                throw new RuntimeException("could not parse Unary addition expression at line " + tokens.peek(0).get().linenum);
            case INCREMENT:
                tokens.matchAndRemove(Token.TokenType.INCREMENT);
                if (acceptSeperators()) {
                    throw new RuntimeException("invalid preincrement operation across multiple lines");
                }
                result = parseOperation();
                if (result.isPresent()) {
                    return Optional.of(new OperationNode(result.get(), OperationNode.operationType.PREINC));
                }
                throw new RuntimeException("could not parse preincrement expression at line " + tokens.peek(0).get().linenum);
            case DECREMENT:
                tokens.matchAndRemove(Token.TokenType.DECREMENT);
                if (acceptSeperators()) {
                    throw new RuntimeException("invalid postincrement operation across multiple lines");
                }
                result = parseOperation();
                if (result.isPresent()) {
                    return Optional.of(new OperationNode(result.get(), OperationNode.operationType.PREDEC));
                }
                throw new RuntimeException("could not parse predecrement expression at line " + tokens.peek(0).get().linenum);
            default:
                return parseFunctionCall();
        }

    }

    private Optional<Node> parseFunctionCall() {
        if(tokens.peek(0).get().type == Token.TokenType.WORD){
            Optional<Node> funcName = parseLValue();
            if(!funcName.isPresent()){
                throw new RuntimeException("unable to parse word at bottom level");
            }
            if(tokens.matchAndRemove(Token.TokenType.OPEN_PAREN).isPresent()){
                Optional<Node> currentParam;
                LinkedList<Node> params = new LinkedList<>();

                while (!tokens.matchAndRemove(Token.TokenType.CLOSE_PAREN).isPresent()) {
                    currentParam = parseOperation();
                    if (!currentParam.isPresent()) {
                        throw new RuntimeException("failed to parse parameter being passed into function call");
                    }
                    params.add(currentParam.get());
                    tokens.matchAndRemove(Token.TokenType.COMMA);
                }
                return Optional.of(new FunctionCallNode(funcName.get(), params));
            }
            return funcName;
        }
        else if(tokens.peek(0).get().type == Token.TokenType.DOLLAR){
            return parseLValue();
        }
        else{
            String builtinName;
            if(tokens.matchAndRemove(Token.TokenType.GETLINE).isPresent()){
                builtinName = "getline";
            }
            else if(tokens.matchAndRemove(Token.TokenType.PRINT).isPresent()){
                builtinName = "print";
            }
            else if(tokens.matchAndRemove(Token.TokenType.PRINTF).isPresent()){
                builtinName = "printf";
            }
            else if(tokens.matchAndRemove(Token.TokenType.EXIT).isPresent()){
                builtinName = "exit";
            }
            else if(tokens.matchAndRemove(Token.TokenType.NEXTFILE).isPresent()){
                builtinName = "nextfile";
            }
            else if(tokens.matchAndRemove(Token.TokenType.NEXT).isPresent()){
                builtinName = "next";
            }
            else{
                throw new RuntimeException("method call failed to parse");
            }
            LinkedList<Node> params = new LinkedList<>();
            Optional<Node> currentParam;
            if(tokens.moreTokens()) {
                while (tokens.peek(0).get().type != Token.TokenType.SEPARATOR){
                    currentParam = parseOperation();
                    if(!currentParam.isPresent()){
                        throw new RuntimeException("failed to parse parameter at " + builtinName + " call");
                    }
                    params.add(currentParam.get());
                    tokens.matchAndRemove(Token.TokenType.COMMA);
                }
                acceptSeperators();
            }
            return Optional.of(new FunctionCallNode(new VariableReferenceNode(builtinName, Optional.empty()), params));
        }
    }

    private Optional<Node> parseLValue() {
        if (tokens.matchAndRemove(Token.TokenType.DOLLAR).isPresent()) {
            if (acceptSeperators()) {
                throw new RuntimeException("invalid operation across multiple lines");
            }
            return Optional.of(new OperationNode(parseBottomLevel().get(), OperationNode.operationType.DOLLAR));
        } else if (tokens.peek(0).get().type == Token.TokenType.WORD) {
            String varName = tokens.matchAndRemove(Token.TokenType.WORD).get().value;
            if (tokens.matchAndRemove(Token.TokenType.OPEN_SQAURE).isPresent()) {
                if (acceptSeperators()) {
                    throw new RuntimeException("invalid array index across multiple lines");
                }
                Optional<Node> expression = parseOperation();
                if (!tokens.matchAndRemove(Token.TokenType.CLOSE_SQUARE).isPresent()) {
                    throw new RuntimeException("failed to close array index brackets at line " + tokens.peek(0).get().linenum);
                }
                acceptSeperators();
                return Optional.of(new VariableReferenceNode(varName, expression));
            } else {
                return Optional.of(new VariableReferenceNode(varName, Optional.empty()));
            }
        } else {
            throw new RuntimeException("$ or word not found when attempting to parse Lvalue at line " + tokens.peek(0).get().linenum);
        }
    }
}