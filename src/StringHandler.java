public class StringHandler {

    private final String filestring;

    public StringHandler(String input){
        filestring = input;
    }

    private int index = 0;

    public char peek(int i){
        return filestring.charAt(index + i);
    }

    public String peekString(int i){
        return filestring.substring(index, i);
    }

    public char getChar(){
        index++;
        return filestring.charAt(index - 1);
    }

    public void swallow(int i){
        index = index + i;
    }

    public boolean isDone(){
        return index >= filestring.length();
    }

    public String remainder(){
        return filestring.substring(index);
    }
}
