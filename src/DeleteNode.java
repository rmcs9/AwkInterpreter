import java.util.Optional;
import java.util.LinkedList;

public class DeleteNode extends StatementNode{

    private VariableReferenceNode array;

    private Optional<LinkedList> indexes;

    public DeleteNode(VariableReferenceNode arr, Optional<LinkedList> in){
        array = arr;
        indexes = in;
    }

}
