package view;
import model.GateVisual;
import model.Wire;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class MainFrame extends JFrame {

    private final ToolPanel    toolPanel;
    private final CircuitPanel circuitPanel;
    private final StatusPanel  statusPanel;

    public MainFrame() {
        applyDarkDefaults();
        try { UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel"); } catch (Exception ignored) {}

        setTitle("Digital Circuit Simulator");
        setSize(1100, 730);
        setMinimumSize(new Dimension(900, 600));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        statusPanel  = new StatusPanel();
        circuitPanel = new CircuitPanel(statusPanel);
        toolPanel    = new ToolPanel();

        // Wire listeners
        toolPanel.setToolSelectionListener(circuitPanel::setSelectedTool);
        toolPanel.setConnectListener(circuitPanel::connectNodes);
        toolPanel.setSignalApplyListener(circuitPanel::applyManualSignals);

        // Layout: tool panel left | (canvas + bottom toolbar) center
        JPanel centerArea = new JPanel(new BorderLayout());
        centerArea.add(circuitPanel, BorderLayout.CENTER);
        centerArea.add(buildBottomToolbar(), BorderLayout.SOUTH);

        JPanel root = new JPanel(new BorderLayout());
        root.add(toolPanel,   BorderLayout.WEST);
        root.add(centerArea,  BorderLayout.CENTER);
        root.add(statusPanel, BorderLayout.SOUTH);
        setContentPane(root);

        setJMenuBar(buildMenuBar());
        setVisible(true);
    }

    // ── Menu bar ──────────────────────────────────────────────────────────────
    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();
        bar.setBackground(new Color(38, 38, 50));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(60, 60, 75)));

        JMenu mFile = dmenu("File");
        mFile.add(ditem("Save",     e -> saveCircuit()));
        mFile.add(ditem("Save As…", e -> saveCircuitAs()));
        mFile.add(ditem("Open",     e -> openCircuit()));
        mFile.addSeparator();
        mFile.add(ditem("Exit",     e -> System.exit(0)));

        JMenu mEdit = dmenu("Edit");
        mEdit.add(ditem("Undo",         e -> statusPanel.setStatusWarn("Undo not implemented.")));
        mEdit.add(ditem("Redo",         e -> statusPanel.setStatusWarn("Redo not implemented.")));
        mEdit.addSeparator();
        mEdit.add(ditem("Clear Canvas", e -> circuitPanel.clearCanvas()));

        JMenu mView = dmenu("View");
        mView.add(ditem("Zoom In",  e -> statusPanel.setStatusWarn("Zoom not implemented.")));
        mView.add(ditem("Zoom Out", e -> statusPanel.setStatusWarn("Zoom not implemented.")));

        JMenu mTools = dmenu("Tools");
        mTools.add(ditem("Verification",         e -> doVerify()));
        mTools.add(ditem("Truth Table Generator",e -> doTruthTable()));
        mTools.add(ditem("Simulate",             e -> statusPanel.setStatusOk("Toggle switches to simulate.")));

        JMenu mHelp = dmenu("Help");
        mHelp.add(ditem("Quick Manual", e -> showManual()));

        bar.add(mFile); bar.add(mEdit); bar.add(mView); bar.add(mTools); bar.add(mHelp);
        return bar;
    }

    // ── Bottom toolbar (matches prototype: Verify | Truth Table | Delete | Remove Wire) ──
    private JPanel buildBottomToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        bar.setBackground(new Color(32, 32, 44));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(60, 60, 75)));

        bar.add(toolbarButton("✔ Verify Circuit",       new Color(35, 90, 55),  e -> doVerify()));
        bar.add(toolbarButton("⊞ Generate Truth Table", new Color(35, 55, 110), e -> doTruthTable()));
        bar.add(toolbarButton("⊖ Delete Gate/Component",new Color(100, 35, 35), e -> circuitPanel.deleteSelected()));
        bar.add(toolbarButton("⤫ Remove Wire/Connection",new Color(80, 50, 20), e ->
                statusPanel.setStatusOk("Right-click on any wire to remove it.")));

        return bar;
    }

    private JButton toolbarButton(String text, Color bg, java.awt.event.ActionListener al) {
        JButton btn = new JButton("<html><center>" + text + "</center></html>");
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        btn.setPreferredSize(new Dimension(180, 46));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bg.brighter(), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        btn.addActionListener(al);
        return btn;
    }

    // ── Actions ───────────────────────────────────────────────────────────────
    private void doVerify() {
        String msg = circuitPanel.verify();
        boolean ok  = msg.startsWith("Circuit OK");
        JOptionPane.showMessageDialog(this, msg, "Circuit Verification",
                ok ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
        if (ok) statusPanel.setStatusOk(msg);
        else    statusPanel.setStatusError(msg);
    }

    private void doTruthTable() {
        List<String[]> data = circuitPanel.generateTruthTable();
        if (data.size() < 2) {
            JOptionPane.showMessageDialog(this,
                    "Add at least one SWITCH and one LED to generate a truth table.",
                    "Truth Table", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        new TruthTableDialog(this, data);
    }

    // ── Save / Load ───────────────────────────────────────────────────────────
    private File lastFile = null;

    private void saveCircuit() {
        if (lastFile == null) { saveCircuitAs(); return; }
        doSave(lastFile);
    }

    private void saveCircuitAs() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Circuit");
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        lastFile = fc.getSelectedFile();
        if (!lastFile.getName().endsWith(".circuit"))
            lastFile = new File(lastFile.getPath() + ".circuit");
        doSave(lastFile);
    }

    private void doSave(File f) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
            for (GateVisual g : circuitPanel.getGates()) pw.println("GATE:" + g.serialize());
            for (Wire w       : circuitPanel.getWires())  pw.println("WIRE:" + w.getFromNode() + "," + w.getToNode());
            statusPanel.setStatusOk("Saved → " + f.getName());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Save failed:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openCircuit() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Open Circuit");
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        List<GateVisual> gates = new ArrayList<>();
        List<Wire>       wires = new ArrayList<>();
        int maxId = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("GATE:")) {
                    GateVisual gv = GateVisual.deserialize(line.substring(5));
                    gates.add(gv);
                    for (int id : gv.getInputNodeIds()) maxId = Math.max(maxId, id);
                    maxId = Math.max(maxId, gv.getOutputNodeId());
                } else if (line.startsWith("WIRE:")) {
                    String[] p = line.substring(5).split(",");
                    wires.add(new Wire(Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim())));
                }
            }
        } catch (IOException | NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Open failed:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        GateVisual.bumpCounterAbove(maxId);
        circuitPanel.loadCircuit(gates, wires);
        lastFile = f;
        statusPanel.setStatusOk("Loaded ← " + f.getName());
    }

    // ── Help ─────────────────────────────────────────────────────────────────
    private void showManual() {
        String txt =
            "Digital Circuit Simulator — Quick Manual\n\n" +
            "ADD COMPONENTS\n" +
            "  Click a gate button (AND/OR/NOT/XOR/SWITCH/LED) then click the canvas.\n\n" +
            "NODE NUMBERS\n" +
            "  Every terminal shows a yellow number. Inputs are on the left,\n" +
            "  outputs on the right of each gate.\n\n" +
            "CONNECT NODES\n" +
            "  Type two node numbers in 'Connect Nodes' and press Connect.\n" +
            "  Connect an output node → an input node.\n\n" +
            "SIMULATE\n" +
            "  Click any SWITCH to toggle it. LEDs update instantly.\n" +
            "  Green wires = HIGH (1), grey wires = LOW (0).\n\n" +
            "MANUAL SIGNALS\n" +
            "  Enter a node number + value (0/1) in 'Input Signals' → Apply All.\n\n" +
            "DELETE\n" +
            "  Select a gate (left-click) → Delete key, or 'Delete Gate' button.\n" +
            "  Right-click a wire to remove it.\n\n" +
            "SAVE / LOAD\n" +
            "  File → Save / Open   (saves as .circuit text file)\n\n" +
            "TRUTH TABLE\n" +
            "  Tools → Truth Table Generator\n" +
            "  Automatically iterates all switch combinations.\n\n" +
            "VERIFY\n" +
            "  Tools → Verification   detects short circuits.";
        JOptionPane.showMessageDialog(this, txt, "Quick Manual", JOptionPane.INFORMATION_MESSAGE);
    }

    // ── Dark UI helpers ───────────────────────────────────────────────────────
    private static void applyDarkDefaults() {
        UIManager.put("Panel.background",             new Color(40, 40, 52));
        UIManager.put("Button.background",            new Color(52, 52, 68));
        UIManager.put("Button.foreground",            Color.WHITE);
        UIManager.put("Label.foreground",             Color.WHITE);
        UIManager.put("TextField.background",         new Color(52, 52, 68));
        UIManager.put("TextField.foreground",         Color.WHITE);
        UIManager.put("TextField.caretForeground",    Color.WHITE);
        UIManager.put("MenuBar.background",           new Color(38, 38, 50));
        UIManager.put("Menu.background",              new Color(38, 38, 50));
        UIManager.put("Menu.foreground",              Color.WHITE);
        UIManager.put("MenuItem.background",          new Color(48, 48, 64));
        UIManager.put("MenuItem.foreground",          Color.WHITE);
        UIManager.put("PopupMenu.background",         new Color(48, 48, 64));
        UIManager.put("OptionPane.background",        new Color(40, 40, 52));
        UIManager.put("OptionPane.messageForeground", Color.WHITE);
    }

    private JMenu dmenu(String name) {
        JMenu m = new JMenu(name); m.setForeground(Color.WHITE); return m;
    }
    private JMenuItem ditem(String name, java.awt.event.ActionListener al) {
        JMenuItem i = new JMenuItem(name); i.addActionListener(al); return i;
    }
}