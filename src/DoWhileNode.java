import java.util.Optional;

public class DoWhileNode extends StatementNode{

    Optional<Node> condition;

    BlockNode statements;

    public DoWhileNode(Optional<Node> cond, BlockNode instruct){
        condition = cond;
        statements = instruct;
    }
}
