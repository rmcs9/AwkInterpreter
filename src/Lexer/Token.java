package Lexer;
public class Token {

    public enum TokenType {
        WORD, NUMBER, SEPARATOR, STRINGLITERAL, PATTERN,

        //reserved words
        WHILE, IF, DO, FOR, BREAK, CONTINUE, ELSE, RETURN,
        BEGIN, END, PRINT, PRINTF, NEXT, IN, DELETE, GETLINE,
        EXIT, NEXTFILE, FUNCTION,

        //DOUBLE SYMBOLS
        GREATER_THAN_EQUALTO, INCREMENT, DECREMENT, LESS_THAN_EQUALTO,
        DOUBLE_EQUALS, NOT_EQUALS, CARROT_EQUALS, PERCENT_EQUALS, STAR_EQUALS,
        SLASH_EQUALS, PLUS_EQUALS, MINUS_EQUALS, NO_MATCH, AND_OP, BASH, OR_OP,

        //SINGLE SYMBOLS
        OPEN_CURLY, CLOSE_CURLY, OPEN_SQAURE, CLOSE_SQUARE, OPEN_PAREN, CLOSE_PAREN,
        DOLLAR, MATCH, EQUALS, LESSTHAN, GREATERTHAN, NOT, PLUS, CARROT, MINUS,
        QUESTION, COLON, STAR, SLASH, PERCENT, SEMICOLON, NEWLINE, ORLINE, COMMA
    }

    public TokenType type;

    public String value;

    public int linenum;

    public int charPosition;

    public Token(TokenType toktype, int lNumber, int pos){
        type = toktype;
        linenum = lNumber;
        charPosition = pos;
    }

    public Token(TokenType toktype, int lNumber, int pos, String val){
        type = toktype;
        linenum = lNumber;
        charPosition = pos;
        value = val;
    }

    public String toString(){
        switch(this.type){
            case WORD:
                return "WORD(" + this.value + ")";
            case NUMBER:
                return "NUMBER(" + this.value + ")";
            default:
                return type.toString();
        }
    }
}
