package Interpreter;
import java.util.HashMap;
import java.util.List;

public class LineManager {

    private List<String> lineList;

    public List<String> getLineList(){
        return  lineList;
    }

    public LineManager(List<String> l){
        lineList = l;
    }

    public boolean SplitAndAssign(HashMap<String, IDT> globals){
        if(lineList.isEmpty()) {
            return false;
        }
        String currentLine = lineList.remove(0);
        globals.put("$0", new IDT(currentLine));
        String[] splitLine = currentLine.split(globals.get("FS").getData());
        for(int i = 0; i < splitLine.length; i++){
            globals.put("$" + (i + 1), new IDT(splitLine[i]));
        }
        //setting NF
        globals.put("NF",new IDT(String.valueOf(splitLine.length)));
        //incrementing NR and FNR
        if(globals.containsKey("NR")){
            globals.get("NR").setData(String.valueOf(Integer.parseInt(globals.get("NR").getData() + 1)));
        }
        else{
            globals.put("NR", new IDT("1"));
        }
        if(globals.containsKey("FNR")) {
            globals.get("FNR").setData(String.valueOf(Integer.parseInt(globals.get("FNR").getData()) + 1));
        }
        else{
            globals.put("FNR", new IDT("1"));
        }
        return true;
    }
}
