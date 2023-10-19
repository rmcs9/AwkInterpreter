import java.util.Optional;

public class ReturnNode extends StatementNode{

    private Optional<Node> returnVal;

    public ReturnNode(Optional<Node> ret){
        returnVal = ret;
    }
}
