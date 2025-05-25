package io.kiponos.demo.view;

import io.kiponos.demo.control.CommControl;
import io.kiponos.sdk.configs.Folder;
import io.kiponos.sdk.system.Log;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import static io.kiponos.demo.control.CommPanelMain.cfg;
import static io.kiponos.demo.control.CommPanelMain.commPanelCfg;

public class CommPanelView extends JFrame {

    private final JPanel mainPane = View.createMainPane();
    private final JTextField tfCityName = new JTextField(32);
    private final JTextField tfFirstName = new JTextField(32);
    private final Folder cityToggleCfg = commPanelCfg.path("toggles", "feature-by-city");
    private final JLabel lblTranslated = new JLabel("Translation");

    private final KeyAdapter keyAdapter = new KeyAdapter() {
        @Override
        public void keyReleased(KeyEvent e) {
            String keyName = "name-" + (tfFirstName.getText() == null ? "" : tfFirstName.getText().toLowerCase());
            String translated = cityToggleCfg.getOrEmpty(keyName);
            lblTranslated.setText(translated);
            lblTranslated.setVisible(isShowFeature());

            mainPane.revalidate();
            mainPane.repaint();
        }
    };

    private boolean isShowFeature() {
        // Users in this city will have the feature enabled for them.
        String featureCity = cityToggleCfg.get("city-name");

        // User input city name.
        String userCity = tfCityName.getText();

        boolean result = featureCity.equalsIgnoreCase(userCity);
        Log.trace("isShowFeature: %b", result);
        return result;
    }

    public CommPanelView() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        cfg.afterValueUpdated(valUpdated -> {
            if (valUpdated.getKey().equalsIgnoreCase("title")) {
              setTitle(valUpdated.getValue());
            }
        });
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

        add(mainPane, View.createMainConstraints());

        // Header Pane
        JPanel headerPane = View.createHeaderPane();
        mainPane.add(headerPane, View.createHeaderConstraints());
        headerPane.add(CommControl.createExitButton(this), View.createExitButtonConstraint());

        // Feature Toggle
        JPanel togglesPane = View.createTogglesPane();
        togglesPane.setLayout(new GridBagLayout());
        View.addTo(togglesPane, new JLabel("Feature Toggles"), 0, 0);

        JPanel namePane = new JPanel(new FlowLayout(FlowLayout.LEFT));
        namePane.add(new JLabel("First Name:"));
        namePane.add(Box.createHorizontalStrut(24));

        namePane.add(tfFirstName);
        namePane.add(Box.createHorizontalStrut(16));

        lblTranslated.setOpaque(true);
        lblTranslated.setSize(280, 24);
        lblTranslated.setForeground(Color.decode("#ee8"));
        lblTranslated.setBackground(Color.orange);
        lblTranslated.setVisible(false);
        namePane.add(lblTranslated);


        View.addTo(togglesPane, namePane, 0,1);

        JPanel lastNamePane = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField tfLastName = new JTextField(32);
        lastNamePane.add(new JLabel("Last Name:"));
        lastNamePane.add(Box.createHorizontalStrut(26));
        lastNamePane.add(tfLastName);
        View.addTo(togglesPane, lastNamePane, 0,2);

        JPanel cityPane = new JPanel(new FlowLayout(FlowLayout.LEFT));
        cityPane.add(new JLabel("City:"));
        cityPane.add(Box.createHorizontalStrut(66));

        cityPane.add(tfCityName);
        View.addTo(togglesPane, cityPane, 0, 3);

        tfFirstName.addKeyListener(keyAdapter);
        tfCityName.addKeyListener(keyAdapter);

        mainPane.add(togglesPane, View.createTogglesConstraints());
        // Bottom Buffer
        mainPane.add(View.createCommSubPane("#101010", "#202020"), View.createBufferConstraints());
    }
}