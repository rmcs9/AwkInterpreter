import java.util.Optional;

public class OperationNode extends Node{

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

    @Override
    public String toString() {
        return null;
    }

    public boolean isBooleanExpression(){
        switch(opType){
            case EQ:
            case NE:
            case LT:
            case LE:
            case GT:
            case GE:
            case MATCH:
            case NOTMATCH:
            case IN:
            case AND:
            case OR:
                return true;
            default:
                return false;
        }
    }
}
