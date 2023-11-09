

public class ForEachNode extends StatementNode{

    private OperationNode arrayExp;

    private BlockNode statements;

    public OperationNode getArrayExp(){
        return arrayExp;
    }

    public BlockNode getStatements(){
        return statements;
    }
    public ForEachNode(OperationNode exp, BlockNode instruct){
        arrayExp = exp;
        statements = instruct;
    }
}
