package Interpreter;
import Parser.FunctionNode;

import java.util.HashMap;
import java.util.function.Function;

public class BuiltInFunctionDefinitionNode extends FunctionNode {

    public Function<HashMap<String, IDT>, String> execute;

    public boolean isVariadic;

    public BuiltInFunctionDefinitionNode(Function<HashMap<String, IDT>, String> func, boolean isvar){
        execute = func;
        isVariadic = isvar;
    }
}
