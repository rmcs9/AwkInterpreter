public class ConstantNode extends Node{

    public ConstantNode(String val){
        value = val;
    }

    private String value;

    @Override
    public String toString() {
        return "constant Node";
    }
}
