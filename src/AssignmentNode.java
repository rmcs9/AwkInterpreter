import java.util.Optional;

public class AssignmentNode extends StatementNode{

    public AssignmentNode(VariableReferenceNode var, Optional<Node> value){
        leftside = var;
        rightside = value;
    }

    private VariableReferenceNode leftside;

    private Optional<Node> rightside;

    @Override
    public String toString() {
        return null;
    }
}
