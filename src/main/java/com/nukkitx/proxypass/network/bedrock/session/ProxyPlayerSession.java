package com.nukkitx.proxypass.network.bedrock.session;

import com.nukkitx.network.util.DisconnectReason;
import com.nukkitx.protocol.bedrock.BedrockClientSession;
import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.protocol.bedrock.BedrockServerSession;
import com.nukkitx.protocol.bedrock.BedrockSession;
import com.nukkitx.protocol.bedrock.exception.PacketSerializeException;
import com.nukkitx.protocol.bedrock.handler.BatchHandler;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.UnknownPacket;
import com.nukkitx.protocol.bedrock.util.EncryptionUtils;
import com.nukkitx.proxypass.ProxyPass;
import com.nukkitx.proxypass.network.bedrock.logging.SessionLogger;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Log4j2
@Getter
public class ProxyPlayerSession {
    private final BedrockServerSession upstream;
    private final BedrockClientSession downstream;
    private final ProxyPass proxy;
    private final AuthData authData;
    private final long timestamp = System.currentTimeMillis();
    @Getter(AccessLevel.PACKAGE)
    private final KeyPair proxyKeyPair = EncryptionUtils.createKeyPair();
    private volatile boolean closed = false;

    public final SessionLogger logger;

    public ProxyPlayerSession(BedrockServerSession upstream, BedrockClientSession downstream, ProxyPass proxy, AuthData authData) {
        this.upstream = upstream;
        this.downstream = downstream;
        this.proxy = proxy;
        this.authData = authData;
        this.upstream.addDisconnectHandler(reason -> {
            if (reason != DisconnectReason.DISCONNECTED) {
                this.downstream.disconnect();
            }
        });
        this.logger = new SessionLogger(
                proxy,
                proxy.getSessionsDir(),
                this.authData.getDisplayName(),
                timestamp
        );
        logger.start();
    }

    public BatchHandler getUpstreamBatchHandler() {
        return new ProxyBatchHandler(downstream, logger, true);
    }

    public BatchHandler getDownstreamTailHandler() {
        return new ProxyBatchHandler(upstream, logger, false);
    }

    private class ProxyBatchHandler implements BatchHandler {
        private final SessionLogger logger;
        private final BedrockSession session;
        private final boolean upstream;

        private ProxyBatchHandler(BedrockSession session, SessionLogger logger, boolean upstream) {
            this.session = session;
            this.logger = logger;
            this.upstream = upstream;
        }

        @Override
        public void handle(BedrockSession session, ByteBuf compressed, Collection<BedrockPacket> packets) {
            boolean packetTesting = ProxyPlayerSession.this.proxy.getConfiguration().isPacketTesting();
            boolean batchHandled = false;
            List<BedrockPacket> unhandled = new ArrayList<>();

            for (BedrockPacket packet : packets) {
                // Additions
                final List<BedrockPacket> additionalPackets = new ArrayList<>();
                if(proxy.onLogPacket != null) {
                    final List<BedrockPacket> output = proxy.onLogPacket.apply(session, packet);
                    if(output != null) {
                        additionalPackets.addAll(output);
                        batchHandled = true;
                    }else {
                        additionalPackets.add(packet);
                    }
                }else {
                    additionalPackets.add(packet);
                }
                // Additions

                for (BedrockPacket packet2 : additionalPackets) {
                    logger.logPacket(session, packet2, upstream);

                    BedrockPacketHandler handler = session.getPacketHandler();

                    if (handler != null && packet2.handle(handler)) {
                        batchHandled = true;
                    } else {
                        unhandled.add(packet2);
                    }

                    if (packetTesting && !(packet2 instanceof UnknownPacket)) {
                        int packetId = ProxyPass.CODEC.getId(packet2.getClass());
                        ByteBuf buffer = ByteBufAllocator.DEFAULT.ioBuffer();
                        try {
                            ProxyPass.CODEC.tryEncode(buffer, packet2, session);
                            BedrockPacket packet3 = ProxyPass.CODEC.tryDecode(buffer, packetId, session);
                            if (!Objects.equals(packet2, packet3)) {
                                // Something went wrong in serialization.
                                log.warn("Packets instances not equal:\n Original  : {}\nRe-encoded : {}",
                                        packet2, packet3);
                            }
                        } catch (PacketSerializeException e) {
                            //ignore
                        } finally {
                            buffer.release();
                        }
                    }
                }
            }

            if (!batchHandled) {
                compressed.resetReaderIndex();
                this.session.sendWrapped(compressed, true);
            } else if (!unhandled.isEmpty()) {
                this.session.sendWrapped(unhandled, true);
            }
        }

    }

}
