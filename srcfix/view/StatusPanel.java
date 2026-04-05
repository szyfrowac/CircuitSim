package view;

import javax.swing.*;
import java.awt.*;

public class StatusPanel extends JPanel {

    private JLabel statusLabel;
    private JLabel hintLabel;

    public StatusPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(30, 30, 30));
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(60, 60, 60)));
        setPreferredSize(UiScale.dimension(0, 28));

        statusLabel = new JLabel("  Status: Ready");
        statusLabel.setForeground(new Color(80, 200, 80));
        statusLabel.setFont(UiScale.font(Font.MONOSPACED, Font.BOLD, 12));

        hintLabel = new JLabel(
            "Quick Guide: Left-click to place or select, Right-click to delete wire, " +
            "'Apply Input' to set signals, 'Delete Gate' to remove component   ");
        hintLabel.setForeground(new Color(150, 150, 150));
        hintLabel.setFont(UiScale.font(Font.MONOSPACED, Font.PLAIN, 11));

        add(statusLabel, BorderLayout.WEST);
        add(hintLabel,   BorderLayout.EAST);
    }

    public void setStatus(String message) {
        statusLabel.setText("  Status: " + message);
    }

    public void setStatusOk(String message) {
        statusLabel.setForeground(new Color(80, 200, 80));
        statusLabel.setText("  Status: " + message);
    }

    public void setStatusWarn(String message) {
        statusLabel.setForeground(new Color(255, 160, 0));
        statusLabel.setText("  Status: " + message);
    }

    public void setStatusError(String message) {
        statusLabel.setForeground(new Color(255, 80, 80));
        statusLabel.setText("  Status: " + message);
    }
}
