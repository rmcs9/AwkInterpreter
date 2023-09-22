import java.util.LinkedList;
import java.util.Optional;

public class BlockNode extends Node{

    public BlockNode(){
    }
    private LinkedList<StatementNode> statements;

    public LinkedList<StatementNode> getStatements(){
        return this.statements;
    }

    private Optional<Node> condition;

    @Override
    public String toString() {
        return null;
    }
}
