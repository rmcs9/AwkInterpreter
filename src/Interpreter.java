import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Interpreter {

    /*
    LIST OF AWK GLOBALS:
    FS - File seperator: what the current file line tokens will be seperated with

    NR - Number of Records: total number of records processed across all input files.

    FNR - File Number of Records: record number within the current file (resets to 1 when a new file is started)

    OFMT - Output Format for numbers

    ORS - Output record seperator

    FILENAME - the current file being read

    NF - Number of fields (aka $ arguments)
     */

    private LineManager lmanager;

    private HashMap<String, IDT> globals;

    private HashMap<String, FunctionNode> functiondefs;


    public Interpreter(ProgramNode program, Optional<String> filepath) throws IOException {
        globals = new HashMap<>();
        functiondefs = new HashMap<>();

        if (filepath.isPresent()) {
            lmanager = new LineManager(Files.readAllLines(Paths.get(filepath.get())));
            globals.put("FILENAME", new IDT(filepath.get()));
        } else {
            lmanager = new LineManager(new LinkedList<>());
            globals.put("FILENAME", new IDT());
        }
        globals.put("FS", new IDT(" "));
        globals.put("OFMT", new IDT("%.6g"));
        globals.put("OFS", new IDT(" "));
        globals.put("ORS", new IDT("\n"));

        for (int i = 0; i < program.getFunctions().size(); i++) {
            functiondefs.put(program.getFunctions().get(i).getName(), program.getFunctions().get(i));
        }

        functiondefs.put("print", new BuiltInFunctionDefinitionNode(params -> {
            if(params.isEmpty()){
                System.out.print(globals.get("$0").getData());
                return null;
            }

            String[] printvals = new String[params.size()];
            for(int i = 0; i < params.size(); i++){
                printvals[i] = params.get(String.valueOf(i)).getData();
            }
            System.out.print(printvals);
            return null;
        }, true));


        functiondefs.put("printf", new BuiltInFunctionDefinitionNode(params -> {
            if(params.isEmpty()){
                throw new RuntimeException("expected at least 1 parameter format string with printf call");
            }

            if(params.size() > 1){
                String[] printvals = new String[params.size() - 1];
                for(int i = 1; i < params.size() - 1; i++){
                    printvals[i - 1] = params.get(String.valueOf(i)).getData();
                }
                System.out.printf(params.get("0").getData(), printvals);
            }
            else{
                System.out.printf(params.get("0").getData());
            }
            return null;
        }, true));


        functiondefs.put("getline", new BuiltInFunctionDefinitionNode(params -> {
            if(!params.isEmpty()){
                throw new RuntimeException("incorrect parameters passed to awk function getline. Expected 0, found " + params.size());
            }

            if(lmanager.SplitAndAssign(globals)){
                return "1";
            }
            return "0";
        }, false));


        functiondefs.put("next", new BuiltInFunctionDefinitionNode(params -> {
            if(!params.isEmpty()){
                throw new RuntimeException("incorrect parameters passed to awk function next. Expected 0, found " + params.size());
            }

            if(lmanager.SplitAndAssign(globals)){
                return "1";
            }

            return "0";
        },false));


        functiondefs.put("gsub", new BuiltInFunctionDefinitionNode(params ->{
            if(params.size() != 3){
                throw new RuntimeException("incorrect parameters passed to awk gsub function. Expected 3, found " + params.size());
            }

            IDT replace = params.get("0");
            IDT replaceWith = params.get("1");
            IDT str = params.get("2");

            Pattern pat = Pattern.compile(replace.getData());
            Matcher match = pat.matcher(str.getData());

            int numReplacements = 0;
            while(match.find()){
                numReplacements++;
            }

            str.setData(str.getData().replaceAll(replace.getData(), replaceWith.getData()));
            return String.valueOf(numReplacements);
        }, false));

        functiondefs.put("index", new BuiltInFunctionDefinitionNode(params -> {
            if(params.size() != 2){
                throw new RuntimeException("incorrect parameters passed to awk index function. Expected 2, found " + params.size());
            }

            IDT str = params.get("0");
            IDT find = params.get("1");

            Pattern pat = Pattern.compile(find.getData());
            Matcher match = pat.matcher(str.getData());

            if(match.find()){
                return String.valueOf(match.start() + 1);
            }
            return "0";
        }, false));


        functiondefs.put("length", new BuiltInFunctionDefinitionNode(params -> {
            if (params.size() != 1) {
                throw new RuntimeException("parameters mismatch in awk length function call. Expected 1, found " + params.size());
            }

            return String.valueOf(params.get("0").getData().length());
        }, false));


        functiondefs.put("match", new BuiltInFunctionDefinitionNode(params -> {
            if(params.size() != 2){
                throw new RuntimeException("incorrect parameters passed to awk match function. Expected 2, found: " + params.size());
            }

            IDT str = params.get("0");
            IDT find = params.get("1");

            Pattern pat = Pattern.compile(find.getData());
            Matcher match = pat.matcher(str.getData());

            if(match.find()){
                return String.valueOf(match.start());
            }
            return "0";
        }, false));


        functiondefs.put("split", new BuiltInFunctionDefinitionNode(params -> {
            if (params.size() > 3 || params.size() < 2) {
                throw new RuntimeException("parameters mismatch in awk split function call. Expected 2|3, found " + params.size());
            }

            if (!(params.get("1") instanceof IADT)) {
                throw new RuntimeException("array not present at parameter 2 in awk split call");
            }

            String fs = params.containsKey("2") ? params.get("2").getData() : " ";
            String[] splitString = params.get("0").getData().split(fs);
            for (int i = 0; i < splitString.length; i++) {
                ((IADT) params.get("1")).getIndexes().get(String.valueOf(i + 1)).setData(splitString[i]);
            }
            return null;
        }, false));

//        functiondefs.put("sprintf", new BuiltInFunctionDefinitionNode(params -> {}, false));

        functiondefs.put("sub", new BuiltInFunctionDefinitionNode(params -> {
            if(params.size() > 3 || params.size() < 2){
                throw new RuntimeException("incorrect parameters passed at awk sub function. Expected 2|3. found " + params.size());
            }

            IDT regex = params.get("0");
            IDT replaceWith = params.get("1");
            Pattern pat = Pattern.compile(regex.getData());

            IDT str = params.size() == 3 ? params.get("2") : globals.get("$0");

            Matcher match = pat.matcher(str.getData());
            str.setData(str.getData().replaceFirst(regex.getData(), replaceWith.getData()));

            if(match.find()){
                return "1";
            }
            return "0";
        }, false));


        functiondefs.put("substr", new BuiltInFunctionDefinitionNode(params -> {
            if (params.size() < 2 || params.size() > 3) {
                throw new RuntimeException("incorrect parameters passed to substr function. expected 2|3 found " + params.size());
            }
            try{
                if (params.size() == 2) {
                    return params.get("0").getData().substring(Integer.parseInt(params.get("1").getData()));
                } else {
                    return params.get("0").getData().substring(Integer.parseInt(params.get("1").getData()), Integer.parseInt(params.get("2").getData()));
                }
            }
            catch(NumberFormatException e){
                throw new RuntimeException("incorrect parameters passed to substr method. \n" + e);
            }
        }, false));


        functiondefs.put("tolower", new BuiltInFunctionDefinitionNode(params -> {
            if (params.size() != 1) {
                throw new RuntimeException("expected 1 parameter in tolower function call. found " + params.size());
            }
            return params.get("0").getData().toLowerCase();
        }, false));


        functiondefs.put("toupper", new BuiltInFunctionDefinitionNode(params -> {
            if (params.size() != 1) {
                throw new RuntimeException("expeceted 1 parameter in toupper function call. found " + params.size());
            }
            return params.get("0").getData().toUpperCase();
        }, false));
    }






    public IDT getIDT(Node current, Optional<HashMap<String, IDT>> locals){
        if(current instanceof AssignmentNode){
            AssignmentNode assign = (AssignmentNode) current;
            IDT val = getIDT(assign.getRightSide().get(), locals);
            if(assign.getLeftside() instanceof VariableReferenceNode){
                VariableReferenceNode var = (VariableReferenceNode) assign.getLeftside();
                if(locals.isPresent()){
                    if(!locals.get().containsKey(var.getVariableName())){
                        if(var.getArrayIndex().isPresent()){
                            locals.get().put(var.getVariableName(), new IADT());
                        }
                        else {
                            locals.get().put(var.getVariableName(), new IDT("0"));
                        }
                    }
                }
                else{
                    if(!globals.containsKey(var.getVariableName())){
                        if(var.getArrayIndex().isPresent()){
                            globals.put(var.getVariableName(), new IADT());
                        }
                        else {
                            globals.put(var.getVariableName(), new IDT("0"));
                        }
                    }
                }
                IDT target = getIDT(assign.getLeftside(), locals);
                target.setData(val.getData());
                return val;
            }
            else if(assign.getLeftside() instanceof OperationNode
                    && ((OperationNode) assign.getLeftside()).getOpType() == OperationNode.operationType.DOLLAR){
                IDT target = getIDT(assign.getLeftside(), locals);
                target.setData(val.getData());
                return val;
            }
            else{
                throw new RuntimeException("left side of assignment must be a variable or field reference");
            }
        }
        else if(current instanceof FunctionCallNode){
            return new IDT(runFunctionCall((FunctionCallNode) current, locals));
        }
        else if(current instanceof ConstantNode){
            return new IDT(((ConstantNode) current).getValue());
        }
        else if(current instanceof PatternNode){
            throw new RuntimeException("cannot interpret pattern");
        }
        else if(current instanceof TernaryNode){
            IDT bool = getIDT(((TernaryNode) current).getCondition(), locals);
            boolean terCond;
            try{
                terCond = Float.parseFloat(bool.getData()) != 0;
            }
            catch(NumberFormatException e){
                terCond = false;
            }
            if(terCond){
                return getIDT(((TernaryNode) current).getTruthExpression(), locals);
            }
            return getIDT(((TernaryNode) current).getFalseExpression(), locals);
        }
        else if(current instanceof VariableReferenceNode){
            VariableReferenceNode currentVar = (VariableReferenceNode) current;
            IDT arrayInd = currentVar.getArrayIndex().isPresent() ? getIDT(currentVar.getArrayIndex().get(), locals) : null;
            if(locals.isPresent() && locals.get().containsKey(currentVar.getVariableName())){
                if(arrayInd != null){
                    if(locals.get().get(currentVar.getVariableName()) instanceof IADT){
                        if(!((IADT) locals.get().get(currentVar.getVariableName())).getIndexes().containsKey(arrayInd.getData())){
                            ((IADT) locals.get().get(currentVar.getVariableName())).getIndexes().put(arrayInd.getData(), new IDT("0"));
                        }
                        return ((IADT) locals.get().get(currentVar.getVariableName())).getIndexes().get(arrayInd.getData());
                    }
                    throw new RuntimeException("attempting to reference variable " + currentVar.getVariableName() + " as an array");
                }
                return locals.get().get(currentVar.getVariableName());
            }
            else if(globals.containsKey(currentVar.getVariableName())){
                if(arrayInd != null){
                    if(globals.get(currentVar.getVariableName()) instanceof IADT){
                        if(!((IADT) globals.get(currentVar.getVariableName())).getIndexes().containsKey(arrayInd.getData())){
                            ((IADT) globals.get(currentVar.getVariableName())).getIndexes().put(arrayInd.getData(), new IDT("0"));
                        }
                        return ((IADT) globals.get(currentVar.getVariableName())).getIndexes().get(arrayInd.getData());
                    }
                    throw new RuntimeException("attempting to reference variable " + currentVar.getVariableName() + " as an array");
                }
                return globals.get(currentVar.getVariableName());
            }
            else{
                throw new RuntimeException("unable to find variable reference " + currentVar.getVariableName());
            }
        }
        else if(current instanceof OperationNode){
            OperationNode currentOP = (OperationNode) current;


            float leftFloat, rightFloat;
            if(currentOP.isMath()){
                IDT left = getIDT(currentOP.getLeft(), locals);
                IDT right;
                if(currentOP.getRight().isPresent()){
                    right = getIDT(currentOP.getRight().get(), locals);
                }
                else{
                    throw new RuntimeException("right side not found in mathmatical operation");
                }
                try{
                    leftFloat = Float.parseFloat(left.getData());
                    rightFloat = Float.parseFloat(right.getData());
                }
                catch(NumberFormatException e){
                    throw new RuntimeException("math operation failed. attempting math operation on data that is not numerical \n" + e);
                }

                switch(currentOP.getOpType()){
                    case ADD:
                        return new IDT(String.valueOf(leftFloat + rightFloat));
                    case SUBTRACT:
                        return new IDT(String.valueOf(leftFloat - rightFloat));
                    case MULTIPLY:
                        return new IDT(String.valueOf(leftFloat * rightFloat));
                    case DIVIDE:
                        return new IDT(String.valueOf(leftFloat / rightFloat));
                    case MODULO:
                        return new IDT(String.valueOf(leftFloat % rightFloat));
                    case EXPONENT:
                        return new IDT(String.valueOf(Math.pow(leftFloat, rightFloat)));
                }
                throw new RuntimeException("error at math operation");
            }
            else if(currentOP.isCompare()){
                IDT left = getIDT(currentOP.getLeft(), locals);
                IDT right;
                if(currentOP.getRight().isPresent()){
                    right = getIDT(currentOP.getRight().get(), locals);
                }
                else{
                    throw new RuntimeException("right side not found in boolean operation");
                }
                int returnVal = -1;
                try{
                    leftFloat = Float.parseFloat(left.getData());
                    rightFloat = Float.parseFloat(right.getData());

                    switch (currentOP.getOpType()){
                        case EQ:
                            returnVal = leftFloat == rightFloat ? 1 : 0;
                            break;
                        case NE:
                            returnVal = leftFloat != rightFloat ? 1 : 0;
                            break;
                        case LT:
                            returnVal = leftFloat < rightFloat ? 1 : 0;
                            break;
                        case LE:
                            returnVal = leftFloat <= rightFloat ? 1 : 0;
                            break;
                        case GT:
                            returnVal = leftFloat > rightFloat ? 1 : 0;
                            break;
                        case GE:
                            returnVal = leftFloat >= rightFloat ? 1 : 0;
                            break;
                    }
                }
                catch (NumberFormatException e){
                    int stringComp = left.getData().compareTo(right.getData());
                    switch (currentOP.getOpType()){
                        case EQ:
                            returnVal = stringComp == 0 ? 1 : 0;
                            break;
                        case NE:
                            returnVal = stringComp != 0 ? 1 : 0;
                            break;
                        case LT:
                            returnVal = stringComp < 0 ? 1 : 0;
                            break;
                        case LE:
                            returnVal = stringComp <= 0 ? 1 : 0;
                            break;
                        case GT:
                            returnVal = stringComp > 0 ? 1 : 0;
                            break;
                        case GE:
                            returnVal = stringComp >= 0 ? 1 : 0;
                    }
                }
                return new IDT(String.valueOf(returnVal));
            }
            else if(currentOP.isBoolean()){
                IDT left = getIDT(currentOP.getLeft(), locals);
                IDT right;
                if(currentOP.getRight().isPresent()){
                    right = getIDT(currentOP.getRight().get(), locals);
                }
                else{
                    throw new RuntimeException("right side not found in boolean operation");
                }
                boolean leftCond, rightCond;

                try{
                   leftCond = Float.parseFloat(left.getData()) != 0;
                }
                catch(NumberFormatException e){
                    leftCond = false;
                }

                try{
                    rightCond = Float.parseFloat(right.getData()) != 0;
                }
                catch (NumberFormatException e){
                    rightCond = false;
                }

                if(currentOP.getOpType() == OperationNode.operationType.AND){
                    if(leftCond && rightCond){
                        return new IDT("1");
                    }
                    return new IDT("0");
                }
                else{
                    if(leftCond || rightCond){
                        return new IDT("1");
                    }
                    return new IDT("0");
                }
            }
            else if(currentOP.isIncDecUnary()){

                if(currentOP.getOpType() != OperationNode.operationType.UNARYPOS && currentOP.getOpType() != OperationNode.operationType.UNARYNEG){
                    if(currentOP.getLeft() instanceof VariableReferenceNode){
                        VariableReferenceNode var = (VariableReferenceNode) currentOP.getLeft();
                        if(locals.isPresent()){
                            if(!locals.get().containsKey(var.getVariableName())){
                                if(var.getArrayIndex().isPresent()){
                                    locals.get().put(var.getVariableName(), new IADT());
                                }
                                else {
                                    locals.get().put(var.getVariableName(), new IDT("0"));
                                }
                            }
                        }
                        else{
                            if(!globals.containsKey(var.getVariableName())){
                                if(var.getArrayIndex().isPresent()){
                                    globals.put(var.getVariableName(), new IADT());
                                }
                                else{
                                    globals.put(var.getVariableName(), new IDT("0"));
                                }
                            }
                        }
                    }
                    else{
                        throw new RuntimeException("cannot apply inc/dec operation to non var argument");
                    }
                }
                IDT left = getIDT(currentOP.getLeft(),locals);

                switch(currentOP.getOpType()){
                    case PREDEC:
                        if(!(currentOP.getLeft() instanceof  VariableReferenceNode)){
                            throw new RuntimeException("cannot apply predec to non var argument");
                        }
                        try{
                            leftFloat = Float.parseFloat(left.getData()) - 1;
                            left.setData(String.valueOf(leftFloat));
                            return left;
                        }
                        catch(NumberFormatException e){
                            throw new RuntimeException("attempting to apply predecrement operator to a variable that is not numerical");
                        }
                    case POSTDEC:
                        if(!(currentOP.getLeft() instanceof  VariableReferenceNode)){
                            throw new RuntimeException("cannot apply postdec to non var argument");
                        }
                        try{
                            leftFloat = Float.parseFloat(left.getData()) - 1;
                            IDT leftcopy = new IDT(left.getData());

                            left.setData(String.valueOf(leftFloat));
                            return leftcopy;
                        }
                        catch (NumberFormatException e){
                            throw new RuntimeException("attempting to apply postdecrement operator to a variable that is not numerical");
                        }
                    case PREINC:
                        if(!(currentOP.getLeft() instanceof  VariableReferenceNode)){
                            throw new RuntimeException("cannot apply preinc to non var argument");
                        }
                        try{
                           leftFloat = Float.parseFloat(left.getData()) + 1;
                           left.setData(String.valueOf(leftFloat));
                           return left;
                        }
                        catch (NumberFormatException e){
                            throw new RuntimeException("attempting to apply preincrement operator to a variable that is not numerical");
                        }
                    case POSTINC:
                        if(!(currentOP.getLeft() instanceof  VariableReferenceNode)){
                            throw new RuntimeException("cannot apply postinc to non var argument");
                        }
                        try{
                            leftFloat = Float.parseFloat(left.getData()) + 1;
                            IDT leftcopy = new IDT(left.getData());

                            left.setData(String.valueOf(leftFloat));
                            return leftcopy;
                        }
                        catch (NumberFormatException e){
                            throw new RuntimeException("attempting to apply postincrement operator to a variable that is not numerical");
                        }
                    case UNARYPOS:
                        return left;
                    case UNARYNEG:
                        try{
                            leftFloat = Float.parseFloat(left.getData());
                            leftFloat = leftFloat - (leftFloat * 2);
                            return new IDT(String.valueOf(leftFloat));
                        }
                        catch (NumberFormatException e){
                            throw new RuntimeException("attempting to apply unary negation operator '-' to var " +
                                    "or expression that is not numerical");
                        }
                }
            }
            else if(currentOP.isMatch()){
                IDT left = getIDT(currentOP.getLeft(), locals);
                if(!currentOP.getRight().isPresent()){
                    throw new RuntimeException("right side not present in match expression");
                }
                if(!(currentOP.getRight().get() instanceof PatternNode)){
                    throw new RuntimeException("right side of match expression must be a pattern");
                }
                Pattern pat = Pattern.compile(((PatternNode) currentOP.getRight().get()).getValue());
                Matcher match = pat.matcher(left.getData());

                if(currentOP.getOpType() == OperationNode.operationType.MATCH) {
                    if (match.find()) {
                        return new IDT("1");
                    }
                    return new IDT("0");
                }

                if(match.find()){
                    return new IDT("0");
                }
                return new IDT("1");
            }
            else if(currentOP.getOpType() == OperationNode.operationType.DOLLAR){
                IDT left = getIDT(currentOP.getLeft(), locals);
                try{
                    leftFloat = Float.parseFloat(left.getData());
                }
                catch(NumberFormatException e){
                    throw new RuntimeException("attempting to access a field reference with a expression or variable that is not numerical");
                }
                return globals.get("$" + left.getData());
            }
            else if(currentOP.getOpType() == OperationNode.operationType.IN){
                IDT left = getIDT(currentOP.getLeft(), locals);
                if(!currentOP.getRight().isPresent()){
                    throw new RuntimeException("right side of in operation is not present");
                }

                if(currentOP.getRight().get() instanceof VariableReferenceNode){
                    VariableReferenceNode rightVar = (VariableReferenceNode) currentOP.getRight().get();
                    HashMap<String, IDT> foundLocOrGlob;
                    if(locals.isPresent() && locals.get().containsKey(rightVar.getVariableName())){
                        foundLocOrGlob = locals.get();
                    }
                    else if(globals.containsKey(((VariableReferenceNode) currentOP.getRight().get()).getVariableName())){
                        foundLocOrGlob = globals;
                    }
                    else{
                        throw new RuntimeException("could not find array variable on right side of in expression");
                    }

                    if(foundLocOrGlob.get(rightVar.getVariableName()) instanceof IADT){
                        if(((IADT) foundLocOrGlob.get(rightVar.getVariableName())).getIndexes().containsKey(left.getData())){
                            return ((IADT) foundLocOrGlob.get(rightVar.getVariableName())).getIndexes().get(left.getData());
                        }
                        else{
                            throw new RuntimeException("index " + left.getData() + " not found in " + rightVar.getVariableName());
                        }
                    }
                    else{
                        throw new RuntimeException("right side of in expression is not an array");
                    }
                }
                else{
                    throw new RuntimeException("right side of in expression must be a reference to an array");
                }
            }
            else if(currentOP.getOpType() == OperationNode.operationType.CONCATENATION){
                IDT left = getIDT(currentOP.getLeft(),locals);
                if(currentOP.getRight().isPresent()){
                    IDT right = getIDT(currentOP.getRight().get(), locals);
                    return new IDT(left.getData() + right.getData());
                }
                else{
                    throw new RuntimeException("no right side found in string concatenation expression");
                }
            }
            else{
                throw new RuntimeException("Operation type " + currentOP.getOpType() + " is not valid somehow. how did u even get here???");
            }
        }
        throw new RuntimeException("invalid node found when attempting to interpret statement");
    }

    private String runFunctionCall(FunctionCallNode call, Optional<HashMap<String, IDT>> locals){
        return "";
    }
}