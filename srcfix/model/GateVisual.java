package model;

import view.UiScale;

import java.awt.*;
import java.util.*;
import java.util.List;

public class GateVisual {

    private static int nodeCounter = 1;
    private static int componentCounter = 1;

    public static void resetNodeCounter() { nodeCounter = 1; }
    public static int  peekNextNode()     { return nodeCounter; }
    public static void bumpCounterAbove(int maxId) {
        if (nodeCounter <= maxId) nodeCounter = maxId + 1;
    }
    public static void resetComponentCounter() { componentCounter = 1; }
    public static void bumpComponentCounterAbove(int maxId) {
        if (componentCounter <= maxId) componentCounter = maxId + 1;
    }

    private String type;
    private int componentId;
    private int x, y;

    public static final int WIDTH  = UiScale.scale(80);
    public static final int HEIGHT = UiScale.scale(60);

    private List<Integer> inputNodeIds = new ArrayList<>();
    private int           outputNodeId;

    private boolean isClosed = false;
    private boolean isOn     = false;

    // ── Public constructor (auto-assigns node IDs) ────────────────────────────
    public GateVisual(String type, int x, int y) {
        this.type = type;
        this.componentId = componentCounter++;
        this.x    = x;
        this.y    = y;
        switch (type) {
            case "NOT": case "LED":
                inputNodeIds.add(nodeCounter++);
                break;
            case "SWITCH":
                break; // no input terminals; output only
            default: // AND, OR, XOR – two inputs
                inputNodeIds.add(nodeCounter++);
                inputNodeIds.add(nodeCounter++);
                break;
        }
        outputNodeId = nodeCounter++;
    }

    // ── Private raw constructor used only by deserialize (no counter bump) ────
    private GateVisual() {}

    // ── Getters ───────────────────────────────────────────────────────────────
    public String        getType()          { return type; }
    public int           getComponentId()   { return componentId; }
    public int           getX()             { return x; }
    public int           getY()             { return y; }
    public int           getWidth()         { return WIDTH; }
    public int           getHeight()        { return HEIGHT; }
    public List<Integer> getInputNodeIds()  { return inputNodeIds; }
    public int           getOutputNodeId()  { return outputNodeId; }
    public boolean       isClosed()         { return isClosed; }
    public boolean       isOn()             { return isOn; }

    public void setComponentId(int componentId) { this.componentId = componentId; }
    public void setOn(boolean state)          { isOn = state; }
    public void setPosition(int x, int y)     { this.x = x; this.y = y; }
    public void setClosed(boolean closed)     { isClosed = closed; }

    public Rectangle getBounds() { return new Rectangle(x, y, WIDTH, HEIGHT); }

    public void toggleSwitch() { if (type.equals("SWITCH")) isClosed = !isClosed; }

    // ── Serialization ─────────────────────────────────────────────────────────
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append(type).append(",").append(componentId).append(",").append(x).append(",").append(y)
          .append(",").append(isClosed);
        for (int id : inputNodeIds) sb.append(",I").append(id);
        sb.append(",O").append(outputNodeId);
        return sb.toString();
    }

    public static GateVisual deserialize(String line) {
        String[] parts = line.split(",");
        GateVisual gv = new GateVisual();
        gv.type = parts[0];

        // Backward-compatible parser:
        // Old format: type,x,y,isClosed,...
        // New format: type,componentId,x,y,isClosed,...
        int base;
        if (parts.length > 4 && ("true".equals(parts[4]) || "false".equals(parts[4]))) {
            gv.componentId = Integer.parseInt(parts[1]);
            gv.x = Integer.parseInt(parts[2]);
            gv.y = Integer.parseInt(parts[3]);
            gv.isClosed = Boolean.parseBoolean(parts[4]);
            base = 5;
        } else {
            gv.componentId = componentCounter++;
            gv.x = Integer.parseInt(parts[1]);
            gv.y = Integer.parseInt(parts[2]);
            gv.isClosed = Boolean.parseBoolean(parts[3]);
            base = 4;
        }

        for (int i = base; i < parts.length; i++) {
            String tok = parts[i];
            if (tok.startsWith("I"))      gv.inputNodeIds.add(Integer.parseInt(tok.substring(1)));
            else if (tok.startsWith("O")) gv.outputNodeId = Integer.parseInt(tok.substring(1));
        }
        return gv;
    }
}