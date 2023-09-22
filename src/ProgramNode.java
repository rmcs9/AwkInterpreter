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

    @Override
    public String toString() {
        return null;
    }
}
