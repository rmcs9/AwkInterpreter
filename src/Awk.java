import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Optional;

public class Awk {
    public static void main(String[] args) throws IOException {
        Path filepath = Paths.get(args[0]);
        String filestring = new String(Files.readAllBytes(filepath));

//        Lexer tests
//        Lexer lex = new Lexer(filestring);
//        lex.Lex();
//
//        for(int i = 0; i < lex.lexedTokens.size(); i++){
//            System.out.println(lex.lexedTokens.get(i));
//        }

//        Parser tests
//        Parser parser = new Parser(lex.lexedTokens);
//        Optional<StatementNode> test = parser.parseStatement();
//        Optional<Node> test1 = parser.parseOperation();
//
//        ProgramNode program = parser.parse();

        //Interpreter tests
        Interpreter interp = new Interpreter(new ProgramNode(), Optional.empty());
        //a[2 + 5]++
        OperationNode incOp = new OperationNode(new VariableReferenceNode("a", Optional.of(new OperationNode(new ConstantNode("2"), new ConstantNode("5"), OperationNode.operationType.ADD))), OperationNode.operationType.POSTINC);
        //a[2]
        Node left = new VariableReferenceNode("a", Optional.of(new ConstantNode("2")));
        //5 > 4 ? 1 : 0
        Node right = new TernaryNode(new OperationNode(new ConstantNode("5"), new ConstantNode("4"), OperationNode.operationType.GT), new ConstantNode("1"), new ConstantNode("0"));
        //a[2] = 5 > 4 ? 1 : 0
        AssignmentNode assign = new AssignmentNode(left, Optional.of(right));

        HashMap<String, IDT> locals = new HashMap<>();
        IDT result = interp.getIDT(incOp, Optional.empty());
        System.out.println("success");

    }
}
