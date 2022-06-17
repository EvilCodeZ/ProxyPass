package de.evilcodez.proxypass;

import javax.swing.*;
import java.awt.*;

public class Main {

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                new ProxyPassWindow();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
