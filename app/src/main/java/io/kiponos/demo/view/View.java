package io.kiponos.demo.view;

import javax.swing.*;
import java.awt.*;

public class View {
    public static JPanel createHeaderPane() {
        JPanel pane = createCommSubPane("#9b7200", "#222");
        pane.setBackground(Color.decode("#246"));
        return pane;
    }

    public static JPanel createCommSubPane() {
        return createCommSubPane("#9b7200", "#222");
    }

    public static JPanel createCommSubPane(String borderColor, String bgColor) {
        JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.X_AXIS));
        pane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.decode(borderColor), 2), BorderFactory.createEmptyBorder(8,16,8,16)));
        pane.setBackground(Color.decode(bgColor));
        return pane;
    }

    public static JPanel createMainPane() {
        JPanel pane = new JPanel(new GridBagLayout());
        pane.setBackground(Color.BLACK);
        return pane;
    }

    public static GridBagConstraints createMainConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets = new Insets(4,4,4,4);
        return gbc;
    }

    public static GridBagConstraints createHeaderConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridheight = 1;
        gbc.insets = new Insets(8,16,8,16);

        return gbc;
    }

    public static GridBagConstraints createBufferConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weighty = 1.0;
        gbc.weightx = 1.0;

        return gbc;
    }

    public static GridBagConstraints createExitButtonConstraint() {
        GridBagConstraints gbc = new GridBagConstraints();
        return gbc;
    }

    public static JPanel createTogglesPane() {
        JPanel pane = createCommSubPane();
        pane.setBackground(Color.decode("#123"));
        return pane;
    }

    public static GridBagConstraints createDefault() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8,16,8,16);
        gbc.weighty = 1.0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        return gbc;
    }

    public static GridBagConstraints createTogglesConstraints() {
        GridBagConstraints c = createDefault();
        c.gridx = 0;
        c.gridy = 1;
        c.anchor = GridBagConstraints.NORTH;
        return c;
    }

    public static void addTo(JPanel pane, JComponent comp, int gridX, int gridY) {
        GridBagConstraints c = createDefault();
        c.gridx = gridX;
        c.gridy = gridY;
        pane.add(comp, c);
    }
}