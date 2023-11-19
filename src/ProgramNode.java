import java.util.LinkedList;

public class ProgramNode extends Node{

    public ProgramNode(){
        functions = new LinkedList<>();
        startBlocks = new LinkedList<>();
        endBlocks = new LinkedList<>();
        blocks = new LinkedList<>();
    }

    private LinkedList<FunctionNode> functions;

    public void addFunction(FunctionNode func){
        functions.add(func);
    }

    public LinkedList<FunctionNode> getFunctions(){
        return functions;
    }

    private LinkedList<BlockNode> startBlocks;

    public void addStartBlock(BlockNode block){
        startBlocks.add(block);
    }

    private LinkedList<BlockNode> endBlocks;

    public void addEndBlock(BlockNode block){
        endBlocks.add(block);
    }

    private LinkedList<BlockNode> blocks;

    public void addBlock(BlockNode block){
        blocks.add(block);
    }

    public LinkedList<BlockNode> getStartBlocks(){
        return startBlocks;
    }

    public LinkedList<BlockNode> getBlocks(){
        return blocks;
    }

    public LinkedList<BlockNode> getEndBlocks(){
        return endBlocks;
    }
    @Override
    public String toString() {
        return null;
    }
}
