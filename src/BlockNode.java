import java.util.LinkedList;
import java.util.Optional;

public class BlockNode extends Node{

    public BlockNode(){
        statements = new LinkedList<>();
        condition = Optional.empty();
    }
    private LinkedList<StatementNode> statements;
    private Optional<Node> condition;

    public void addCondition(Optional<Node> cond){
        condition = cond;
    }

    public LinkedList<StatementNode> getStatements(){
        return this.statements;
    }

    public Optional<Node> getCondition(){
        return condition;
    }
    public void addStatement(StatementNode node){
        this.statements.add(node);
    }

    @Override
    public String toString() {
        return null;
    }
}
