package Parser;
import java.util.LinkedList;
import java.util.Optional;
import Lexer.*;

public class TokenManager {

    private LinkedList<Token> tokens;

    public TokenManager(LinkedList<Token> list){
        tokens = list;
    }

    public Optional<Token> peek(int i) {
        if(this.moreTokens()){
            return Optional.of(tokens.get(i));
        }
        return Optional.empty();
    }

    public boolean moreTokens(){
        return !tokens.isEmpty();
    }

    public Optional<Token> matchAndRemove(Token.TokenType type){
        if(!this.moreTokens()){
            return Optional.empty();
        }
        if(type == tokens.get(0).type){
            return Optional.of(tokens.remove());
        }
        else{
            return Optional.empty();
        }
    }
}
