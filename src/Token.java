public class Token {

    public enum TokenType {
        WORD, NUMBER, SEPARATOR
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
            case SEPARATOR:
                return "NEWLINE";
            default:
                return null;
        }
    }
}
