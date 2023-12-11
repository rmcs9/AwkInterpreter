package Parser;

import java.util.LinkedList;
import java.util.Optional;

import Lexer.*;

public class Parser {

    private TokenManager tokens;

    public Parser(LinkedList<Token> list) {
        tokens = new TokenManager(list);
    }

    /**
     * when called, acceptSeparators will attempt to match and remove a single separator token from
     * the top of the tokens list. if a separator is found, acceptSeparators will then attempt to continue
     * matching separator tokens until there are none left
     * @return true if any separators were removed, false if no separators were removed
     */
    private boolean acceptSeparators() {
        if (tokens.moreTokens() && tokens.peek(0).get().type == Token.TokenType.SEPARATOR) {
            while (tokens.moreTokens() && tokens.peek(0).get().type == Token.TokenType.SEPARATOR) {
                tokens.matchAndRemove(Token.TokenType.SEPARATOR);
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * entry method for the parser. a new programNode is created.
     * parseFunction and parseAction are then called repeatedly until there are no more tokens left to read
     * @return the program node representing the users program
     */
    public ProgramNode parse() {
        ProgramNode program = new ProgramNode();
        while (tokens.moreTokens()) {
            acceptSeparators();
            if (!parseFunction(program)) {
                if (!parseAction(program)) {
                    throw new RuntimeException("failed to parse function or action");
                }
            }
        }
        return program;
    }

    /**
     * parses a single user defined function block in an awk program
     * an example user defined awk function:
     *
     * function add(x, y) {
     *     return x + y;
     * }
     *
     * @param program the current programs program node
     * @return true if a function was parsed, false if otherwise
     */
    private boolean parseFunction(ProgramNode program) {
        //look for function keyword, if not found return false
        if (tokens.peek(0).get().type != Token.TokenType.FUNCTION) {
            return false;
        }
        tokens.matchAndRemove(Token.TokenType.FUNCTION);

        //attempt to match the function name
        if (tokens.peek(0).get().type != Token.TokenType.WORD) {
            throw new RuntimeException("function name is expected after 'function' keyowrd at line " + tokens.peek(0).get().linenum);
        }
        String name = tokens.matchAndRemove(Token.TokenType.WORD).get().value;

        //attempt to match the opening parameters parentheses
        if (tokens.peek(0).get().type != Token.TokenType.OPEN_PAREN) {
            throw new RuntimeException("'(' must follow function declaration at line " + tokens.peek(0).get().linenum);
        }
        tokens.matchAndRemove(Token.TokenType.OPEN_PAREN);

        LinkedList<String> params = new LinkedList<>();
        //if there are parameters present...
        if (tokens.peek(0).get().type != Token.TokenType.CLOSE_PAREN) {
            if (tokens.peek(0).get().type != Token.TokenType.WORD) {
                throw new RuntimeException("parameter identifier expected after open paren in function declaration at line " + tokens.peek(0).get().linenum);
            }
            //add the first parameter
            params.add(tokens.matchAndRemove(Token.TokenType.WORD).get().value);
            //loop until there are no parameters left
            while (tokens.peek(0).get().type != Token.TokenType.CLOSE_PAREN) {
                if (!tokens.matchAndRemove(Token.TokenType.COMMA).isPresent()) {
                    throw new RuntimeException("comma expected after parameter declaration in function definition at line " + tokens.peek(0).get().linenum);
                }
                acceptSeparators();
                if (tokens.peek(0).get().type != Token.TokenType.WORD) {
                    throw new RuntimeException("parameter identifier expected after comma in function declaration at line " + tokens.peek(0).get().linenum);
                }
                params.add(tokens.matchAndRemove(Token.TokenType.WORD).get().value);
            }
        }
        //remove the closing parameters parentheses
        if (!tokens.matchAndRemove(Token.TokenType.CLOSE_PAREN).isPresent()) {
            throw new RuntimeException("no close parentheses found at the end of function declration at lime " + tokens.peek(0).get().linenum);
        }

        //add the new function node to the program node, containing all gathered data (name, block statements, parameters)
        program.addFunction(new FunctionNode(name, parseBlock().getStatements(), params));
        return true;
    }

    /**
     * parses a single awk action block
     * block types include:
     * BEGIN{statements here}
     * END{statements here}
     * (CONDITION){statements here
     * {statements here}
     * @param program the current programs program node
     * @return true if an action was parsed, false if otherwise
     */
    private boolean parseAction(ProgramNode program) {
        if (tokens.peek(0).isPresent()) {
            //attempt to match BEGIN block
            if (tokens.matchAndRemove(Token.TokenType.BEGIN).isPresent()) {
                program.addStartBlock(parseBlock());
            }
            //attempt to match an END block
            else if (tokens.matchAndRemove(Token.TokenType.END).isPresent()) {
                program.addEndBlock(parseBlock());
            }
            //attempt to match a condition or blank block
            else {
                if(tokens.peek(0).get().type != Token.TokenType.OPEN_PAREN && tokens.peek(0).get().type != Token.TokenType.OPEN_CURLY){
                    throw new RuntimeException("invalid block found. block must be of type BEGIN, END, (CONDITION) or non condition");
                }
                Optional<Node> cond = Optional.empty();
                if(tokens.peek(0).get().type == Token.TokenType.OPEN_PAREN) {
                    cond = parseOperation();
                }
                BlockNode condBlock = parseBlock();
                if(cond.isPresent()){
                    condBlock.addCondition(cond);
                }
                program.addBlock(condBlock);
            }
            return true;
        }
        throw new RuntimeException("no tokens left in file when trying to parse action at line");
    }

    /**
     * parses a single awk block. a block is defined as anything within {}
     * @return a BlockNode containing a list of all the blocks statements
     */
    private BlockNode parseBlock() {
        BlockNode thisBlock = new BlockNode();
        Optional<StatementNode> currentStatement;
        //remove the opening bracket
        if (tokens.matchAndRemove(Token.TokenType.OPEN_CURLY).isPresent()) {
            //absorb empty lines
            acceptSeparators();
            //while we are still inside the block, parse blocks statements repeatedly
            while(!tokens.matchAndRemove(Token.TokenType.CLOSE_CURLY).isPresent()){
                currentStatement = parseStatement();
                if(currentStatement.isPresent()){
                    thisBlock.addStatement(currentStatement.get());
                }
                else{
                    throw new RuntimeException("failed to parse statement in block");
                }
                acceptSeparators();
            }
            acceptSeparators();
        }
        //in the case that no brackets are present, we assume that there is only one statement to parse
        else {
            currentStatement = parseStatement();
            if (!currentStatement.isPresent()) {
                throw new RuntimeException("invalid statement found in block");
            }
            acceptSeparators();
            thisBlock.addStatement(currentStatement.get());
        }
        return thisBlock;
    }

    private Optional<StatementNode> parseStatement() {
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
            //operations that can be considered statements such as, increments and decrements
            Optional<Node> opStatement = parseOperation();
            if (opStatement.isPresent() && opStatement.get() instanceof StatementNode) {
                return Optional.of((StatementNode) opStatement.get());
            } else {
                throw new RuntimeException("failed to parse incoming statement");
            }
        }
    }

    /**
     * parses an if block as well as its optional chained components (else if, else)
     * @return the complete IfNode
     */
    private Optional<StatementNode> parseIf() {
        //if we can match an open paren, we can assume that we are currently parsing an if block
        if (tokens.matchAndRemove(Token.TokenType.OPEN_PAREN).isPresent()) {
            //parse the condition
            Optional<Node> ifCondition = parseOperation();
            if (!ifCondition.isPresent()) {
                throw new RuntimeException("error parsing if statements condition");
            }
            //remove the closing paren
            if (!tokens.matchAndRemove(Token.TokenType.CLOSE_PAREN).isPresent()) {
                throw new RuntimeException("no right parenthesis found in if statement");
            }
            acceptSeparators();
            //parse the if/else if statements
            BlockNode ifBlock = parseBlock();
            acceptSeparators();
            //if there are more elses in the chain, return a new ifNode with a recursive call to parseIf for the next else in the chain
            if (tokens.moreTokens() && tokens.peek(0).get().type == Token.TokenType.ELSE) {
                return Optional.of(new IfNode(ifCondition, ifBlock, (IfNode) parseIf().get()));
            }
            return Optional.of(new IfNode(ifCondition, ifBlock));
        }
        //if there is no open paren, we can assume we are parsing an else if or else block
        else {
            if (!tokens.matchAndRemove(Token.TokenType.ELSE).isPresent()) {
                throw new RuntimeException("invalid if else chain at end of if statement");
            }
            //else if case. call parseIf again
            if (tokens.matchAndRemove(Token.TokenType.IF).isPresent()) {
                return parseIf();
            }
            return Optional.of(new IfNode(Optional.empty(), parseBlock()));
        }
    }

    /**
     * parses a while block
     * @return a while node, containing the whiles condition and statements
     */
    private Optional<StatementNode> parseWhile() {
        //remove the opening parentheses
        if (tokens.matchAndRemove(Token.TokenType.OPEN_PAREN).isPresent()) {
            //parse the while condition
            Optional<Node> whileCond = parseOperation();
            if (!whileCond.isPresent()) {
                throw new RuntimeException("unable to parse while condition");
            }
            //remove the closing parentheses
            if (!tokens.matchAndRemove(Token.TokenType.CLOSE_PAREN).isPresent()) {
                throw new RuntimeException("no closing parentheses found at while loop");
            }
            acceptSeparators();
            //parse the while blocks statements
            BlockNode whileBlock = parseBlock();
            acceptSeparators();
            //return completed WhileNode
            return Optional.of(new WhileNode(whileCond, whileBlock));
        } else {
            throw new RuntimeException("no open parenthesis found for condition at while loop");
        }
    }

    /**
     * parses a for block. can either be a standard for loop:
     *
     * for(int i = 0; i < j; i++)
     *
     * OR a foreach block:
     *
     * for(i in array)
     * @return either a ForNode (standard for loop) or a ForEachNode depending on the circumstances
     */
    private Optional<StatementNode> parseFor() {
        //remove the open parentheses
        if (!tokens.matchAndRemove(Token.TokenType.OPEN_PAREN).isPresent()) {
            throw new RuntimeException("no open parentheses found at for loop");
        }
        //parse the initial operation in the parentheses
        Optional<Node> forExp1 = parseOperation();
        if (!forExp1.isPresent()) {
            throw new RuntimeException("failed to parse expression in for loop condition");
        }
        //if the parsed expression is an operation, we can assume the case of a foreach loop
        if (forExp1.get() instanceof OperationNode) {
            //ensure that the parsed operation is a (var in array) expression
            if (((OperationNode) forExp1.get()).getOpType() == OperationNode.operationType.IN) {
                //remove the closing parentheses
                if (!tokens.matchAndRemove(Token.TokenType.CLOSE_PAREN).isPresent()) {
                    throw new RuntimeException("no close parentheses found at for each loop expression");
                }
                //return completed ForEachNode
                return Optional.of(new ForEachNode(((OperationNode) forExp1.get()), parseBlock()));
            } else {
                throw new RuntimeException("invalid expression found in for loop");
            }
        }
        //if the parsed operation is an asignment, we can assume that we are dealing with a standard for loop
        else if (forExp1.get() instanceof AssignmentNode) {
            acceptSeparators();
            //parse the comparison component
            Optional<Node> forExp2 = parseOperation();
            if (!forExp2.isPresent()) {
                throw new RuntimeException("failed to parse for loop expression 2");
            }
            acceptSeparators();
            //parse the action component
            Optional<Node> forExp3 = parseOperation();
            if (!forExp3.isPresent()) {
                throw new RuntimeException("failed to parse for loop expression 3");
            }
            if (!tokens.matchAndRemove(Token.TokenType.CLOSE_PAREN).isPresent()) {
                throw new RuntimeException("no closing parentheses found at for loop");
            }
            //return complete ForNode
            return Optional.of(new ForNode(forExp1.get(), forExp2.get(), forExp3.get(), parseBlock()));
        } else {
            throw new RuntimeException("invalid expression at part 1 of for loop condition");
        }
    }

    /**
     * parses a do while loop
     * @return a complete DoWhileNode
     */
    private Optional<StatementNode> parseDoWhile() {
        acceptSeparators();
        //parse the do whiles statements
        BlockNode doWhileBlock = parseBlock();
        acceptSeparators();
        //expect a while after the closing bracket
        if (!tokens.matchAndRemove(Token.TokenType.WHILE).isPresent()) {
            throw new RuntimeException("no while keyword found after closing bracket at do while loop");
        }
        //remove the whiles opening paren
        if (!tokens.matchAndRemove(Token.TokenType.OPEN_PAREN).isPresent()) {
            throw new RuntimeException("no open parentheses found after while keyword in do while loop");
        }
        //parse the whiles condition
        Optional<Node> doWhileExpression = parseOperation();
        //remove the closing paren
        if (!tokens.matchAndRemove(Token.TokenType.CLOSE_PAREN).isPresent()) {
            throw new RuntimeException("no close parentehses found at end of do while expression");
        }
        //return completed DoWhileNode
        return Optional.of(new DoWhileNode(doWhileExpression, doWhileBlock));
    }

    /**
     * parses a delete statement
     * @return a complete DeleteNode
     */
    private Optional<StatementNode> parseDelete() {
        if (tokens.peek(0).get().type == Token.TokenType.WORD) {
            //create a new VariableReferenceNode for the array that is to be deleted
            VariableReferenceNode arr = new VariableReferenceNode(tokens.matchAndRemove(Token.TokenType.WORD).get().value,
                    Optional.empty());
            //in the case that a '[' is present, the user is expected to reference specific array indexes to be deleted
            if (tokens.matchAndRemove(Token.TokenType.OPEN_SQAURE).isPresent()) {
                LinkedList<Node> indexes = new LinkedList<>();
                Optional<Node> currentInd;
                //add the deleted indexes to a list repeatedly
                do {
                    currentInd = parseOperation();
                    if (!currentInd.isPresent()) {
                        throw new RuntimeException("error parsing index at delete statement");
                    }
                    indexes.add(currentInd.get());
                } while (tokens.matchAndRemove(Token.TokenType.COMMA).isPresent());
                //return the complete DeleteNode with the array reference and the list of indexes
                return Optional.of(new DeleteNode(arr, Optional.of(indexes)));
            }
            //in the case of no specified indexes, return a DeleteNode with just the array reference
            return Optional.of(new DeleteNode(arr, Optional.empty()));
        } else {
            throw new RuntimeException("no array referenced after delete keyword");
        }
    }

    /**
     * parses a single return statement
     * @return a complete ReturnNode with the specified return value
     */
    private Optional<StatementNode> parseReturn() {
        Optional<Node> returnVal = parseOperation();
        if (returnVal.isPresent()) {
            return Optional.of(new ReturnNode(returnVal));
        } else {
            throw new RuntimeException("could not parse return value at return statement");
        }
    }

    /**
     * the entry point for the parseOperation recursive descent chain
     * @return a node containing the operation that was parsed
     */
    private Optional<Node> parseOperation() {
        return parseAssignment();
    }

    /**
     * 1st link in the parse operation chain. parses assignment statements
     * @return an assignment node if an assignment is found, otherwise, whatever was passed up from the chain
     */
    private Optional<Node> parseAssignment() {
        //call to next link in the chain
        Optional<Node> ex1 = parseTernary();
        Optional<Node> ex2;

        if (ex1.isPresent()) {
            //check for normal assignment
            if (tokens.matchAndRemove(Token.TokenType.EQUALS).isPresent()) {
                if (acceptSeparators()) {
                    throw new RuntimeException("invalid assignment operation across multiple lines");
                }
                ex2 = parseOperation();
                if (ex2.isPresent()) {
                    ex1 = Optional.of(new AssignmentNode(ex1.get(), ex2));
                } else {
                    throw new RuntimeException("no expression found after = at line " + tokens.peek(0).get().linenum);
                }
            }
            //check for += assignment
            else if (tokens.matchAndRemove(Token.TokenType.PLUS_EQUALS).isPresent()) {
                if (acceptSeparators()) {
                    throw new RuntimeException("invalid assignment operation across multiple lines");
                }
                ex2 = parseOperation();
                if (ex2.isPresent()) {
                    ex1 = Optional.of(new AssignmentNode(ex1.get(),
                            Optional.of(new OperationNode(ex1.get(), ex2.get(), OperationNode.operationType.ADD))));
                } else {
                    throw new RuntimeException("no expression found after += at line " + tokens.peek(0).get().linenum);
                }
            }
            //check for -= assignment
            else if (tokens.matchAndRemove(Token.TokenType.MINUS_EQUALS).isPresent()) {
                if (acceptSeparators()) {
                    throw new RuntimeException("invalid assignment operation across multiple lines");
                }
                ex2 = parseOperation();
                if (ex2.isPresent()) {
                    ex1 = Optional.of(new AssignmentNode(ex1.get(),
                            Optional.of(new OperationNode(ex1.get(), ex2.get(), OperationNode.operationType.SUBTRACT))));
                } else {
                    throw new RuntimeException("no expression found after -= at line " + tokens.peek(0).get().linenum);
                }
            }
            //check for /= assignment
            else if (tokens.matchAndRemove(Token.TokenType.SLASH_EQUALS).isPresent()) {
                if (acceptSeparators()) {
                    throw new RuntimeException("invalid assignment operation across multiple lines");
                }
                ex2 = parseOperation();
                if (ex2.isPresent()) {
                    ex1 = Optional.of(new AssignmentNode(ex1.get(),
                            Optional.of(new OperationNode(ex1.get(), ex2.get(), OperationNode.operationType.DIVIDE))));
                } else {
                    throw new RuntimeException("no expression found after /= at line " + tokens.peek(0).get().linenum);
                }
            }
            //check for *= assignment
            else if (tokens.matchAndRemove(Token.TokenType.STAR_EQUALS).isPresent()) {
                if (acceptSeparators()) {
                    throw new RuntimeException("invalid assignment operation across multiple lines");
                }
                ex2 = parseOperation();
                if (ex2.isPresent()) {
                    ex1 = Optional.of(new AssignmentNode(ex1.get(),
                            Optional.of(new OperationNode(ex1.get(), ex2.get(), OperationNode.operationType.MULTIPLY))));
                } else {
                    throw new RuntimeException("no expression found after *= at line " + tokens.peek(0).get().linenum);
                }
            }
            //check for %= assignment
            else if (tokens.matchAndRemove(Token.TokenType.PERCENT_EQUALS).isPresent()) {
                if (acceptSeparators()) {
                    throw new RuntimeException("invalid assignment operation across multiple lines");
                }
                ex2 = parseOperation();
                if (ex2.isPresent()) {
                    ex1 = Optional.of(new AssignmentNode(ex1.get(),
                            Optional.of(new OperationNode(ex1.get(), ex2.get(), OperationNode.operationType.MODULO))));
                } else {
                    throw new RuntimeException("no expression found after %= at line " + tokens.peek(0).get().linenum);
                }
            }
            //check for ^= assignment
            else if (tokens.matchAndRemove(Token.TokenType.CARROT_EQUALS).isPresent()) {
                if (acceptSeparators()) {
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

    /**
     * 2nd link in parseOperation chain
     * @return a TernaryNode or whatever was passed up from the chain
     */
    private Optional<Node> parseTernary() {
        //call to next link in the chain
        Optional<Node> ex1 = parseAnd();
        Optional<Node> truthexp, falseexp;
        if (ex1.isPresent()) {
            //check for ternary operator
            if (tokens.matchAndRemove(Token.TokenType.QUESTION).isPresent()) {
                acceptSeparators();
                //parse the truth expression of the ternary statement
                truthexp = parseOperation();
                if (!truthexp.isPresent()) {
                    throw new RuntimeException("invalid expression found at line " + tokens.peek(0).get().linenum);
                }
                //remove the colon operator
                if (!tokens.matchAndRemove(Token.TokenType.COLON).isPresent()) {
                    throw new RuntimeException("no ':' found in ternary expression at line " + tokens.peek(0).get().linenum);
                }
                //parse the false expression
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

    /**
     * 3rd link in the parseOperation chain
     * @return an OperationNode of type AND or whatever was passed up from the chain
     */
    private Optional<Node> parseAnd() {
        //call to next link in the chain
        Optional<Node> ex1 = parseOr();
        Optional<Node> ex2;
        if (ex1.isPresent()) {
            //attempt to match and operator
            if (tokens.matchAndRemove(Token.TokenType.AND_OP).isPresent()) {
                acceptSeparators();
                //call down into the chain for right side of and expression
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

    /**
     * 4th link in the parseOperation chain
     * @return a OperationNode of type OR or whatever was passed up the chain
     */
    private Optional<Node> parseOr() {
        //call to next link in the chain
        Optional<Node> ex1 = parseArrayMembership();
        Optional<Node> ex2;
        if (ex1.isPresent()) {
            // attempt to match the or operator
            if (tokens.matchAndRemove(Token.TokenType.OR_OP).isPresent()) {
                acceptSeparators();
                //call into the chain again for the right side of or expression
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

    /**
     * 5th link in the parseOperation chain
     * @return either a OperationNode of type IN or whatever was passed up from the chain
     */
    private Optional<Node> parseArrayMembership() {
        //call to next link in the chain
        Optional<Node> ex1 = parseMatch();
        if (ex1.isPresent()) {
            if (tokens.matchAndRemove(Token.TokenType.IN).isPresent()) {
                if (acceptSeparators()) {
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

    /**
     * 6th link in the parseOperation chain
     * @return either a OperationNode of type MATCH/NOTMATCH or whatever was passed up from the chain
     */
    private Optional<Node> parseMatch() {
        Optional<Node> ex1 = parseBoolean();
        Optional<Node> ex2;
        if (ex1.isPresent()) {
            if (tokens.matchAndRemove(Token.TokenType.MATCH).isPresent()) {
                if (acceptSeparators()) {
                    throw new RuntimeException("invalid match operation across multiple lines");
                }
                ex2 = parseOperation();
                if (!ex2.isPresent()) {
                    throw new RuntimeException("invalid expression found on right side of ERE match operation");
                }
                ex1 = Optional.of(new OperationNode(ex1.get(), ex2.get(), OperationNode.operationType.MATCH));
            } else if (tokens.matchAndRemove(Token.TokenType.NO_MATCH).isPresent()) {
                if (acceptSeparators()) {
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

    /**
     * 7th link in the parseOperation chain
     * @return either a OperationNode with the boolean expression or whatever was passed up from the chain
     */
    private Optional<Node> parseBoolean() {
        Optional<Node> ex1 = parseConcatination();
        Optional<Node> ex2;
        if (!ex1.isPresent()) {
            throw new RuntimeException("no valid expression found at line " + tokens.peek(0).get().linenum);
        }

        if (tokens.matchAndRemove(Token.TokenType.LESSTHAN).isPresent()) {
            if (acceptSeparators()) {
                throw new RuntimeException("invalid boolean operation across multiple lines");
            }
            ex2 = parseConcatination();
            if (ex2.isPresent()) {
                ex1 = Optional.of(new OperationNode(ex1.get(), ex2.get(), OperationNode.operationType.LT));
            } else {
                throw new RuntimeException("no/invalid expression found after less than sign at line " + tokens.peek(0).get().linenum);
            }
        } else if (tokens.matchAndRemove(Token.TokenType.LESS_THAN_EQUALTO).isPresent()) {
            if (acceptSeparators()) {
                throw new RuntimeException("invalid boolean operation across multiple lines");
            }
            ex2 = parseConcatination();
            if (ex2.isPresent()) {
                ex1 = Optional.of(new OperationNode(ex1.get(), ex2.get(), OperationNode.operationType.LE));
            } else {
                throw new RuntimeException("no/invalid expression found after less than or equal to sign at line " + tokens.peek(0).get().linenum);
            }
        } else if (tokens.matchAndRemove(Token.TokenType.NOT_EQUALS).isPresent()) {
            if (acceptSeparators()) {
                throw new RuntimeException("invalid boolean operation across multiple lines");
            }
            ex2 = parseConcatination();
            if (ex2.isPresent()) {
                ex1 = Optional.of(new OperationNode(ex1.get(), ex2.get(), OperationNode.operationType.NE));
            } else {
                throw new RuntimeException("no/invalid expression found after not equals sign at line " + tokens.peek(0).get().linenum);
            }
        } else if (tokens.matchAndRemove(Token.TokenType.DOUBLE_EQUALS).isPresent()) {
            if (acceptSeparators()) {
                throw new RuntimeException("invalid boolean operation across multiple lines");
            }
            ex2 = parseConcatination();
            if (ex2.isPresent()) {
                ex1 = Optional.of(new OperationNode(ex1.get(), ex2.get(), OperationNode.operationType.EQ));
            } else {
                throw new RuntimeException("no/invalid expression found after double equals at line " + tokens.peek(0).get().linenum);
            }
        } else if (tokens.matchAndRemove(Token.TokenType.GREATERTHAN).isPresent()) {
            if (acceptSeparators()) {
                throw new RuntimeException("invalid boolean operation across multiple lines");
            }
            ex2 = parseConcatination();
            if (ex2.isPresent()) {
                ex1 = Optional.of(new OperationNode(ex1.get(), ex2.get(), OperationNode.operationType.GT));
            } else {
                throw new RuntimeException("no/invalid expression found after greater than at line " + tokens.peek(0).get().linenum);
            }
        } else if (tokens.matchAndRemove(Token.TokenType.GREATER_THAN_EQUALTO).isPresent()) {
            if (acceptSeparators()) {
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

    /**
     * 8th link in the parseOperation chain
     * @return either a OperationNode of type CONCATENATION or whatever was passed up the chain
     */
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

    /**
     * 9th link in the parseOperation chain. parses + and - mathmatical expressions
     * @return an OperationNode for the math op or whatever was passed up from the chain
     */
    private Optional<Node> expression() {
        Optional<Node> term1 = term();
        Optional<Node> term2;

        while (tokens.moreTokens() && (tokens.peek(0).get().type == Token.TokenType.PLUS || tokens.peek(0).get().type == Token.TokenType.MINUS)) {
            if (tokens.matchAndRemove(Token.TokenType.PLUS).isPresent()) {
                if (acceptSeparators()) {
                    throw new RuntimeException("invalid mathmatical operation across multiple lines");
                }
                term2 = term();
                if (!term2.isPresent()) {
                    throw new RuntimeException("invalid expression at line " + tokens.peek(0).get().linenum);
                }
                term1 = Optional.of(new OperationNode(term1.get(), term2.get(), OperationNode.operationType.ADD));
            } else {
                tokens.matchAndRemove(Token.TokenType.MINUS);
                if (acceptSeparators()) {
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

    /**
     * 10th link in the parseOperation chain. parses *, /, % mathmatical operations
     * @return either a OperationNode for the math operation or whatever was passed up from the chain
     */
    private Optional<Node> term() {
        Optional<Node> factor1 = factor();
        Optional<Node> factor2;
        while (tokens.moreTokens() && (tokens.peek(0).get().type == Token.TokenType.STAR ||
                tokens.peek(0).get().type == Token.TokenType.SLASH ||
                tokens.peek(0).get().type == Token.TokenType.PERCENT)) {

            if (tokens.matchAndRemove(Token.TokenType.STAR).isPresent()) {
                if (acceptSeparators()) {
                    throw new RuntimeException("invalid mathmatical operation across multiple lines");
                }
                factor2 = factor();
                if (!factor2.isPresent()) {
                    throw new RuntimeException("invalid expression found at line " + tokens.peek(0).get().linenum);
                }
                factor1 = Optional.of(new OperationNode(factor1.get(), factor2.get(), OperationNode.operationType.MULTIPLY));
            } else if (tokens.matchAndRemove(Token.TokenType.SLASH).isPresent()) {
                if (acceptSeparators()) {
                    throw new RuntimeException("invalid mathmatical operation across multiple lines");
                }
                factor2 = factor();
                if (!factor2.isPresent()) {
                    throw new RuntimeException("invalid expression found at line " + tokens.peek(0).get().linenum);
                }
                factor1 = Optional.of(new OperationNode(factor1.get(), factor2.get(), OperationNode.operationType.DIVIDE));
            } else {
                tokens.matchAndRemove(Token.TokenType.PERCENT);
                if (acceptSeparators()) {
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

    /**
     * 11th link in the parseOperation chain.
     * @return a OperationNode of type EXPONENT or whatever was passed up from the chain
     */
    private Optional<Node> parseExponents() {
        Optional<Node> ex1 = parsePostIncDec();
        Optional<Node> ex2;
        if (ex1.isPresent()) {
            if (tokens.matchAndRemove(Token.TokenType.CARROT).isPresent()) {
                if (acceptSeparators()) {
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

    /**
     * 12th link in the parseOperation chain
     * @return an OperationNode for the increment or decrement, or whatever was passed up from the chain
     */
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

    /**
     * the bottom level of the parseOperation chain. handles:
     * string literals, numbers, unary operators, preincrements and decrements and function calls
     * @return a node for one of the above components
     */
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
                if (acceptSeparators()) {
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
                if (acceptSeparators()) {
                    throw new RuntimeException("invalid negated operation across multiple lines");
                }
                result = parseOperation();
                if (result.isPresent()) {
                    return Optional.of(new OperationNode(result.get(), OperationNode.operationType.NOT));
                }
                throw new RuntimeException("could not parse not expression at line " + tokens.peek(0).get().linenum);
            case MINUS:
                tokens.matchAndRemove(Token.TokenType.MINUS);
                if (acceptSeparators()) {
                    throw new RuntimeException("invalid negative expression across multiple lines");
                }
                result = parseOperation();
                if (result.isPresent()) {
                    return Optional.of(new OperationNode(result.get(), OperationNode.operationType.UNARYNEG));
                }
                throw new RuntimeException("could not parse unary negation expression at line " + tokens.peek(0).get().linenum);
            case PLUS:
                tokens.matchAndRemove(Token.TokenType.PLUS);
                if (acceptSeparators()) {
                    throw new RuntimeException("invalid positive expression across multiple lines");
                }
                result = parseOperation();
                if (result.isPresent()) {
                    return Optional.of(new OperationNode(result.get(), OperationNode.operationType.UNARYPOS));
                }
                throw new RuntimeException("could not parse Unary addition expression at line " + tokens.peek(0).get().linenum);
            case INCREMENT:
                tokens.matchAndRemove(Token.TokenType.INCREMENT);
                if (acceptSeparators()) {
                    throw new RuntimeException("invalid preincrement operation across multiple lines");
                }
                result = parseOperation();
                if (result.isPresent()) {
                    return Optional.of(new OperationNode(result.get(), OperationNode.operationType.PREINC));
                }
                throw new RuntimeException("could not parse preincrement expression at line " + tokens.peek(0).get().linenum);
            case DECREMENT:
                tokens.matchAndRemove(Token.TokenType.DECREMENT);
                if (acceptSeparators()) {
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

    /**
     * method responsible for parsing function calls
     * @return a FunctionCallNode including function name and parameters
     */
    private Optional<Node> parseFunctionCall() {
        if(tokens.peek(0).get().type == Token.TokenType.WORD){
            //attempts to parse the name of the function
            Optional<Node> funcName = parseLValue();
            if(!funcName.isPresent()){
                throw new RuntimeException("unable to parse word at bottom level");
            }
            //if open paren is present, we know we have a function call
            if(tokens.matchAndRemove(Token.TokenType.OPEN_PAREN).isPresent()){
                Optional<Node> currentParam;
                LinkedList<Node> params = new LinkedList<>();
                //gather function parameters
                while (!tokens.matchAndRemove(Token.TokenType.CLOSE_PAREN).isPresent()) {
                    currentParam = parseOperation();
                    if (!currentParam.isPresent()) {
                        throw new RuntimeException("failed to parse parameter being passed into function call");
                    }
                    params.add(currentParam.get());
                    tokens.matchAndRemove(Token.TokenType.COMMA);
                }
                //return function call node
                return Optional.of(new FunctionCallNode((VariableReferenceNode) funcName.get(), params));
            }
            //if parentheses are not present, we know we are just dealing with a variable refrence. return funcName
            return funcName;
        }
        //if incoming token is a dollar, we know we are dealing with a field reference
        else if(tokens.peek(0).get().type == Token.TokenType.DOLLAR){
            return parseLValue();
        }
        //else, the token must be refering to a awk built in function
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
            //gather built in parameters
            if(tokens.moreTokens()) {
                if(tokens.peek(0).get().type != Token.TokenType.OPEN_PAREN) {
                    while (tokens.peek(0).get().type != Token.TokenType.SEPARATOR
                            && tokens.peek(0).get().type != Token.TokenType.CLOSE_CURLY) {
                        currentParam = parseOperation();
                        if (!currentParam.isPresent()) {
                            throw new RuntimeException("failed to parse parameter at " + builtinName + " call");
                        }
                        params.add(currentParam.get());
                        tokens.matchAndRemove(Token.TokenType.COMMA);
                    }
                }
                //built in parameters can also be provided without parentheses
                else{
                    tokens.matchAndRemove(Token.TokenType.OPEN_PAREN);
                    while(tokens.peek(0).get().type != Token.TokenType.CLOSE_PAREN){
                        currentParam = parseOperation();
                        if (!currentParam.isPresent()) {
                            throw new RuntimeException("failed to parse parameter at " + builtinName + " call");
                        }
                        params.add(currentParam.get());
                        tokens.matchAndRemove(Token.TokenType.COMMA);
                    }
                    tokens.matchAndRemove(Token.TokenType.CLOSE_PAREN);
                }
                acceptSeparators();
            }
            return Optional.of(new FunctionCallNode(new VariableReferenceNode(builtinName, Optional.empty()), params));
        }
    }

    /**
     * method responsible for parsing field references and variable references
     * @return either an OperationNode for the field reference, or a VariableReferenceNode for the var reference
     */
    private Optional<Node> parseLValue() {
        //field reference case
        if (tokens.matchAndRemove(Token.TokenType.DOLLAR).isPresent()) {
            if (acceptSeparators()) {
                throw new RuntimeException("invalid operation across multiple lines");
            }
            return Optional.of(new OperationNode(parseBottomLevel().get(), OperationNode.operationType.DOLLAR));
        }
        //var reference case
        else if (tokens.peek(0).get().type == Token.TokenType.WORD) {
            String varName = tokens.matchAndRemove(Token.TokenType.WORD).get().value;
            if (tokens.matchAndRemove(Token.TokenType.OPEN_SQAURE).isPresent()) {
                if (acceptSeparators()) {
                    throw new RuntimeException("invalid array index across multiple lines");
                }
                Optional<Node> expression = parseOperation();
                if (!tokens.matchAndRemove(Token.TokenType.CLOSE_SQUARE).isPresent()) {
                    throw new RuntimeException("failed to close array index brackets at line " + tokens.peek(0).get().linenum);
                }
                acceptSeparators();
                return Optional.of(new VariableReferenceNode(varName, expression));
            } else {
                return Optional.of(new VariableReferenceNode(varName, Optional.empty()));
            }
        } else {
            throw new RuntimeException("$ or word not found when attempting to parse Lvalue at line " + tokens.peek(0).get().linenum);
        }
    }
}