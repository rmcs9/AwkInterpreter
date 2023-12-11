package Parser;
public class PatternNode extends Node{

    public PatternNode(String val){
        value = val;
    }

    private String value;

    public String getValue(){
        return value;
    }

    public String toString(){
        return "PatternNode";
    }
}
