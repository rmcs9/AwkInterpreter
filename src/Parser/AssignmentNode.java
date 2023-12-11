package Parser;
import java.util.Optional;

public class AssignmentNode extends StatementNode{

    public AssignmentNode(Node var, Optional<Node> value){
        leftside = var;
        rightside = value;
    }

    private Node leftside;

    private Optional<Node> rightside;

    public Node getLeftside(){
        return leftside;
    }

    public Optional<Node> getRightSide(){
        return rightside;
    }

    @Override
    public String toString() {
        return null;
    }
}
