package de.evilcodez.proxypass;

import com.nukkitx.protocol.bedrock.*;
import com.nukkitx.proxypass.Configuration;
import com.nukkitx.proxypass.ProxyPass;
import com.nukkitx.proxypass.network.bedrock.util.LogTo;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.*;

public class ProxyPassWindow {

    private final Map<BedrockSession, PacketLoggerWindow> packetLoggerWindows = new HashMap<>();
    private ProxyPass proxy;
    private JFrame frame;
    private Configuration config;

    public ProxyPassWindow() throws IOException {
        this.config = ProxyPass.loadConfig();
        this.initialize();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                ProxyPass.saveConfig(config);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }));
    }

    private void initialize() {
        frame = new JFrame();
        frame.setTitle("ProxyPass - Minecraft v" + ProxyPass.CODEC.getMinecraftVersion() + " (" + ProxyPass.CODEC.getProtocolVersion() + ")");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 600);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);

        frame.getContentPane().setLayout(new BorderLayout());

        JPanel panelProxyConfig = new JPanel();
        panelProxyConfig.setLayout(new BoxLayout(panelProxyConfig, BoxLayout.Y_AXIS));
        panelProxyConfig.setBorder(new TitledBorder(new BevelBorder(BevelBorder.LOWERED), "Proxy Config", TitledBorder.CENTER, TitledBorder.TOP, new Font("Segoe UI", Font.PLAIN, 18), null));
        frame.getContentPane().add(panelProxyConfig, BorderLayout.NORTH);

        JPanel panelProxyAddress = new JPanel();
        panelProxyAddress.setLayout(new BoxLayout(panelProxyAddress, BoxLayout.X_AXIS));
        panelProxyConfig.add(panelProxyAddress);

        JLabel lblProxyAddress = new JLabel("Proxy Address   ");
        lblProxyAddress.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panelProxyAddress.add(lblProxyAddress);

        JTextField tfProxyAddress = new JTextField();
        tfProxyAddress.setText(config.getProxy().getHost());
        tfProxyAddress.setColumns(10);
        tfProxyAddress.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tfProxyAddress.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                config.getProxy().setHost(tfProxyAddress.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                config.getProxy().setHost(tfProxyAddress.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                config.getProxy().setHost(tfProxyAddress.getText());
            }
        });
        panelProxyAddress.add(tfProxyAddress);

        panelProxyConfig.add(new JLabel(" "));

        JPanel panelProxyPort = new JPanel();
        panelProxyPort.setLayout(new BoxLayout(panelProxyPort, BoxLayout.X_AXIS));
        panelProxyConfig.add(panelProxyPort);

        JLabel lblProxyPort = new JLabel("Proxy Port         ");
        lblProxyPort.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panelProxyPort.add(lblProxyPort);

        JSpinner spProxyPort = new JSpinner();
        spProxyPort.setModel(new SpinnerNumberModel(config.getProxy().getPort(), 0, 65535, 1));
        spProxyPort.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        spProxyPort.addChangeListener(e -> config.getProxy().setPort((int) spProxyPort.getValue()));
        panelProxyPort.add(spProxyPort);

        panelProxyConfig.add(new JLabel(" "));

        JPanel panelServerAddress = new JPanel();
        panelServerAddress.setLayout(new BoxLayout(panelServerAddress, BoxLayout.X_AXIS));
        panelProxyConfig.add(panelServerAddress);

        JLabel lblServerAddress = new JLabel("Server Address  ");
        lblServerAddress.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panelServerAddress.add(lblServerAddress);

        JTextField tfServerAddress = new JTextField();
        tfServerAddress.setText(config.getDestination().getHost());
        tfServerAddress.setColumns(10);
        tfServerAddress.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tfServerAddress.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                config.getDestination().setHost(tfServerAddress.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                config.getDestination().setHost(tfServerAddress.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                config.getDestination().setHost(tfServerAddress.getText());
            }
        });
        panelServerAddress.add(tfServerAddress);

        panelProxyConfig.add(new JLabel(" "));

        JPanel panelServerPort = new JPanel();
        panelServerPort.setLayout(new BoxLayout(panelServerPort, BoxLayout.X_AXIS));
        panelProxyConfig.add(panelServerPort);

        JLabel lblServerPort = new JLabel("Server Port        ");
        lblServerPort.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panelServerPort.add(lblServerPort);

        JSpinner spServerPort = new JSpinner();
        spServerPort.setModel(new SpinnerNumberModel(config.getDestination().getPort(), 0, 65535, 1));
        spServerPort.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        spServerPort.addChangeListener(e -> config.getDestination().setPort((int) spServerPort.getValue()));
        panelServerPort.add(spServerPort);

        JPanel panelProxyOptions = new JPanel();
        panelProxyOptions.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panelProxyConfig.add(panelProxyOptions);

        JCheckBox cbxPacketTesting = new JCheckBox("Packet Testing");
        cbxPacketTesting.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cbxPacketTesting.setSelected(config.isPacketTesting());
        cbxPacketTesting.addActionListener(e -> config.setPacketTesting(cbxPacketTesting.isSelected()));
        panelProxyOptions.add(cbxPacketTesting);

        JCheckBox cbxLoggingPackets = new JCheckBox("Logging Packets");
        cbxLoggingPackets.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cbxLoggingPackets.setSelected(config.isLoggingPackets());
        cbxLoggingPackets.addActionListener(e -> config.setLoggingPackets(cbxLoggingPackets.isSelected()));
        panelProxyOptions.add(cbxLoggingPackets);

        JLabel lblLogTo = new JLabel("Log To");
        lblLogTo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panelProxyOptions.add(lblLogTo);

        JComboBox<LogTo> cbLogTo = new JComboBox<>();
        cbLogTo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cbLogTo.setModel(new DefaultComboBoxModel<>(LogTo.values()));
        cbLogTo.setSelectedItem(config.getLogTo());
        cbLogTo.addActionListener(e -> config.setLogTo((LogTo) cbLogTo.getSelectedItem()));
        panelProxyOptions.add(cbLogTo);

        JLabel lblMaxClients = new JLabel("Max Clients");
        lblMaxClients.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panelProxyOptions.add(lblMaxClients);

        JSpinner spMaxClients = new JSpinner();
        spMaxClients.setModel(new SpinnerNumberModel(config.getMaxClients(), 0, null, 1));
        spMaxClients.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        spMaxClients.setPreferredSize(new Dimension(80, 25));
        spMaxClients.addChangeListener(e -> config.setMaxClients((int) spMaxClients.getValue()));
        panelProxyOptions.add(spMaxClients);

        JPanel panelIgnoredPackets = new JPanel();
        panelIgnoredPackets.setLayout(new BorderLayout());
        panelIgnoredPackets.setBorder(new TitledBorder(new BevelBorder(BevelBorder.LOWERED), "Ignored Packets", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION, new Font("Segoe UI", Font.PLAIN, 18), null));
        frame.getContentPane().add(panelIgnoredPackets, BorderLayout.CENTER);

        JList<String> packetList = new JList<>();
        final DefaultListModel<String> packetModel = new DefaultListModel<>();
        JList<String> ignoredList = new JList<>();
        final DefaultListModel<String> ignoredModel = new DefaultListModel<>();

        JScrollPane scrollPanePacketList = new JScrollPane();
        scrollPanePacketList.setPreferredSize(new Dimension(255, 0));
        panelIgnoredPackets.add(scrollPanePacketList, BorderLayout.WEST);

        packetList.setModel(packetModel);
        packetList.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        this.getAllPackets().stream().map(Class::getSimpleName).forEach(packet -> {
            (config.getIgnoredPackets().contains(packet) ? ignoredModel : packetModel).addElement(packet);
        });
        packetList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                    final int index = packetList.getSelectedIndex();
                    if(index < 0) return;
                    final String packet = packetModel.remove(index);
                    ignoredModel.addElement(packet);
                    config.getIgnoredPackets().add(packet);
                    sortList(ignoredModel);
                }
            }
        });
        scrollPanePacketList.setViewportView(packetList);

        JScrollPane scrollPaneIgnoredList = new JScrollPane();
        scrollPaneIgnoredList.setPreferredSize(new Dimension(255, 0));
        panelIgnoredPackets.add(scrollPaneIgnoredList, BorderLayout.EAST);

        ignoredList.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        ignoredList.setModel(ignoredModel);
        ignoredList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                    final int index = ignoredList.getSelectedIndex();
                    if(index < 0) return;
                    final String packet = ignoredModel.remove(index);
                    packetModel.addElement(packet);
                    config.getIgnoredPackets().remove(packet);
                    sortList(packetModel);
                }
            }
        });
        scrollPaneIgnoredList.setViewportView(ignoredList);

        JPanel panelIgnoreButtons = new JPanel();
        panelIgnoreButtons.setLayout(new BoxLayout(panelIgnoreButtons, BoxLayout.Y_AXIS));
        panelIgnoredPackets.add(panelIgnoreButtons, BorderLayout.CENTER);

        panelIgnoreButtons.add(Box.createVerticalGlue());

        JButton btnAddIgnoredPacket = new JButton(">>");
        btnAddIgnoredPacket.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnAddIgnoredPacket.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        btnAddIgnoredPacket.addActionListener(e -> {
            final int[] indices = packetList.getSelectedIndices();
            for (int index : indices) {
                final String packet = packetModel.getElementAt(index);
                ignoredModel.addElement(packet);
                config.getIgnoredPackets().add(packet);
            }
            this.sortList(ignoredModel);
            for (int i = indices.length - 1; i >= 0; i--) {
                final int index = indices[i];
                packetModel.removeElementAt(index);
            }
        });
        panelIgnoreButtons.add(btnAddIgnoredPacket);

        JButton btnRemoveIgnoredPacket = new JButton("<<");
        btnRemoveIgnoredPacket.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnRemoveIgnoredPacket.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        btnRemoveIgnoredPacket.addActionListener(e -> {
            final int[] indices = ignoredList.getSelectedIndices();
            for (int index : indices) {
                final String packet = ignoredModel.getElementAt(index);
                packetModel.addElement(packet);
                config.getIgnoredPackets().remove(packet);
            }
            this.sortList(packetModel);
            for (int i = indices.length - 1; i >= 0; i--) {
                final int index = indices[i];
                ignoredModel.removeElementAt(index);
            }
        });
        panelIgnoreButtons.add(btnRemoveIgnoredPacket);

        panelIgnoreButtons.add(Box.createVerticalGlue());

        JPanel panelLaunch = new JPanel();
        panelLaunch.setLayout(new FlowLayout(FlowLayout.CENTER));
        frame.getContentPane().add(panelLaunch, BorderLayout.SOUTH);

        JButton btnLaunch = new JButton("Launch");
        btnLaunch.setFont(new Font("Segoe UI", Font.BOLD, 24));
        btnLaunch.addActionListener(e -> {
            if(btnLaunch.getText().equals("Launch")) {
                if(this.proxy != null) return;
                btnLaunch.setEnabled(false);
                this.setComponentEnabled(panelProxyConfig, false);
                this.setComponentEnabled(panelIgnoredPackets, false);
                new Thread(() -> {
                    if(this.proxy != null) return;
                    try {
                        this.proxy = ProxyPass.launch(config);
                        this.proxy.onNewClient = this::newClientCallback;
                        this.proxy.onLogPacket = this::logPacketCallback;
                        btnLaunch.setText("Stop");
                        btnLaunch.setEnabled(true);
                        proxy.boot();
                    } catch (Exception ex) {
                        try {
                            this.proxy.shutdown();
                        }catch (Exception ignored) {}
                        this.proxy = null;
                        this.setComponentEnabled(panelProxyConfig, true);
                        this.setComponentEnabled(panelIgnoredPackets, true);
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(frame, ex.getClass().getSimpleName() + " - " + ex.getMessage(), "Failed to start ProxyPass server!", JOptionPane.ERROR_MESSAGE);
                        btnLaunch.setText("Launch");
                        btnLaunch.setEnabled(true);
                    }
                }, "Proxy Pass Thread").start();
            }else {
                if(this.proxy == null) return;
                btnLaunch.setEnabled(false);
                new Thread(() -> {
                    try {
                        Thread.sleep(250L);
                    } catch (InterruptedException ex) {
                        return;
                    }
                    this.proxy.shutdown();
                    this.proxy = null;
                    this.packetLoggerWindows.values().forEach(PacketLoggerWindow::close);
                    this.packetLoggerWindows.clear();
                    btnLaunch.setEnabled(true);
                    this.setComponentEnabled(panelProxyConfig, true);
                    this.setComponentEnabled(panelIgnoredPackets, true);
                    JOptionPane.showMessageDialog(frame, "ProxyPass server stopped!", "Information", JOptionPane.INFORMATION_MESSAGE);
                    btnLaunch.setText("Launch");
                }).start();
            }
        });
        panelLaunch.add(btnLaunch);

        frame.setVisible(true);
    }

    private void newClientCallback(BedrockServerSession downstream, BedrockClientSession upstream) {
        final PacketLoggerWindow loggerWindow = new PacketLoggerWindow(this, downstream, upstream);
        packetLoggerWindows.put(downstream, loggerWindow);
        packetLoggerWindows.put(upstream, loggerWindow);
        loggerWindow.show();
    }

    private List<BedrockPacket> logPacketCallback(BedrockSession session, BedrockPacket packet) {
        final PacketLoggerWindow loggerWindow = packetLoggerWindows.get(session);
        if(loggerWindow != null) {
            return loggerWindow.logPackets(session, packet);
        }
        return null;
    }

    private List<Class<?>> getAllPackets() {
        final BedrockPacketCodec codec = ProxyPass.CODEC;
        try {
            final Field field = BedrockPacketCodec.class.getDeclaredField("packetsByClass");
            field.setAccessible(true);
            final Map<Class<? extends BedrockPacket>, BedrockPacketDefinition<? extends BedrockPacket>> packetMap = (Map<Class<? extends BedrockPacket>, BedrockPacketDefinition<? extends BedrockPacket>>) field.get(codec);
            final List<Class<?>> packetClasses = new ArrayList<>(packetMap.keySet());
            packetClasses.sort(Comparator.comparing(Class::getSimpleName));
            return packetClasses;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Set<String> getIgnoredPackets(DefaultListModel<String> list) {
        final Set<String> ignoredPackets = new HashSet<>();
        for (int i = 0; i < list.getSize(); i++) {
            ignoredPackets.add(list.getElementAt(i));
        }
        return ignoredPackets;
    }

    private void sortList(DefaultListModel<String> model) {
        final List<String> list = new ArrayList<>(model.getSize());
        for (int i = 0; i < model.getSize(); i++) {
            list.add(model.getElementAt(i));
        }
        list.sort(null);
        model.removeAllElements();
        list.forEach(model::addElement);
    }

    private void setComponentEnabled(Component component, boolean enabled) {
        component.setEnabled(enabled);
        if(component instanceof Container) {
            final Container container = (Container) component;
            for (Component child : container.getComponents()) {
                this.setComponentEnabled(child, enabled);
            }
        }
    }

    public Map<BedrockSession, PacketLoggerWindow> getPacketLoggerWindows() {
        return packetLoggerWindows;
    }

    public Configuration getConfig() {
        return config;
    }

    public JFrame getFrame() {
        return frame;
    }

    public ProxyPass getProxy() {
        return proxy;
    }
}
