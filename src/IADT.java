import java.util.HashMap;

public class IADT extends IDT {

    private HashMap<String, IDT> indexes;

    public HashMap<String, IDT> getIndexes(){
        return indexes;
    }

    public IADT(){
        indexes = new HashMap<>();
    }
}
