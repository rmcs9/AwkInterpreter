import java.util.Optional;

public class VariableReferenceNode extends Node{

    public VariableReferenceNode(String name, Optional<Node> op){
        variableName = name;
        arrayIndex = op;
    }
    private String variableName;

    private Optional<Node> arrayIndex;
    @Override
    public String toString() {
        return null;
    }
}
