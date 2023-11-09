public class ForNode extends StatementNode{

    private Node initialization;

    private Node condition;

    private Node operation;

    private BlockNode statements;

    public Node getInitialization(){
        return  initialization;
    }

    public Node getCondition(){
        return condition;
    }

    public Node getOperation(){
        return operation;
    }

    public BlockNode getStatements(){
        return statements;
    }

    public ForNode(Node init, Node cond, Node op, BlockNode instruct){
        initialization = init;
        condition = cond;
        operation = op;
        statements = instruct;
    }


}
