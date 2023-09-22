import java.util.LinkedList;
import java.util.Optional;

public class TokenManager {

    private LinkedList<Token> tokens;

    public TokenManager(LinkedList<Token> list){
        tokens = list;
    }

    public Optional<Token> peek(int i) {
       return Optional.of(tokens.get(i));
    }

    public boolean moreTokens(){
        return !tokens.isEmpty();
    }

    public Optional<Token> matchAndRemove(Token.TokenType type){
        if(type == tokens.get(0).type){
            return Optional.of(tokens.remove());
        }
        else{
            return Optional.empty();
        }
    }
}
