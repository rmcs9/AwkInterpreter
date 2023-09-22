import java.util.LinkedList;

public class FunctionNode extends Node{

    public FunctionNode(String n, LinkedList<StatementNode> lines, LinkedList<String> param){
        name = n;
        statements = lines;
        parameters = param;
    }

    //TODO: DELETE / OVERWRITE HERE NEXT PARSER
    //I overloaded constructor purely for the purposes of parser 1
    public FunctionNode(String n, LinkedList<String> param){
        name = n;
        parameters = param;
    }

    private String name;

    private LinkedList<StatementNode> statements;

    private LinkedList<String> parameters;

    @Override
    public String toString() {
        return null;
    }
}
