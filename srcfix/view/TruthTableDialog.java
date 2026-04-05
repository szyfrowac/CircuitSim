package view;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;

public class TruthTableDialog extends JDialog {

    public TruthTableDialog(JFrame parent, List<String[]> tableData) {
        super(parent, "Truth Table", true);
        setSize(UiScale.dimension(600, 400));
        setLocationRelativeTo(parent);
        setBackground(new Color(30, 30, 40));

        if (tableData == null || tableData.size() < 2) {
            JLabel lbl = new JLabel("No switches or LEDs found. Add components first.", SwingConstants.CENTER);
            lbl.setForeground(Color.WHITE);
            add(lbl);
            return;
        }

        String[] header = tableData.get(0);
        String[][] rows = tableData.subList(1, tableData.size()).toArray(new String[0][]);

        DefaultTableModel model = new DefaultTableModel(rows, header) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(model);
        table.setBackground(new Color(35, 35, 50));
        table.setForeground(Color.WHITE);
        table.setGridColor(new Color(70, 70, 90));
        table.setRowHeight(UiScale.scale(24));
        table.setFont(UiScale.font(Font.MONOSPACED, Font.PLAIN, 13));
        table.getTableHeader().setBackground(new Color(50, 50, 70));
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setFont(UiScale.font(Font.MONOSPACED, Font.BOLD, 13));

        // Colour rows: 1 = green, 0 = grey, SHORT = red
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean isSel, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(t, val, isSel, hasFocus, row, col);
                setHorizontalAlignment(CENTER);
                String v = val == null ? "" : val.toString();
                if (v.equals("SHORT")) {
                    setBackground(new Color(120, 30, 30)); setForeground(Color.WHITE);
                } else if (v.equals("1")) {
                    setBackground(new Color(30, 90, 50)); setForeground(new Color(100, 255, 130));
                } else {
                    setBackground(new Color(35, 35, 50)); setForeground(new Color(180, 180, 200));
                }
                return this;
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(new Color(35, 35, 50));
        add(scroll, BorderLayout.CENTER);

        JButton closeBtn = new JButton("Close");
        closeBtn.setBackground(new Color(55, 55, 70));
        closeBtn.setForeground(Color.WHITE);
        closeBtn.addActionListener(e -> dispose());
        JPanel south = new JPanel();
        south.setBackground(new Color(40, 40, 55));
        south.add(closeBtn);
        add(south, BorderLayout.SOUTH);

        setVisible(true);
    }
}
