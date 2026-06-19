package io.kiponos.demo.control;

import io.kiponos.demo.view.CommPanelView;
import io.kiponos.sdk.Kiponos;
import io.kiponos.sdk.configs.Folder;

import javax.swing.*;

public class CommPanelMain {
    public static final Kiponos cfg = Kiponos.createForCurrentTeam();
    public static final Folder commPanelCfg = cfg.getRootFolder().folderOrCreate("Demo").folderOrCreate("CommPanel");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CommPanelView().setVisible(true));
    }
}