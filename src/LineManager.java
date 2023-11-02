import java.util.HashMap;
import java.util.List;

public class LineManager {

    private List<String> lineList;

    public LineManager(List<String> l){
        lineList = l;
    }

    public boolean SplitAndAssign(HashMap<String, IDT> globals){
        if(lineList.isEmpty()) {
            return false;
        }
        String currentLine = lineList.remove(0);
        String[] splitLine = currentLine.split(globals.get("FS").getData());
        for(int i = 0; i < splitLine.length; i++){
            globals.put("$" + i, new IDT(splitLine[i]));
        }
        //setting NF
        globals.get("NF").setData(String.valueOf(splitLine.length));
        //incrementing NR and FNR
        globals.get("NR").setData(String.valueOf(Integer.parseInt(globals.get("NR").getData()) + 1));
        globals.get("FNR").setData(String.valueOf(Integer.parseInt(globals.get("FNR").getData()) + 1));
        return true;
    }
}
