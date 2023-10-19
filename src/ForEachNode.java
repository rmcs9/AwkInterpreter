

public class ForEachNode extends StatementNode{

    private OperationNode arrayExp;

    private BlockNode statements;

    public ForEachNode(OperationNode exp, BlockNode instruct){
        arrayExp = exp;
        statements = instruct;
    }
}
