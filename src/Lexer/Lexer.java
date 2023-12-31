package Lexer;
import java.util.HashMap;
import java.util.LinkedList;

public class Lexer {

    public Lexer(String input) {
        file = new StringHandler(input);
        hashWordsFill();
    }

    private void hashWordsFill() {
        //KNOWN WORDS
        knownwords.put("while", Token.TokenType.WHILE);
        knownwords.put("if", Token.TokenType.IF);
        knownwords.put("do", Token.TokenType.DO);
        knownwords.put("for", Token.TokenType.FOR);
        knownwords.put("break", Token.TokenType.BREAK);
        knownwords.put("continue", Token.TokenType.CONTINUE);
        knownwords.put("else", Token.TokenType.ELSE);
        knownwords.put("return", Token.TokenType.RETURN);
        knownwords.put("BEGIN", Token.TokenType.BEGIN);
        knownwords.put("END", Token.TokenType.END);
        knownwords.put("print", Token.TokenType.PRINT);
        knownwords.put("printf", Token.TokenType.PRINTF);
        knownwords.put("next", Token.TokenType.NEXT);
        knownwords.put("in", Token.TokenType.IN);
        knownwords.put("delete", Token.TokenType.DELETE);
        knownwords.put("getline", Token.TokenType.GETLINE);
        knownwords.put("exit", Token.TokenType.EXIT);
        knownwords.put("nextfile", Token.TokenType.NEXTFILE);
        knownwords.put("function", Token.TokenType.FUNCTION);

        //DOUBLE SYMBOLS
        doubleSymbols.put(">=", Token.TokenType.GREATER_THAN_EQUALTO);
        doubleSymbols.put("++", Token.TokenType.INCREMENT);
        doubleSymbols.put("--", Token.TokenType.DECREMENT);
        doubleSymbols.put("<=", Token.TokenType.LESS_THAN_EQUALTO);
        doubleSymbols.put("==", Token.TokenType.DOUBLE_EQUALS);
        doubleSymbols.put("!=", Token.TokenType.NOT_EQUALS);
        doubleSymbols.put("^=", Token.TokenType.CARROT_EQUALS);
        doubleSymbols.put("%=", Token.TokenType.PERCENT_EQUALS);
        doubleSymbols.put("*=", Token.TokenType.STAR_EQUALS);
        doubleSymbols.put("/=", Token.TokenType.SLASH_EQUALS);
        doubleSymbols.put("+=", Token.TokenType.PLUS_EQUALS);
        doubleSymbols.put("-=", Token.TokenType.MINUS_EQUALS);
        doubleSymbols.put("!~", Token.TokenType.NO_MATCH);
        doubleSymbols.put("&&", Token.TokenType.AND_OP);
        doubleSymbols.put(">>", Token.TokenType.BASH);
        doubleSymbols.put("||", Token.TokenType.OR_OP);

        //SINGLE SYMBOLS
        singleSymbols.put("{", Token.TokenType.OPEN_CURLY);
        singleSymbols.put("}", Token.TokenType.CLOSE_CURLY);
        singleSymbols.put("[", Token.TokenType.OPEN_SQAURE);
        singleSymbols.put("]", Token.TokenType.CLOSE_SQUARE);
        singleSymbols.put("(", Token.TokenType.OPEN_PAREN);
        singleSymbols.put(")", Token.TokenType.CLOSE_PAREN);
        singleSymbols.put("$", Token.TokenType.DOLLAR);
        singleSymbols.put("~", Token.TokenType.MATCH);
        singleSymbols.put("=", Token.TokenType.EQUALS);
        singleSymbols.put("<", Token.TokenType.LESSTHAN);
        singleSymbols.put(">", Token.TokenType.GREATERTHAN);
        singleSymbols.put("!", Token.TokenType.NOT);
        singleSymbols.put("+", Token.TokenType.PLUS);
        singleSymbols.put("^", Token.TokenType.CARROT);
        singleSymbols.put("-", Token.TokenType.MINUS);
        singleSymbols.put("?", Token.TokenType.QUESTION);
        singleSymbols.put(":", Token.TokenType.COLON);
        singleSymbols.put("*", Token.TokenType.STAR);
        singleSymbols.put("/", Token.TokenType.SLASH);
        singleSymbols.put("%", Token.TokenType.PERCENT);
        singleSymbols.put(";", Token.TokenType.SEMICOLON);
        singleSymbols.put("\n", Token.TokenType.NEWLINE);
        singleSymbols.put("|", Token.TokenType.ORLINE);
        singleSymbols.put(",", Token.TokenType.COMMA);
    }

    public LinkedList<Token> lexedTokens = new LinkedList<>();

    private StringHandler file;

    private int linenum = 1;

    private int charPos = 0;

    private HashMap<String, Token.TokenType> knownwords = new HashMap<>();
    private HashMap<String, Token.TokenType> doubleSymbols = new HashMap<>();
    private HashMap<String, Token.TokenType> singleSymbols = new HashMap<>();

    /**
     * main method of the lexer
     * here, characters are iterated over and added to lexedTokens as tokens
     */
    public void Lex() {
        while (!file.isDone()) {
            switch (file.peek(0)) {
                case ' ':
                case '\t':
                    //spaces and tabs absorbed
                    file.swallow(1);
                    charPos++;
                    break;
                case '\n':
                case ';':
                    //newlines and semicolons are used to seperate statements
                    lexedTokens.add(new Token(Token.TokenType.SEPARATOR, linenum, charPos));
                    file.swallow(1);
                    charPos = 0;
                    linenum++;
                    break;
                case '\r':
                    //carriage returns absorbed
                    file.swallow(1);
                    break;
                case '#':
                    // octothorps signal a comment. the entire line containing the comment is absorbed
                    file.swallow(1);
                    while (!file.isDone() && file.peek(0) != '\n') {
                        file.swallow(1);
                    }
                    break;
                case '"':
                    // " signals the start of a sliteral. processStringLiteral is called
                    lexedTokens.add(processStringLiteral());
                    break;
                case '`':
                    // ` signals the start of a regex pattern. processPattern is called
                    lexedTokens.add(processPattern());
                    break;
                default:
                    // if none of the cases are hit, we are expecting either a word, number or symbol
                    if (Character.isLetter(file.peek(0))) {
                        lexedTokens.add(processWord());
                    } else if (Character.isDigit(file.peek(0))) {
                        lexedTokens.add(processNumber());
                    } else {
                        lexedTokens.add(processSymbol());
                    }
                    break;
            }
        }
    }

    /**
     * processWord accumulates characters until it has a completed word,
     * which is then returned in token form
     * @return a token, either containing the unique word, or the word matched from one of the languages knownwords
     */
    public Token processWord() {
        char current = file.getChar();
        String accum = "" + current;
        charPos++;
        while (!file.isDone() &&
                (Character.isLetter(file.peek(0)) || Character.isDigit(file.peek(0)) || file.peek(0) == '_')) {
            current = file.getChar();
            accum += current;
            charPos++;
        }

        if (knownwords.containsKey(accum)) {
            return new Token(knownwords.get(accum), linenum, charPos);
        } else {
            return new Token(Token.TokenType.WORD, linenum, charPos, accum);
        }
    }

    /**
     * accumulates characters until a complete number is obtained.
     * a number can be defined by the following regex: [0-9]+(\.[0-9]+)?
     * @return a number token with the accumulated number
     */
    public Token processNumber() {
        char current = file.getChar();
        String accum = "" + current;
        charPos++;
        boolean hadDot = false;
        while (!file.isDone() && (Character.isDigit(file.peek(0)) || file.peek(0) == '.')) {
            if(file.peek(0) == '.' && !hadDot){
                accum += file.getChar();
                hadDot = true;
            }
            else if(file.peek(0) == '.' && hadDot){
                throw new RuntimeException("2 decimal points present in number at line " + linenum);
            }
            current = file.getChar();
            accum += current;
            charPos++;
        }
        return new Token(Token.TokenType.NUMBER, linenum, charPos, accum);
    }

    /**
     * accumulates characters until a closing double quote is found.
     * @return a sliteral token with the sliteral
     */
    public Token processStringLiteral() {
        file.swallow(1);
        String accum = "";
        while (!file.isDone() && file.peek(0) != '"') {
            if (file.peek(0) == '\\') {
                file.swallow(1);
                if(file.peek(0) == 'n'){
                    accum += '\n';
                    file.swallow(1);
                }
                else if(file.peek(0) == '\\'){
                    file.swallow(1);
                    if(file.peek(0) == 'n'){
                        accum += "\\n";
                        file.swallow(1);
                    }
                }
                else if(file.peek(0) == 't'){
                    accum += '\t';
                    file.swallow(1);
                }
                else if(file.peek(0) == '"'){
                    accum += '"';
                    file.swallow(1);
                }
            }
            else {
                accum += file.getChar();
                charPos++;
            }
        }
        file.swallow(1);
        return new Token(Token.TokenType.STRINGLITERAL, linenum, charPos, accum);
    }

    /**
     * accumulates characters until the closing ` is found
     * @return a pattern token with the accumulated pattern
     */
    public Token processPattern() {
        file.swallow(1);
        String accum = "";
        while (!file.isDone() && file.peek(0) != '`') {
            accum += file.getChar();
            charPos++;
        }
        file.swallow(1);
        return new Token(Token.TokenType.PATTERN, linenum, charPos, accum);
    }

    /**
     * processes the incoming symbol character(s)
     * first attempts to match a double character symbol,
     * then matches a single symbol
     * @return a token with the matched symbol
     */
    public Token processSymbol() {
        String symbolPeek;
        try {
            symbolPeek = "" + file.peek(0) + file.peek(1);
        } catch (Exception e) {
            symbolPeek = "" + file.peek(0);
            file.swallow(1);
            charPos++;
            return new Token(singleSymbols.get(symbolPeek), linenum, charPos-1);
        }
        if (doubleSymbols.containsKey(symbolPeek)) {
            file.swallow(2);
            charPos += 2;
            return new Token(doubleSymbols.get(symbolPeek), linenum, charPos - 2);
        } else {
            String sym = "" + file.peek(0);
            file.swallow(1);
            charPos++;
            return new Token(singleSymbols.get(sym), linenum, charPos - 1);
        }
    }
}