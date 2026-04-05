package view;

import controller.CircuitEngine;
import model.GateVisual;
import model.Wire;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class CircuitPanel extends JPanel {

    // ── State ─────────────────────────────────────────────────────────────────
    private final List<GateVisual> gates = new ArrayList<>();
    private final List<Wire>       wires = new ArrayList<>();

    private String     selectedTool  = null;
    private GateVisual selectedGate  = null;
    private GateVisual draggingGate  = null;
    private int        dragOffX, dragOffY;

    private Map<Integer,Integer> manualSignals  = new HashMap<>();
    private Map<Integer,Integer> lastSimResult  = new HashMap<>();
    private final Map<Integer, Point> junctionNodePositions = new HashMap<>();

    private Integer pendingWireFromNode = null;
    private Point pendingWireMouse = null;

    private static final int PORT_HIT_RADIUS = 9;

    private final StatusPanel statusPanel;

    // ── Constructor ───────────────────────────────────────────────────────────
    public CircuitPanel(StatusPanel sp) {
        this.statusPanel = sp;
        setBackground(new Color(25, 25, 35));
        setFocusable(true);

        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE)  deleteSelected();
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE)  {
                    selectedTool = null;
                    selectedGate = null;
                    cancelPendingWire();
                    repaint();
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                requestFocusInWindow();

                // Right-click → remove wire under cursor
                if (SwingUtilities.isRightMouseButton(e)) {
                    if (pendingWireFromNode != null) {
                        cancelPendingWire();
                        statusPanel.setStatusWarn("Wire creation canceled.");
                        repaint();
                        return;
                    }
                    Wire hit = wireAt(e.getPoint());
                    if (hit != null) { wires.remove(hit); runSimulation(); repaint(); }
                    return;
                }

                // While creating a wire: finish on a port or an existing wire.
                if (pendingWireFromNode != null) {
                    Integer targetPort = portAt(e.getPoint());
                    if (targetPort != null) {
                        if (targetPort.equals(pendingWireFromNode)) {
                            cancelPendingWire();
                            statusPanel.setStatusWarn("Wire creation canceled.");
                            repaint();
                            return;
                        }
                        if (tryConnectNodes(pendingWireFromNode, targetPort, true)) {
                            cancelPendingWire();
                        }
                        repaint();
                        return;
                    }

                    Wire hitWire = wireAt(e.getPoint());
                    if (hitWire != null) {
                        Integer junctionNode = createOrReuseJunctionOnWire(hitWire, e.getPoint());
                        if (junctionNode != null && tryConnectNodes(pendingWireFromNode, junctionNode, true)) {
                            cancelPendingWire();
                        }
                        runSimulation();
                        repaint();
                        return;
                    }

                    pendingWireMouse = e.getPoint();
                    repaint();
                    return;
                }

                // Start wire by clicking a port.
                Integer clickedPort = portAt(e.getPoint());
                if (clickedPort != null) {
                    pendingWireFromNode = clickedPort;
                    pendingWireMouse = e.getPoint();
                    selectedTool = null;
                    draggingGate = null;
                    selectedGate = null;
                    statusPanel.setStatusOk("Wire started from node " + clickedPort + ". Click a port or wire to connect.");
                    repaint();
                    return;
                }

                // Left-click on an existing gate
                for (int i = gates.size() - 1; i >= 0; i--) {
                    GateVisual g = gates.get(i);
                    if (!g.getBounds().contains(e.getPoint())) continue;

                    if (g.getType().equals("SWITCH")) {
                        g.toggleSwitch();
                        runSimulation();
                        repaint();
                        return;
                    }
                    selectedGate = g;
                    draggingGate = g;
                    dragOffX = e.getX() - g.getX();
                    dragOffY = e.getY() - g.getY();
                    repaint();
                    return;
                }

                // Left-click on empty canvas → place gate if tool active
                if (selectedTool != null) {
                    GateVisual gv = new GateVisual(selectedTool,
                            e.getX() - GateVisual.WIDTH  / 2,
                            e.getY() - GateVisual.HEIGHT / 2);
                    gates.add(gv);
                    selectedTool = null;
                    selectedGate = gv;
                    runSimulation();
                    repaint();
                    statusPanel.setStatusOk("Placed " + gv.getType()
                            + " — inputs: " + gv.getInputNodeIds()
                            + "  output: " + gv.getOutputNodeId());
                } else {
                    selectedGate = null;
                    repaint();
                }
            }
            @Override public void mouseReleased(MouseEvent e) { draggingGate = null; }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                if (draggingGate != null) {
                    draggingGate.setPosition(e.getX() - dragOffX, e.getY() - dragOffY);
                    repaint();
                    return;
                }
                if (pendingWireFromNode != null) {
                    pendingWireMouse = e.getPoint();
                    repaint();
                }
            }

            @Override public void mouseMoved(MouseEvent e) {
                if (pendingWireFromNode != null) {
                    pendingWireMouse = e.getPoint();
                    repaint();
                }
            }
        });
    }

    // ── Public API ────────────────────────────────────────────────────────────
    public void setSelectedTool(String tool) {
        selectedTool = tool;
        selectedGate = null;
        statusPanel.setStatusOk("Click canvas to place " + tool);
        repaint();
    }

    public void connectNodes(int n1, int n2) {
        tryConnectNodes(n1, n2, true);
    }

    private boolean tryConnectNodes(int n1, int n2, boolean showErrors) {
        if (findNodePos(n1) == null) {
            showConnectError("Node " + n1 + " not found on canvas.", showErrors);
            return false;
        }
        if (findNodePos(n2) == null) {
            showConnectError("Node " + n2 + " not found on canvas.", showErrors);
            return false;
        }

        int from = n1;
        int to = n2;
        boolean n1Source = isOutputNode(n1) || isJunctionNode(n1);
        boolean n1Sink = isInputNode(n1) || isJunctionNode(n1);
        boolean n2Source = isOutputNode(n2) || isJunctionNode(n2);
        boolean n2Sink = isInputNode(n2) || isJunctionNode(n2);

        if (n1Source && n2Sink) {
            // keep direction
        } else if (n2Source && n1Sink) {
            from = n2;
            to = n1;
        } else {
            showConnectError("Connect an output/junction node to an input/junction node.", showErrors);
            return false;
        }

        for (Wire w : wires) if ((w.getFromNode() == from && w.getToNode() == to)
                || (w.getFromNode() == to && w.getToNode() == from)) {
            statusPanel.setStatusWarn("Wire " + from + "→" + to + " already exists.");
            return false;
        }

        wires.add(new Wire(from, to));
        runSimulation();
        repaint();
        statusPanel.setStatusOk("Connected " + from + " → " + to);
        return true;
    }

    public void applyManualSignals(Map<Integer,Integer> signals) {
        manualSignals = signals;
        runSimulation();
        repaint();
        statusPanel.setStatusOk("Signals applied: " + signals);
    }

    public void deleteSelected() {
        if (selectedGate == null) return;
        int out = selectedGate.getOutputNodeId();
        List<Integer> ins = selectedGate.getInputNodeIds();
        wires.removeIf(w -> w.getFromNode() == out || w.getToNode() == out
                         || ins.contains(w.getFromNode()) || ins.contains(w.getToNode()));
        gates.remove(selectedGate);
        selectedGate = null;
        runSimulation();
        repaint();
        statusPanel.setStatusOk("Component deleted.");
    }

    public void clearCanvas() {
        gates.clear();
        wires.clear();
        junctionNodePositions.clear();
        manualSignals.clear();
        lastSimResult.clear();
        selectedGate = null;
        cancelPendingWire();
        GateVisual.resetNodeCounter();
        GateVisual.resetComponentCounter();
        repaint();
        statusPanel.setStatusOk("Canvas cleared.");
    }

    public List<GateVisual> getGates()          { return gates; }
    public List<Wire>       getWires()           { return wires; }
    public Map<Integer,Integer> getManualSignals() { return new HashMap<>(manualSignals); }
    public Point getNodePosition(int nodeId) { return findNodePos(nodeId); }

    public void loadCircuit(List<GateVisual> g, List<Wire> w, Map<Integer,Integer> signals) {
        gates.clear(); gates.addAll(g);
        wires.clear(); wires.addAll(w);
        junctionNodePositions.clear();
        manualSignals.clear();
        if (signals != null) manualSignals.putAll(signals);
        cancelPendingWire();
        runSimulation();
        repaint();
    }

    public String        verify()             { return CircuitEngine.verify(gates, wires); }
    public List<String[]> generateTruthTable() { return CircuitEngine.truthTable(gates, wires); }

    // ── Simulation ────────────────────────────────────────────────────────────
    private void runSimulation() {
        Map<Integer,Integer> result = CircuitEngine.simulate(gates, wires, new HashMap<>(manualSignals));
        if (result == null) {
            lastSimResult = new HashMap<>();
            for (GateVisual g : gates) g.setOn(false);
            statusPanel.setStatusError("Short circuit detected!");
            return;
        }
        lastSimResult = result;
        for (GateVisual g : gates) {
            if (g.getType().equals("LED")) {
                g.setOn(getLedSignal(g, result) == 1);
            }
        }
    }

    // ── Painting ──────────────────────────────────────────────────────────────
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // Subtle grid
        g2.setColor(new Color(38, 38, 52));
        for (int gx = 0; gx < getWidth();  gx += 28)
            for (int gy = 0; gy < getHeight(); gy += 28)
                g2.fillRect(gx, gy, 1, 1);

        for (Wire wire : wires)   drawWire(g2, wire);
        for (GateVisual gate : gates) drawGate(g2, gate);

        if (pendingWireFromNode != null && pendingWireMouse != null) {
            Point from = findNodePos(pendingWireFromNode);
            if (from != null) {
                g2.setColor(new Color(120, 180, 255));
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(from.x, from.y, pendingWireMouse.x, pendingWireMouse.y);
                g2.fillOval(from.x - 5, from.y - 5, 10, 10);
            }
        }
    }

    // ── Wire ──────────────────────────────────────────────────────────────────
    private void drawWire(Graphics2D g2, Wire wire) {
        Point p1 = findNodePos(wire.getFromNode());
        Point p2 = findNodePos(wire.getToNode());
        if (p1 == null || p2 == null) return;

        Integer sig = lastSimResult.get(wire.getFromNode());
        if (sig == null) sig = lastSimResult.get(wire.getToNode());
        boolean high = (sig != null && sig == 1);
        Color wireColor = high ? new Color(0, 210, 90) : new Color(90, 90, 120);

        g2.setColor(wireColor);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Orthogonal routing: horizontal then vertical then horizontal
        int midX = (p1.x + p2.x) / 2;
        g2.drawLine(p1.x, p1.y, midX, p1.y);
        g2.drawLine(midX, p1.y, midX, p2.y);
        g2.drawLine(midX, p2.y, p2.x, p2.y);

        // Junction dots
        g2.setColor(wireColor.brighter());
        g2.fillOval(p1.x - 3, p1.y - 3, 6, 6);
        g2.fillOval(p2.x - 3, p2.y - 3, 6, 6);
    }

    // ── Gate ─────────────────────────────────────────────────────────────────
    private void drawGate(Graphics2D g2, GateVisual gate) {
        int x = gate.getX(), y = gate.getY();
        int w = gate.getWidth(), h = gate.getHeight();
        boolean sel = (gate == selectedGate);

        Color body = sel ? new Color(0, 190, 230) : new Color(180, 180, 205);
        Color fill = new Color(42, 42, 58);

        g2.setStroke(new BasicStroke(2f));

        switch (gate.getType()) {
            case "AND":    drawAND   (g2, x, y, w, h, body, fill); break;
            case "OR":     drawOR    (g2, x, y, w, h, body, fill); break;
            case "NOT":    drawNOT   (g2, x, y, w, h, body, fill); break;
            case "XOR":    drawXOR   (g2, x, y, w, h, body, fill); break;
            case "SWITCH": drawSWITCH(g2, x, y, w, h, gate, sel); break;
            case "LED":    drawLED   (g2, x, y, h,     gate, sel); break;
        }

        // Input terminal stubs + node labels (not for LED/SWITCH — they draw their own)
        if (!gate.getType().equals("LED") && !gate.getType().equals("SWITCH")) {
            List<Integer> inputIds = gate.getInputNodeIds();
            int spacing = h / (inputIds.size() + 1);
            for (int i = 0; i < inputIds.size(); i++) {
                int nodeId = inputIds.get(i);
                int ny = y + spacing * (i + 1);
                int nx = x - 20;
                drawTerminal(g2, nodeId, nx, ny, nx, ny, x, ny, false);
            }
        }

        // Output terminal (not for LED)
        if (!gate.getType().equals("LED")) {
            int outId = gate.getOutputNodeId();
            int outY  = y + h / 2;
            drawTerminal(g2, outId, x + w, outY, x + w, outY, x + w + 20, outY, true);
        }
    }

    /**
     * Draw a terminal: wire stub, dot, node-number label.
     * lineFrom→lineTo is the stub line; dot is at dotX,dotY; label is beyond.
     */
    private void drawTerminal(Graphics2D g2, int nodeId,
                              int lineFromX, int lineFromY,
                              int lineToX,  int lineToY,
                              int dotX,     int dotY,
                              boolean isOutput) {
        Integer sig = lastSimResult.get(nodeId);
        boolean high = (sig != null && sig == 1);
        Color sigColor = high ? new Color(0, 210, 90) : new Color(90, 90, 120);

        g2.setColor(sigColor);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(lineFromX, lineFromY, dotX, dotY);

        // Dot
        g2.setColor(new Color(210, 200, 110));
        g2.fillOval(dotX - 4, dotY - 4, 8, 8);
        g2.setColor(new Color(110, 100, 50));
        g2.drawOval(dotX - 4, dotY - 4, 8, 8);

        // Label
        g2.setColor(new Color(230, 220, 110));
        g2.setFont(new Font("Monospaced", Font.BOLD, 10));
        String label = String.valueOf(nodeId);
        if (isOutput) g2.drawString(label, dotX + 6, dotY + 4);
        else          g2.drawString(label, dotX - 6 - g2.getFontMetrics().stringWidth(label), dotY + 4);
    }

    // ── Shape Renderers ───────────────────────────────────────────────────────
    private void drawAND(Graphics2D g2, int x, int y, int w, int h, Color body, Color fill) {
        int r = h / 2;
        GeneralPath p = new GeneralPath();
        p.moveTo(x, y);
        p.lineTo(x, y + h);
        p.lineTo(x + r, y + h);
        p.append(new Arc2D.Double(x + r - 2, y, h, h, -90, 180, Arc2D.OPEN), true);
        p.lineTo(x, y);
        p.closePath();
        g2.setColor(fill); g2.fill(p);
        g2.setColor(body); g2.draw(p);
        g2.setFont(new Font("Arial", Font.BOLD, 11)); g2.drawString("AND", x + 6, y + h/2 + 4);
    }

    private void drawOR(Graphics2D g2, int x, int y, int w, int h, Color body, Color fill) {
        GeneralPath p = new GeneralPath();
        p.moveTo(x, y);
        p.quadTo(x + w * 0.4, y + h * 0.5, x, y + h);
        p.quadTo(x + w * 0.6, y + h,         x + w, y + h * 0.5);
        p.quadTo(x + w * 0.6, y,              x, y);
        p.closePath();
        g2.setColor(fill); g2.fill(p);
        g2.setColor(body); g2.draw(p);
        g2.setFont(new Font("Arial", Font.BOLD, 11)); g2.drawString("OR", x + 18, y + h/2 + 4);
    }

    private void drawNOT(Graphics2D g2, int x, int y, int w, int h, Color body, Color fill) {
        int[] xs = { x, x, x + w - 10 };
        int[] ys = { y, y + h, y + h/2 };
        g2.setColor(fill);  g2.fillPolygon(xs, ys, 3);
        g2.setColor(body);  g2.drawPolygon(xs, ys, 3);
        g2.drawOval(x + w - 10, y + h/2 - 5, 10, 10);
        g2.setFont(new Font("Arial", Font.BOLD, 11)); g2.drawString("NOT", x + 4, y + h/2 + 4);
    }

    private void drawXOR(Graphics2D g2, int x, int y, int w, int h, Color body, Color fill) {
        GeneralPath p = new GeneralPath();
        int dx = 8;
        p.moveTo(x + dx, y);
        p.quadTo(x + dx + w * 0.4, y + h * 0.5, x + dx, y + h);
        p.quadTo(x + dx + w * 0.6, y + h,          x + dx + w, y + h * 0.5);
        p.quadTo(x + dx + w * 0.6, y,               x + dx, y);
        p.closePath();
        g2.setColor(fill); g2.fill(p);
        g2.setColor(body); g2.draw(p);
        // Extra arc for XOR
        GeneralPath extra = new GeneralPath();
        extra.moveTo(x, y);
        extra.quadTo(x + w * 0.35, y + h * 0.5, x, y + h);
        g2.draw(extra);
        g2.setFont(new Font("Arial", Font.BOLD, 11)); g2.drawString("XOR", x + 14, y + h/2 + 4);
    }

    private void drawSWITCH(Graphics2D g2, int x, int y, int w, int h, GateVisual gate, boolean sel) {
        int midY = y + h / 2;
        Color c   = sel ? new Color(0, 190, 230) : new Color(180, 180, 205);
        Color box = new Color(42, 42, 58);

        g2.setColor(box);
        g2.fillRoundRect(x, y + h/4, w, h/2, 8, 8);
        g2.setColor(c);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(x, y + h/4, w, h/2, 8, 8);

        // Contact dots
        g2.setColor(c);
        g2.fillOval(x + 4,         midY - 4, 8, 8);
        g2.fillOval(x + w - 4 - 8, midY - 4, 8, 8);

        // Lever
        g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (gate.isClosed()) {
            g2.setColor(new Color(0, 210, 90));
            g2.drawLine(x + 12, midY, x + w - 12, midY);
        } else {
            g2.setColor(new Color(220, 90, 70));
            g2.drawLine(x + 12, midY, x + w - 16, midY - 16);
        }

        // State label
        g2.setFont(new Font("Arial", Font.BOLD, 9));
        g2.setColor(gate.isClosed() ? new Color(0,210,90) : new Color(220,90,70));
        String st = gate.isClosed() ? "[1]" : "[0]";
        g2.drawString(st, x + w/2 - 8, midY + 4);

        // "SWITCH" caption below
        g2.setColor(c);
        g2.setFont(new Font("Arial", Font.PLAIN, 9));
        g2.drawString("SWITCH", x + 6, y + h + 12);

        // Input terminal drawn on left (VCC symbol)
        int lx = x - 20, ly = midY;
        g2.setColor(new Color(90, 90, 120));
        g2.setStroke(new BasicStroke(1.5f));
        // No actual input node for switch; just a VCC symbol
        g2.setColor(new Color(150, 150, 180));
        g2.setFont(new Font("Arial", Font.BOLD, 9));
        g2.drawString("VCC", x - 22, midY + 4);
    }

    private void drawLED(Graphics2D g2, int x, int y, int diameter, GateVisual gate, boolean sel) {
        int inputNode = gate.getInputNodeIds().get(0);
        boolean on = getLedSignal(gate, lastSimResult) == 1;
        gate.setOn(on);
        Color offFill  = new Color(70, 20, 20);
        Color onFill   = new Color(20, 200, 70);
        Color glowCol  = new Color(0, 255, 100, 60);

        if (on) {
            // Glow halo
            for (int r = 12; r > 0; r -= 3) {
                g2.setColor(new Color(glowCol.getRed(), glowCol.getGreen(), glowCol.getBlue(), 20 + (12 - r) * 4));
                g2.fillOval(x - r, y - r, diameter + 2*r, diameter + 2*r);
            }
        }

        g2.setColor(on ? onFill : offFill);
        g2.fillOval(x, y, diameter, diameter);

        Color border = sel ? new Color(0, 190, 230) : (on ? new Color(0, 160, 60) : new Color(120, 50, 50));
        g2.setColor(border);
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawOval(x, y, diameter, diameter);

        // Shine
        if (on) {
            g2.setColor(new Color(255, 255, 255, 100));
            g2.fillOval(x + diameter/5, y + diameter/5, diameter/4, diameter/5);
        }

        // "LED" caption
        g2.setColor(on ? new Color(0, 210, 90) : new Color(150, 70, 70));
        g2.setFont(new Font("Arial", Font.BOLD, 9));
        g2.drawString("LED", x + diameter/2 - 9, y + diameter + 13);

        // Input terminal on left side
        int ny = y + diameter / 2;
        int nx = x - 20;
        drawTerminal(g2, inputNode, nx, ny, nx, ny, nx, ny, false);
        // draw stub from terminal to LED body
        Integer sig = lastSimResult.get(inputNode);
        boolean high = (sig != null && sig == 1);
        g2.setColor(high ? new Color(0, 210, 90) : new Color(90, 90, 120));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(nx, ny, x, ny);
    }

    // Some older/manual connections may target the LED's output node ID.
    // Treat either LED-side node as a valid sense point so the lamp still reflects logic HIGH.
    private int getLedSignal(GateVisual gate, Map<Integer,Integer> signalMap) {
        int inputNode = gate.getInputNodeIds().get(0);
        int outputNode = gate.getOutputNodeId();
        return Math.max(signalMap.getOrDefault(inputNode, 0), signalMap.getOrDefault(outputNode, 0));
    }

    // ── Position helpers ──────────────────────────────────────────────────────
    Point findNodePos(int nodeId) {
        Point j = junctionNodePositions.get(nodeId);
        if (j != null) return j;

        for (GateVisual gate : gates) {
            int x = gate.getX(), y = gate.getY(), w = gate.getWidth(), h = gate.getHeight();

            // Input nodes (left side)
            List<Integer> ins = gate.getInputNodeIds();
            if (!ins.isEmpty()) {
                int spacing = h / (ins.size() + 1);
                for (int i = 0; i < ins.size(); i++) {
                    if (ins.get(i) == nodeId) {
                        if (gate.getType().equals("LED"))
                            return new Point(x - 20, y + h / 2);
                        return new Point(x - 20, y + spacing * (i + 1));
                    }
                }
            }

            // Output node (right side)
            if (gate.getOutputNodeId() == nodeId)
                return new Point(x + w + 20, y + h / 2);
        }
        return null;
    }

    private Integer portAt(Point p) {
        Integer bestNode = null;
        double bestDistSq = PORT_HIT_RADIUS * PORT_HIT_RADIUS;

        for (GateVisual gate : gates) {
            for (Integer inId : gate.getInputNodeIds()) {
                Point np = findNodePos(inId);
                if (np == null) continue;
                double dsq = np.distanceSq(p);
                if (dsq <= bestDistSq) {
                    bestDistSq = dsq;
                    bestNode = inId;
                }
            }

            int outId = gate.getOutputNodeId();
            Point outPos = findNodePos(outId);
            if (outPos != null) {
                double dsq = outPos.distanceSq(p);
                if (dsq <= bestDistSq) {
                    bestDistSq = dsq;
                    bestNode = outId;
                }
            }
        }

        for (Map.Entry<Integer, Point> e : junctionNodePositions.entrySet()) {
            double dsq = e.getValue().distanceSq(p);
            if (dsq <= bestDistSq) {
                bestDistSq = dsq;
                bestNode = e.getKey();
            }
        }
        return bestNode;
    }

    private Wire wireAt(Point p) {
        for (Wire wire : wires) {
            Point p1 = findNodePos(wire.getFromNode());
            Point p2 = findNodePos(wire.getToNode());
            if (p1 == null || p2 == null) continue;
            int midX = (p1.x + p2.x) / 2;
            if (Line2D.ptSegDist(p1.x, p1.y, midX, p1.y, p.x, p.y) < 6) return wire;
            if (Line2D.ptSegDist(midX, p1.y, midX, p2.y, p.x, p.y) < 6) return wire;
            if (Line2D.ptSegDist(midX, p2.y, p2.x, p2.y, p.x, p.y) < 6) return wire;
        }
        return null;
    }

    private Integer createOrReuseJunctionOnWire(Wire wire, Point click) {
        Point p1 = findNodePos(wire.getFromNode());
        Point p2 = findNodePos(wire.getToNode());
        if (p1 == null || p2 == null) return null;

        Point snapped = closestPointOnOrthWire(p1, p2, click);
        if (snapped == null) return null;

        if (snapped.distance(p1) <= PORT_HIT_RADIUS) return wire.getFromNode();
        if (snapped.distance(p2) <= PORT_HIT_RADIUS) return wire.getToNode();

        int junctionId = GateVisual.peekNextNode();
        GateVisual.bumpCounterAbove(junctionId);
        junctionNodePositions.put(junctionId, snapped);

        wires.remove(wire);
        wires.add(new Wire(wire.getFromNode(), junctionId));
        wires.add(new Wire(junctionId, wire.getToNode()));
        statusPanel.setStatusOk("Junction inserted at node " + junctionId + ".");
        return junctionId;
    }

    private Point closestPointOnOrthWire(Point p1, Point p2, Point click) {
        int midX = (p1.x + p2.x) / 2;

        Point a = new Point(clamp(click.x, Math.min(p1.x, midX), Math.max(p1.x, midX)), p1.y);
        Point b = new Point(midX, clamp(click.y, Math.min(p1.y, p2.y), Math.max(p1.y, p2.y)));
        Point c = new Point(clamp(click.x, Math.min(midX, p2.x), Math.max(midX, p2.x)), p2.y);

        double da = a.distanceSq(click);
        double db = b.distanceSq(click);
        double dc = c.distanceSq(click);

        if (da <= db && da <= dc) return a;
        if (db <= da && db <= dc) return b;
        return c;
    }

    private int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private void cancelPendingWire() {
        pendingWireFromNode = null;
        pendingWireMouse = null;
    }

    private void showConnectError(String msg, boolean showDialog) {
        statusPanel.setStatusError(msg);
        if (showDialog) {
            JOptionPane.showMessageDialog(this, msg, "Connect Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean isOutputNode(int nodeId) {
        for (GateVisual gate : gates) {
            if (gate.getOutputNodeId() == nodeId) return true;
        }
        return false;
    }

    private boolean isInputNode(int nodeId) {
        for (GateVisual gate : gates) {
            if (gate.getInputNodeIds().contains(nodeId)) return true;
        }
        return false;
    }

    private boolean isJunctionNode(int nodeId) {
        return junctionNodePositions.containsKey(nodeId);
    }
}
