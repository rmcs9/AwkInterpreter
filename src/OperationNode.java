import java.util.Optional;

public class OperationNode extends StatementNode{

    public OperationNode(Node l, operationType op){
        left = l;
        opType = op;
    }

    public OperationNode(Node l, Node r, operationType op){
        left = l;
        right = Optional.of(r);
        opType = op;
    }

    private Node left;

    private Optional<Node> right;

    private operationType opType;

    public operationType getOpType(){
        return opType;
    }

    public enum operationType{
        EQ, NE, LT, LE, GT, GE, AND, OR, NOT, MATCH, NOTMATCH, DOLLAR,
        PREINC, POSTINC, PREDEC, POSTDEC, UNARYPOS, UNARYNEG, IN,
        EXPONENT, ADD, SUBTRACT, MULTIPLY, DIVIDE, MODULO, CONCATENATION
    }

    public boolean isMath(){
        return opType == operationType.EXPONENT
                || opType == operationType.ADD
                || opType == operationType.SUBTRACT
                || opType == operationType.MULTIPLY
                || opType == operationType.DIVIDE
                || opType == operationType.MODULO;
    }

    public boolean isCompare(){
        return opType == operationType.EQ
                || opType == operationType.NE
                || opType == operationType.LT
                || opType ==operationType.LE
                || opType ==operationType.GT
                || opType ==operationType.GE;
    }

    public boolean isBoolean(){
        return opType == operationType.AND || opType == operationType.OR;
    }
    public boolean isMatch(){
        return opType == operationType.MATCH || opType == operationType.NOTMATCH;
    }

    public boolean isIncDecUnary(){
        return opType == operationType.PREDEC
                || opType == operationType.POSTDEC
                || opType == operationType.PREINC
                || opType == operationType.POSTINC
                || opType == operationType.UNARYPOS
                || opType == operationType.UNARYNEG;
    }
    public Node getLeft(){
        return left;
    }

    public Optional<Node> getRight(){
        return right;
    }

    @Override
    public String toString() {
        return null;
    }

}
