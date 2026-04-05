package model;

public class Wire {
    private int fromNode;
    private int toNode;

    public Wire(int fromNode, int toNode) {
        this.fromNode = fromNode;
        this.toNode = toNode;
    }

    public int getFromNode() { return fromNode; }
    public int getToNode()   { return toNode; }

    @Override
    public String toString() {
        return fromNode + "->" + toNode;
    }
}
