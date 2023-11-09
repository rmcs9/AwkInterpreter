import java.util.Optional;

public class IfNode extends StatementNode{

    public IfNode(Optional<Node> cond, BlockNode instruct){
        condition = cond;
        statements = instruct;
        next = Optional.empty();
    }

    public IfNode(Optional<Node> cond, BlockNode instruct, IfNode elsenode){
        condition = cond;
        statements = instruct;
        next = Optional.of(elsenode);
    }

    private Optional<Node> condition;

    private BlockNode statements;

    private Optional<IfNode> next;


    public Optional<Node> getCondition(){
        return condition;
    }

    public BlockNode getStatements(){
        return statements;
    }

    public Optional<IfNode> getNext(){
        return next;
    }

}
