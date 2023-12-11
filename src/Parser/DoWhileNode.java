package Parser;
import java.util.Optional;

public class DoWhileNode extends StatementNode{

    private Optional<Node> condition;

    public Optional<Node> getCondition(){
        return condition;
    }
    private BlockNode statements;

    public BlockNode getStatements(){
        return statements;
    }

    public DoWhileNode(Optional<Node> cond, BlockNode instruct){
        condition = cond;
        statements = instruct;
    }
}
