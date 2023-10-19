import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class Awk {
    public static void main(String[] args) throws IOException {
        Path filepath = Paths.get(args[0]);
        String filestring = new String(Files.readAllBytes(filepath));

        Lexer lex = new Lexer(filestring);
        lex.Lex();

        for(int i = 0; i < lex.lexedTokens.size(); i++){
            System.out.println(lex.lexedTokens.get(i));
        }

        Parser parser = new Parser(lex.lexedTokens);
        //Optional<StatementNode> test = parser.parseStatement();
        //Optional<Node> test = parser.parseOperation();

        ProgramNode program = parser.parse();
        System.out.println("success");

    }
}
