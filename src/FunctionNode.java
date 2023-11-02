import java.util.LinkedList;

public class FunctionNode extends Node{

    public FunctionNode(String n, LinkedList<StatementNode> lines, LinkedList<String> param){
        name = n;
        statements = lines;
        parameters = param;
    }

    public FunctionNode(String n, LinkedList<String> param){
        name = n;
        parameters = param;
    }

    public FunctionNode(){

    }

    private String name;

    public String getName(){
        return name;
    }

    private LinkedList<StatementNode> statements;

    private LinkedList<String> parameters;

    @Override
    public String toString() {
        return null;
    }
}
