package de.evilcodez.proxypass;

import com.nukkitx.protocol.bedrock.BedrockClientSession;
import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.BedrockServerSession;
import com.nukkitx.protocol.bedrock.BedrockSession;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.List;

public class PacketLoggerWindow implements ListCellRenderer<LoggedPacket> {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");
    private final ProxyPassWindow mainWindow;
    private final BedrockServerSession downstream;
    private final BedrockClientSession upstream;
    private JFrame frame;
    private JList<LoggedPacket> packetLogList;
    private DefaultListModel<LoggedPacket> packetLogModel;
    private LoggedPacket lastUpstreamPacket;
    private LoggedPacket lastDownstreamPacket;
    private JTextArea packetDataTextArea;

    public PacketLoggerWindow(ProxyPassWindow mainWindow, BedrockServerSession downstream, BedrockClientSession upstream) {
        this.mainWindow = mainWindow;
        this.downstream = downstream;
        this.upstream = upstream;
        this.initialize();
    }

    private void initialize() {
        frame = new JFrame();
        frame.setTitle("Proxy Pass - Packet Logger");
        frame.setSize(900, 550);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLocationRelativeTo(mainWindow.getFrame());
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                mainWindow.getPacketLoggerWindows().remove(downstream);
                mainWindow.getPacketLoggerWindows().remove(upstream);
                try {
                    if(!downstream.isClosed()) downstream.disconnect();
                }catch (Exception ignored) {}
                try {
                    if(!upstream.isClosed()) upstream.disconnect();
                }catch (Exception ignored) {}
            }
        });

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(0.5);
        splitPane.setResizeWeight(0.5);
        frame.getContentPane().add(splitPane, BorderLayout.CENTER);

        JScrollPane scrollPaneLeft = new JScrollPane();
        splitPane.setLeftComponent(scrollPaneLeft);

        packetLogList = new JList<>();
        packetLogList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        packetLogList.setFixedCellHeight(30);
        packetLogModel = new DefaultListModel<>();
        packetLogList.setModel(packetLogModel);
        packetLogList.setCellRenderer(this);
        packetLogList.addListSelectionListener(e -> {
            if(packetLogList.getSelectedIndex() == -1) return;
            final LoggedPacket packet = packetLogModel.getElementAt(packetLogList.getSelectedIndex());
            packetDataTextArea.setText(packet.getSerializedPacket());
        });
        scrollPaneLeft.setViewportView(packetLogList);

        JScrollPane scrollPaneRight = new JScrollPane();
        splitPane.setRightComponent(scrollPaneRight);

        packetDataTextArea = new JTextArea();
        packetDataTextArea.setEditable(false);
        packetDataTextArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        scrollPaneRight.setViewportView(packetDataTextArea);
    }

    private void logPacket(BedrockPacket packet, boolean upstream) {
        final long now = System.currentTimeMillis();
        final int delta = (int) (upstream ? (lastUpstreamPacket == null ? 0 : now - lastUpstreamPacket.getTime()) : (lastDownstreamPacket == null ? 0 : now - lastDownstreamPacket.getTime()));
        final LoggedPacket loggedPacket = new LoggedPacket(packet, upstream, System.currentTimeMillis(), delta);
        packetLogModel.addElement(loggedPacket);

        if(upstream) {
            lastUpstreamPacket = loggedPacket;
        } else {
            lastDownstreamPacket = loggedPacket;
        }
    }

    public List<BedrockPacket> logPackets(BedrockSession session, BedrockPacket packet) {
        final boolean upstream = session instanceof BedrockServerSession;
        if(!mainWindow.getProxy().isIgnoredPacket(packet.getClass())) {
            this.logPacket(packet, upstream);
        }
        return null;
    }

    public void show() {
        frame.setVisible(true);
    }

    public void close() {
        frame.setVisible(false);
        frame.dispose();
    }

    private static final Font FONT = new Font("Segoe UI", Font.PLAIN, 20);

    @Override
    public Component getListCellRendererComponent(JList<? extends LoggedPacket> list, LoggedPacket value, int index, boolean isSelected, boolean cellHasFocus) {
        final JPanel panel = new JPanel();
        if(isSelected) {
            panel.setBackground(UIManager.getColor("List.selectionBackground"));
        }
        panel.setLayout(new BorderLayout());

        String name = value.getPacket().getClass().getSimpleName();
        if(name.toLowerCase().endsWith("packet")) {
            name = name.substring(0, name.length() - 6);
        }

        JLabel label = new JLabel("[" + (value.isUpstream() ? "UP" : "DOWN") + "] " + name);
        label.setFont(FONT);
        panel.add(label, BorderLayout.CENTER);

        String timeStr = "";
        if(index > 0) {
            timeStr += "(" + value.getTimeDelta() + "ms) ";
        }
        timeStr += DATE_FORMAT.format(value.getTime());
        JLabel timeLabel = new JLabel(timeStr);
        timeLabel.setFont(FONT);
        timeLabel.setForeground(Color.GRAY);
        panel.add(timeLabel, BorderLayout.EAST);

        return panel;
    }
}
