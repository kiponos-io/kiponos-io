package io.kiponos.demo.control;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;

public class CommControl {

    public static JButton createExitButton(Window windowToClose) {
        JButton exitButton = new JButton(new AbstractAction("Exit") {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                windowToClose.dispatchEvent(new WindowEvent(windowToClose, WindowEvent.WINDOW_CLOSING));
            }

        });

        exitButton.setBackground(Color.decode("#9B7200"));
        return exitButton;
    }
}