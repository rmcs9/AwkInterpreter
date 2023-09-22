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
                //conditions and errors
                //TODO: EDIT AT PARSER 2
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
        return Optional.empty();
    }
}
