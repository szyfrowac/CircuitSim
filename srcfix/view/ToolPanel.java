package view;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ToolPanel extends JPanel {

    private Consumer<String>          toolListener;
    private BiConsumer<Integer,Integer> connectListener;
    private Runnable                  applyAllListener;
    private Consumer<Map<Integer,Integer>> signalApplyListener;

    private JTextField node1Field;
    private JTextField node2Field;

    // Dynamic input rows: each row is [nodeField, valueField]
    private final List<JTextField[]> inputRows = new ArrayList<>();
    private JPanel inputRowsPanel;

    public ToolPanel() {
        setPreferredSize(UiScale.dimension(175, 0));
        setBackground(new Color(40, 40, 40));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(60, 60, 60)));

        add(Box.createVerticalStrut(UiScale.scale(10)));

        // ── Add Components ────────────────────────────────────────────────────
        addSectionLabel("Add Components");
        addGateButton("AND");
        addGateButton("OR");
        addGateButton("NOT");
        addGateButton("XOR");
        addGateButton("SWITCH");
        addGateButton("LED");

        add(Box.createVerticalStrut(UiScale.scale(12)));
        addDivider();
        add(Box.createVerticalStrut(UiScale.scale(8)));

        // ── Connect Nodes ─────────────────────────────────────────────────────
        addSectionLabel("Connect Nodes");

        addFieldLabel("Node 1");
        node1Field = createField();
        add(node1Field);
        add(Box.createVerticalStrut(UiScale.scale(4)));

        addFieldLabel("Node 2");
        node2Field = createField();
        add(node2Field);
        add(Box.createVerticalStrut(UiScale.scale(8)));

        JButton connectBtn = createStdButton("Connect");
        connectBtn.addActionListener(e -> {
            try {
                int n1 = Integer.parseInt(node1Field.getText().trim());
                int n2 = Integer.parseInt(node2Field.getText().trim());
                if (connectListener != null) connectListener.accept(n1, n2);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Enter valid integers for both nodes.");
            }
        });
        add(connectBtn);

        add(Box.createVerticalStrut(UiScale.scale(12)));
        addDivider();
        add(Box.createVerticalStrut(UiScale.scale(8)));

        // ── Input Signals ─────────────────────────────────────────────────────
        addSectionLabel("Input Signals");

        inputRowsPanel = new JPanel();
        inputRowsPanel.setLayout(new BoxLayout(inputRowsPanel, BoxLayout.Y_AXIS));
        inputRowsPanel.setBackground(new Color(40, 40, 40));
        inputRowsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(inputRowsPanel);
        addInputRow(); // start with one row

        add(Box.createVerticalStrut(UiScale.scale(6)));

        JButton addInputBtn = createStdButton("+ Add Input");
        addInputBtn.addActionListener(e -> { addInputRow(); revalidate(); repaint(); });
        add(addInputBtn);

        add(Box.createVerticalStrut(UiScale.scale(6)));

        JButton applyBtn = createStdButton("Apply All Inputs");
        applyBtn.addActionListener(e -> applyAllSignals());
        add(applyBtn);

        add(Box.createVerticalGlue());
    }

    // ── Public wiring ─────────────────────────────────────────────────────────
    public void setToolSelectionListener(Consumer<String> l)              { toolListener = l; }
    public void setConnectListener(BiConsumer<Integer,Integer> l)         { connectListener = l; }
    public void setSignalApplyListener(Consumer<Map<Integer,Integer>> l)  { signalApplyListener = l; }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void addGateButton(String text) {
        JButton btn = new JButton(text);
        btn.setMaximumSize(UiScale.dimension(140, 30));
        btn.setPreferredSize(UiScale.dimension(140, 30));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setBackground(new Color(55, 55, 65));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(new Color(80, 80, 90)));
        btn.addActionListener(e -> { if (toolListener != null) toolListener.accept(text); });
        add(btn);
        add(Box.createVerticalStrut(UiScale.scale(4)));
    }

    private JButton createStdButton(String text) {
        JButton btn = new JButton(text);
        btn.setMaximumSize(UiScale.dimension(140, 28));
        btn.setPreferredSize(UiScale.dimension(140, 28));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setBackground(new Color(55, 55, 65));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(new Color(80, 80, 90)));
        return btn;
    }

    private void addSectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(Color.WHITE);
        lbl.setFont(UiScale.font(Font.SANS_SERIF, Font.BOLD, 12));
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(lbl);
        add(Box.createVerticalStrut(UiScale.scale(6)));
    }

    private void addFieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(new Color(180, 180, 180));
        lbl.setFont(UiScale.font(Font.SANS_SERIF, Font.PLAIN, 11));
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(lbl);
        add(Box.createVerticalStrut(UiScale.scale(2)));
    }

    private JTextField createField() {
        JTextField f = new JTextField();
        f.setMaximumSize(UiScale.dimension(120, 26));
        f.setPreferredSize(UiScale.dimension(120, 26));
        f.setHorizontalAlignment(JTextField.CENTER);
        f.setAlignmentX(Component.CENTER_ALIGNMENT);
        f.setBackground(new Color(55, 55, 65));
        f.setForeground(Color.WHITE);
        f.setCaretColor(Color.WHITE);
        f.setBorder(BorderFactory.createLineBorder(new Color(80, 80, 90)));
        return f;
    }

    private void addDivider() {
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(UiScale.dimension(155, 1));
        sep.setForeground(new Color(70, 70, 70));
        sep.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(sep);
    }

    private void addInputRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, UiScale.scale(4), UiScale.scale(2)));
        row.setBackground(new Color(40, 40, 40));
        row.setMaximumSize(UiScale.dimension(160, 32));

        JLabel nodeLbl = new JLabel("Node");
        nodeLbl.setForeground(new Color(180, 180, 180));
        nodeLbl.setFont(UiScale.font(Font.SANS_SERIF, Font.PLAIN, 11));

        JTextField nodeF = new JTextField(3);
        nodeF.setBackground(new Color(55, 55, 65));
        nodeF.setForeground(Color.WHITE);
        nodeF.setCaretColor(Color.WHITE);
        nodeF.setHorizontalAlignment(JTextField.CENTER);
        nodeF.setBorder(BorderFactory.createLineBorder(new Color(80, 80, 90)));

        JTextField valF = new JTextField(2);
        valF.setBackground(new Color(55, 55, 65));
        valF.setForeground(Color.WHITE);
        valF.setCaretColor(Color.WHITE);
        valF.setHorizontalAlignment(JTextField.CENTER);
        valF.setBorder(BorderFactory.createLineBorder(new Color(80, 80, 90)));

        row.add(nodeLbl);
        row.add(nodeF);
        row.add(new JLabel(" ") {{ setForeground(Color.WHITE); }});
        row.add(valF);

        inputRowsPanel.add(row);
        inputRows.add(new JTextField[]{ nodeF, valF });
    }

    private void applyAllSignals() {
        Map<Integer,Integer> signals = new HashMap<>();
        for (JTextField[] pair : inputRows) {
            String nodeStr = pair[0].getText().trim();
            String valStr  = pair[1].getText().trim();
            if (nodeStr.isEmpty() || valStr.isEmpty()) continue;
            try {
                int node = Integer.parseInt(nodeStr);
                int val  = Integer.parseInt(valStr);
                if (val != 0 && val != 1) { JOptionPane.showMessageDialog(this,"Value must be 0 or 1"); return; }
                signals.put(node, val);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid node or value.");
                return;
            }
        }
        if (signalApplyListener != null) signalApplyListener.accept(signals);
    }
}
