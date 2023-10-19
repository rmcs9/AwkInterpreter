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

    public Optional<Node> getRight(){
        return right;
    }

    @Override
    public String toString() {
        return null;
    }

}
