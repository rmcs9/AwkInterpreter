package Interpreter;
public class IDT {

    private String data;

    public IDT(String dat){
        data = dat;
    }

    public IDT(){
        data = "";
    }

    public String getData(){
        return data;
    }

    public void setData(String dat){
        try{
            float newDataFloat = Float.parseFloat(dat);
            if(newDataFloat == (int) newDataFloat){
                data = String.valueOf((int) newDataFloat);
            }
            else{
                data = dat;
            }
        }
        catch (NumberFormatException e) {
            data = dat;
        }
    }

}
