package Interpreter;
public class ReturnType {


    private String returnVal;

    private returnType returnCause;

    public returnType getReturnCause(){
        return returnCause;
    }

    public String getReturnVal(){
        return returnVal;
    }
    public ReturnType(returnType cause){
        returnCause = cause;
        returnVal = "";
    }

    public ReturnType(returnType cause, String r){
        returnCause = cause;
        returnVal = r;
    }

    public enum returnType{
        NORMAL, BREAK, CONTINUE, RETURN
    }
}
