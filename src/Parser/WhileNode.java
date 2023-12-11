package Parser;
import java.util.Optional;

public class WhileNode extends StatementNode{

    private Optional<Node> condition;

    private BlockNode statements;

    public Optional<Node> getCondition(){
        return condition;
    }

    public BlockNode getStatements(){
        return  statements;
    }

    public WhileNode(Optional<Node> cond, BlockNode instruct){
        condition = cond;
        statements = instruct;
    }
}
