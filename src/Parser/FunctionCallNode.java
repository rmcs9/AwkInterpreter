package Parser;
import java.util.LinkedList;

public class FunctionCallNode extends StatementNode{

    private VariableReferenceNode funcName;

    private LinkedList<Node> params;

    public VariableReferenceNode getFuncName() {
        return funcName;
    }

    public LinkedList<Node> getParams(){
        return params;
    }
    public FunctionCallNode(VariableReferenceNode func, LinkedList<Node> par){
        funcName = func;
        params = par;
    }
}
