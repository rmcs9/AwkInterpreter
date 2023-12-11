package Parser;
import java.util.Optional;

public class ReturnNode extends StatementNode{

    private Optional<Node> returnVal;

    public Optional<Node> getReturnVal(){
        return returnVal;
    }

    public ReturnNode(Optional<Node> ret){
        returnVal = ret;
    }
}
