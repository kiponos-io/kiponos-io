package io.kiponos.demo.view;

import io.kiponos.demo.control.CommControl;
import io.kiponos.sdk.system.Log;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import static io.kiponos.demo.control.CommPanelMain.commPanelCfg;

public class CommPanelView extends JFrame {

    public CommPanelView() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        int posX = commPanelCfg.getInt("pos-x");
        int posY = commPanelCfg.getInt("pos-y");
        int width = commPanelCfg.getInt("width");
        int height = commPanelCfg.getInt("height");

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                Point locationOnScreen = getLocationOnScreen();
                commPanelCfg.set("pos-x", String.valueOf(locationOnScreen.x));
                commPanelCfg.set("pos-y", String.valueOf(locationOnScreen.y));
                Log.trace("CommPanel moved to (%d, %d)", locationOnScreen.x, locationOnScreen.y);
            }

            @Override
            public void componentResized(ComponentEvent e) {
                commPanelCfg.set("width", String.valueOf(getWidth()));
                commPanelCfg.set("height", String.valueOf(getHeight()));
            }
        });

        setBounds(posX, posY, width, height);
        setTitle(commPanelCfg.get("title"));

        Container contentPane = getContentPane();
        contentPane.setBackground(Color.BLACK);

        setLayout(new GridBagLayout());

        // Main Pane
        JPanel mainPane = View.createMainPane();
        add(mainPane, View.createMainConstraints());

        // Header Pane
        JPanel headerPane = View.createHeaderPane();
        mainPane.add(headerPane, View.createHeaderConstraints());
        headerPane.add(CommControl.createExitButton(this), View.createExitButtonConstraint());

        // Feature Toggle
        JPanel togglesPane = View.createTogglesPane();
        togglesPane.setLayout(new GridBagLayout());
        View.addTo(togglesPane, new JLabel("Feature Toggles"), 0, 0);
        JPanel editName = new JPanel();
        BoxLayout editLayout = new BoxLayout(editName, BoxLayout.X_AXIS);
        editName.setLayout(editLayout);
        editName.add(new JLabel("Your Name:"));
        editName.add(Box.createHorizontalStrut(24));
        editName.add(new JTextField(32));
        View.addTo(togglesPane, editName, 0,1);

        mainPane.add(togglesPane, View.createTogglesConstraints());
        // Bottom Buffer
        mainPane.add(View.createCommSubPane("#101010", "#202020"), View.createBufferConstraints());
    }
}