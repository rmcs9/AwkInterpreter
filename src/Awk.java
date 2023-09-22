import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
        ProgramNode newProgram = parser.parse();
    }


//    403 HOMEWORK 2
//
//    public static int averagesSum(int[] arr) {
//        int averages = 0;
//        for (int i = 1; i < arr.length - 1; i++) {
//            if ((arr[i - 1] + arr[i + 1]) / 2 == arr[i]) {
//                averages++;
//            }
//        }
//        return averages;
//    }
//
//    public static int averageSumRecursive(int[] A, int s, int e) {
//        int middle;
//        int averages = 0;
//        if (s < e) {
//            middle = (s + e) / 2;
//            averages += averageSumRecursive(A, s, middle);
//            averages += averageSumRecursive(A, middle + 1, e);
//        } else {
//            if ((A[s - 1] + A[s + 1]) / 2 == A[s]) {
//                averages++;
//            }
//        }
//        return averages;
//    }
}
