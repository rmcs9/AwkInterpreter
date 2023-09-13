import java.util.LinkedList;

public class Lexer {

    public Lexer(String input){
        file = new StringHandler(input);
    }

    public LinkedList<Token> lexedTokens = new LinkedList<>();

    private StringHandler file;

    private int linenum = 1;

    private int charPos = 0;

    public void Lex() {
        while(!file.isDone()){
            switch(file.peek(0)){
                case ' ':
                case '\t':
                    file.swallow(1);
                    charPos++;
                    break;
                case '\n':
                    lexedTokens.add(new Token(Token.TokenType.SEPARATOR, linenum, charPos));
                    file.swallow(1);
                    charPos = 0;
                    linenum++;
                    break;
                case '\r':
                    file.swallow(1);
                    break;
                default:
                    if(Character.isLetter(file.peek(0))){
                        lexedTokens.add(processWord());
                    }
                    else if(Character.isDigit(file.peek(0))){
                        lexedTokens.add(processNumber());
                    }
                    else{
                        throw new RuntimeException("unknown character at " + linenum + " position " + charPos);
                    }
                    break;
            }
        }
    }

    public Token processWord(){
        char current = file.getChar();
        String accum = "" + current;
        charPos++;
        while(!file.isDone() &&
                (Character.isLetter(file.peek(0)) || Character.isDigit(file.peek(0)) || file.peek(0) == '_')){
            current = file.getChar();
            accum += current;
            charPos++;
        }
        return new Token(Token.TokenType.WORD, linenum, charPos, accum);
    }

    public Token processNumber(){
        char current = file.getChar();
        String accum = "" + current;
        charPos++;
        while(!file.isDone() && Character.isDigit(file.peek(0))){
            current = file.getChar();
            accum += current;
            charPos++;
        }
        return new Token(Token.TokenType.NUMBER, linenum, charPos, accum);
    }

}
