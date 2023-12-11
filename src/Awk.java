import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;


import Lexer.*;
import Parser.*;
import Interpreter.*;

public class Awk {
    public static void main(String[] args) throws IOException {
       //argument 0, the awk program file
        //argument 1, the optional txt records file

        String filestring;
        Optional<String> recordPath;
        //records file attached
        if(args.length == 2){
            Path filepath = Paths.get(args[0]);
             filestring = new String(Files.readAllBytes(filepath));
             recordPath = Optional.of(args[1]);
        }
        //no records file
        else if(args.length == 1){
            Path filepath = Paths.get(args[0]);
            filestring = new String(Files.readAllBytes(filepath));
            recordPath = Optional.empty();
        }
        else{
            throw new RuntimeException("incorrect arguments passed to awkinterp. expected path to awk program at arg 0, " +
                    "optional path to record file at arg 1");
        }

        //create new lexer and lex tokens
        Lexer lex = new Lexer(filestring);
        lex.Lex();
        //create new parser and parse tokens
        Parser parser = new Parser(lex.lexedTokens);
        ProgramNode program = parser.parse();
        //create new interpreter and interpret the program
        Interpreter interp = new Interpreter(program, recordPath);
        interp.InterpretProgram();
    }
}
