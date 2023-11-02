public class TernaryNode extends Node{

    public TernaryNode(Node cond, Node tExp, Node fExp){
        condition = cond;
        truthExpression = tExp;
        falseExpression = fExp;
    }

    private Node condition;
    private Node truthExpression;
    private Node falseExpression;

    public Node getCondition(){
        return condition;
    }

    public Node getTruthExpression(){
        return truthExpression;
    }

    public Node getFalseExpression(){
        return falseExpression;
    }
    @Override
    public String toString(){
        return null;
    }
}
