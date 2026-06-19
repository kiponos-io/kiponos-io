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
import static io.kiponos.demo.view.View.gbc;

/**
        ------[  Main Frame  ]------[  CommPanelView  ]------
*/
public class CommPanelView extends JFrame {
    private final JPanel mainPane = View.createMainPane();
    private final JTextField tfCityName = new JTextField(32);
    private final JTextField tfFirstName = new JTextField(32);
    private final Folder cityToggleCfg = commPanelCfg.path("toggles", "feature-by-city");
    private final JLabel lblTranslated = new JLabel("Translation");
    private final JLabel lblFeatureToggles = new JLabel("Feature Toggles");
    private final JLabel lblBeta = new JLabel(" First Name Translation * BETA * ");

    //------[  Constructor  ]------
    //
    public CommPanelView() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        cfg.afterValueUpdated(valUpdated -> {
            if (valUpdated.getKey().equalsIgnoreCase("title")) {
              setTitle(valUpdated.getValue());
            }

            if (valUpdated.getKey().equalsIgnoreCase("city-name")) {
                translate();
            }
        });
        int posX = commPanelCfg.getInt("pos-x");
        int posY = commPanelCfg.getInt("pos-y");
        int width = commPanelCfg.getInt("width");
        int height = commPanelCfg.getInt("height");

//        String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
//        for (int i=0; i<fonts.length; ++i) {
//            //commPanelCfg.set("font-"+i, fonts[i]);
//        }

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

        headerPane.add(CommControl.createExitButton(this), View.createExitButtonConstraint());

        // Feature Toggle
        JPanel togglesPane = View.createTogglesPane();
        lblFeatureToggles.setForeground(Color.GRAY);
        togglesPane.add(lblFeatureToggles);
        JPanel formPane = new JPanel();
        formPane.setLayout(new BoxLayout(formPane, BoxLayout.Y_AXIS));
        togglesPane.add(formPane);

        formPane.add(createFirstNamePane());
        formPane.add(createLastNamePane());
        formPane.add(createCityPane());

        lblBeta.setVisible(false);
        lblBeta.setForeground(Color.YELLOW);
        togglesPane.add(lblBeta, 1, 0);

        tfFirstName.addKeyListener(keyAdapter);
        tfCityName.addKeyListener(keyAdapter);

        JPanel betaPane = View.createTranslationBETAPane();
        betaPane.add(new JLabel("TRANSLATION 1"));
        betaPane.add(Box.createHorizontalStrut(24));
        betaPane.add(new JLabel("TRANSLATION 2"));
        betaPane.add(Box.createHorizontalStrut(24));
        betaPane.add(new JLabel("TRANSLATION 3"));

        mainPane.add(headerPane, View.createHeaderConstraints());
        mainPane.add(togglesPane, gbc(0,1,2)); //View.createTogglesConstraints());
        mainPane.add(betaPane, gbc(0,2, 2));
        mainPane.add(View.createCommSubPane("#101010", "#202020"), View.createBufferConstraints());
    }

    private JPanel createCityPane() {
        JPanel cityPane = new JPanel(new FlowLayout(FlowLayout.LEFT));
        cityPane.add(new JLabel("City:"));
        cityPane.add(Box.createHorizontalStrut(66));

        cityPane.add(tfCityName);
        return cityPane;
    }

    private static JPanel createLastNamePane() {
        JPanel lastNamePane = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField tfLastName = new JTextField(32);
        lastNamePane.add(new JLabel("Last Name:"));
        lastNamePane.add(Box.createHorizontalStrut(26));
        lastNamePane.add(tfLastName);
        return lastNamePane;
    }

    private JPanel createFirstNamePane() {
        JPanel firstNamePane = new JPanel(new FlowLayout(FlowLayout.LEFT));
        firstNamePane.add(new JLabel("First Name:"));
        firstNamePane.add(Box.createHorizontalStrut(24));

        firstNamePane.add(tfFirstName);
        firstNamePane.add(Box.createHorizontalStrut(16));

        lblTranslated.setOpaque(true);
        lblTranslated.setSize(280, 24);
        lblTranslated.setForeground(Color.decode("#ee8"));
        lblTranslated.setBackground(Color.orange);
        lblTranslated.setVisible(false);
        firstNamePane.add(lblTranslated);
        return firstNamePane;
    }

    private void translate() {
        String keyName = "name-" + (tfFirstName.getText() == null ? "" : tfFirstName.getText().toLowerCase());
        String translated = cityToggleCfg.getOrEmpty(keyName);
        lblTranslated.setText(translated);
        lblTranslated.setVisible(isShowFeature());

        mainPane.revalidate();
        mainPane.repaint();
    }

    private final KeyAdapter keyAdapter = new KeyAdapter() {
        @Override
        public void keyReleased(KeyEvent e) {
            translate();
        }
    };

    private boolean isShowFeature() {
        // Users in this city will have the feature enabled for them.
        String featureCity = cityToggleCfg.get("city-name");

        // User input city name.
        String userCity = tfCityName.getText();

        boolean isShow = featureCity.equalsIgnoreCase(userCity);
        Log.trace("isShowFeature: %b", isShow);

        if (isShow) {
            lblFeatureToggles.setForeground(Color.YELLOW);
        }
        else {
            lblFeatureToggles.setForeground(Color.GRAY);
        }

        lblBeta.setVisible(isShow);
        return isShow;
    }

}