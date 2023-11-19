import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class Awk {
    public static void main(String[] args) throws IOException {
        String filestring;
        Optional<String> recordPath;
        if(args.length == 2){
            Path filepath = Paths.get(args[0]);
             filestring = new String(Files.readAllBytes(filepath));
             recordPath = Optional.of(args[1]);
        }
        else if(args.length == 1){
            Path filepath = Paths.get(args[0]);
            filestring = new String(Files.readAllBytes(filepath));
            recordPath = Optional.empty();
        }
        else{
            throw new RuntimeException("incorrect arguments passed to awkinterp. expected path to awk program at arg 0, " +
                    "optional path to record file at arg 1");
        }


        Lexer lex = new Lexer(filestring);
        lex.Lex();

        Parser parser = new Parser(lex.lexedTokens);
        ProgramNode program = parser.parse();

        Interpreter interp = new Interpreter(program, recordPath);
        interp.InterpretProgram();
    }
}
