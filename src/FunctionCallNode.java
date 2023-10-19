import java.util.LinkedList;

public class FunctionCallNode extends StatementNode{

    private Node funcName;

    private LinkedList<Node> params;

    public FunctionCallNode(Node func, LinkedList<Node> par){
        funcName = func;
        params = par;
    }
}
