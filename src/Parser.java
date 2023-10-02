import java.util.LinkedList;
import java.util.Optional;

public class Parser {

    private TokenManager tokens;

    public Parser(LinkedList<Token> list){
        tokens = new TokenManager(list);
    }

    private boolean acceptSeperators(){
        if(tokens.moreTokens() && tokens.peek(0).get().type == Token.TokenType.SEPARATOR){
            while(tokens.moreTokens() && tokens.peek(0).get().type == Token.TokenType.SEPARATOR){
                tokens.matchAndRemove(Token.TokenType.SEPARATOR);
            }
            return true;
        }
        else{
            return false;
        }
    }

    public ProgramNode parse(){
        ProgramNode program = new ProgramNode();
        while(tokens.moreTokens()){
            if(!parseFunction(program)){
                if(!parseAction(program)){
                    throw new RuntimeException("failed to parse function or action");
                }
            }
        }
        return program;
    }

    private boolean parseFunction(ProgramNode program){
        if(tokens.peek(0).get().type != Token.TokenType.FUNCTION){
            return false;
        }
        tokens.matchAndRemove(Token.TokenType.FUNCTION);
        if(tokens.peek(0).get().type != Token.TokenType.WORD){
            throw new RuntimeException("function name is expected after 'function' keyowrd at line " + tokens.peek(0).get().linenum);
        }
        String name = tokens.matchAndRemove(Token.TokenType.WORD).get().value;

        if(tokens.peek(0).get().type != Token.TokenType.OPEN_PAREN){
            throw new RuntimeException("'(' must follow function declaration at line " + tokens.peek(0).get().linenum);
        }
        tokens.matchAndRemove(Token.TokenType.OPEN_PAREN);

        LinkedList<String> params = new LinkedList<>();
        if(tokens.peek(0).get().type != Token.TokenType.CLOSE_PAREN) {
            if(tokens.peek(0).get().type != Token.TokenType.WORD){
                throw new RuntimeException("parameter identifier expected after open paren in function declaration at line " + tokens.peek(0).get().linenum);
            }
            params.add(tokens.matchAndRemove(Token.TokenType.WORD).get().value);
            while (tokens.peek(0).get().type != Token.TokenType.CLOSE_PAREN) {
                if(!tokens.matchAndRemove(Token.TokenType.COMMA).isPresent()){
                    throw new RuntimeException("comma expected after parameter declaration in function definition at line " + tokens.peek(0).get().linenum);
                }
                acceptSeperators();
                if(tokens.peek(0).get().type != Token.TokenType.WORD){
                    throw new RuntimeException("parameter identifier expected after comma in function declaration at line " + tokens.peek(0).get().linenum);
                }
                params.add(tokens.matchAndRemove(Token.TokenType.WORD).get().value);
            }
        }
        if(!tokens.matchAndRemove(Token.TokenType.CLOSE_PAREN).isPresent()){
            throw new RuntimeException("no close parentheses found at the end of function declration at lime " + tokens.peek(0).get().linenum);
        }

        program.addFunction(new FunctionNode(name, parseBlock().getStatements(), params));
        return true;
    }

    private boolean parseAction(ProgramNode program){
        if(tokens.peek(0).isPresent()) {
            if (tokens.peek(0).get().type == Token.TokenType.BEGIN) {
                tokens.matchAndRemove(Token.TokenType.BEGIN);
                program.addStartBlock(parseBlock());
            } else if (tokens.peek(0).get().type == Token.TokenType.END) {
                tokens.matchAndRemove(Token.TokenType.END);
                program.addEndBlock(parseBlock());
            } else {
                parseOperation();
                program.addBlock(parseBlock());
            }
            return true;
        }
        throw new RuntimeException("no tokens left in file when trying to parse action at line");
    }

    private BlockNode parseBlock(){
        return new BlockNode();
    }

    private Optional<Node> parseOperation(){
        Optional<Node> bottomLevelResult = parseBottomLevel();
        if(tokens.moreTokens() && bottomLevelResult.isPresent()){
            if(tokens.peek(0).get().type == Token.TokenType.INCREMENT){
                return Optional.of(new OperationNode(bottomLevelResult.get(), OperationNode.operationType.POSTINC));
            }
            else if(tokens.peek(0).get().type == Token.TokenType.DECREMENT){
                return Optional.of(new OperationNode(bottomLevelResult.get(), OperationNode.operationType.POSTDEC));
            } else {
                return bottomLevelResult;
            }
        }

    }



    private Optional<Node> parseBottomLevel(){
        Optional<Node> result;
        switch (tokens.peek(0).get().type){
            case STRINGLITERAL:
            case NUMBER:
                return Optional.of(new ConstantNode(tokens.matchAndRemove(tokens.peek(0).get().type).get().value));
            case PATTERN:
                return Optional.of(new PatternNode(tokens.matchAndRemove(Token.TokenType.PATTERN).get().value));
            case OPEN_PAREN:
                tokens.matchAndRemove(Token.TokenType.OPEN_PAREN);
                result = parseOperation();
                if(tokens.peek(0).get().type != Token.TokenType.CLOSE_PAREN){
                    throw new RuntimeException("no close parenthes found at expression at line " + tokens.peek(0).get().linenum );
                }
                tokens.matchAndRemove(Token.TokenType.CLOSE_PAREN);
                return result;
            case NOT:
                tokens.matchAndRemove(Token.TokenType.NOT);
                result = parseOperation();
                if(result.isPresent()){
                    return Optional.of(new OperationNode(result.get(), OperationNode.operationType.NOT));
                }
                throw new RuntimeException("could not parse not expression at line " + tokens.peek(0).get().linenum);
            case MINUS:
                tokens.matchAndRemove(Token.TokenType.MINUS);
                result = parseOperation();
                if(result.isPresent()){
                    return Optional.of(new OperationNode(result.get(), OperationNode.operationType.UNARYNEG));
                }
                throw new RuntimeException("could not parse unary negation expression at line " + tokens.peek(0).get().linenum);
            case PLUS:
                tokens.matchAndRemove(Token.TokenType.PLUS);
                result = parseOperation();
                if(result.isPresent()){
                    return Optional.of(new OperationNode(result.get(), OperationNode.operationType.UNARYPOS));
                }
                throw new RuntimeException("could not parse Unary addition expression at line " + tokens.peek(0).get().linenum);
            case INCREMENT:
                tokens.matchAndRemove(Token.TokenType.INCREMENT);
                result = parseOperation();
                if(result.isPresent()){
                    return Optional.of(new OperationNode(result.get(), OperationNode.operationType.PREINC));
                }
                throw new RuntimeException("could not parse preincrement expression at line " + tokens.peek(0).get().linenum);
            case DECREMENT:
                tokens.matchAndRemove(Token.TokenType.DECREMENT);
                result = parseOperation();
                if(result.isPresent()){
                    return Optional.of(new OperationNode(result.get(), OperationNode.operationType.PREDEC));
                }
                throw new RuntimeException("could not parse predecrement expression at line " + tokens.peek(0).get().linenum);
            default:
                return parseLValue();
        }

    }

    private Optional<Node> parseLValue(){
        if(tokens.peek(0).get().type == Token.TokenType.DOLLAR){
            tokens.matchAndRemove(Token.TokenType.DOLLAR);
            return Optional.of(new OperationNode(parseBottomLevel().get(), OperationNode.operationType.DOLLAR));
        }
        else if(tokens.peek(0).get().type == Token.TokenType.WORD){
            String varName = tokens.matchAndRemove(Token.TokenType.WORD).get().value;
            if(tokens.moreTokens() && tokens.peek(0).get().type == Token.TokenType.OPEN_SQAURE){
                tokens.matchAndRemove(Token.TokenType.OPEN_SQAURE);
                Optional<Node> expression = parseOperation();
                if(tokens.peek(0).get().type != Token.TokenType.CLOSE_SQUARE){
                    throw new RuntimeException("failed to close array index brackets at line " + tokens.peek(0).get().linenum);
                }
                return Optional.of(new VariableReferenceNode(varName, expression));
            }
            else{
                return Optional.of(new VariableReferenceNode(varName, Optional.empty()));
            }
        }
        else{
            throw new RuntimeException("$ or word not found when attempting to parse Lvalue at line " + tokens.peek(0).get().linenum);
        }
    }
}
