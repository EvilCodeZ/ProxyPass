package de.evilcodez.proxypass;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nukkitx.protocol.bedrock.BedrockPacket;
import com.nukkitx.proxypass.ProxyPass;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
public class LoggedPacket {

    private final BedrockPacket packet;
    private final boolean upstream;
    private final long time;
    private final int timeDelta;
    @Setter(AccessLevel.NONE) private String serializedPacket;

    public LoggedPacket(BedrockPacket packet, boolean upstream, long time, int timeDelta) {
        this.packet = packet;
        this.upstream = upstream;
        this.time = time;
        this.timeDelta = timeDelta;
    }

    public String getSerializedPacket() {
        if(serializedPacket == null) {
            try {
                final JsonNode node = ProxyPass.JSON_MAPPER.valueToTree(packet);
                final ObjectNode objectNode = (ObjectNode) node;
                objectNode.remove("packetId");
                objectNode.remove("senderId");
                objectNode.remove("clientId");
                objectNode.remove("packetType");
                this.serializedPacket = ProxyPass.JSON_MAPPER.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS).writerWithDefaultPrettyPrinter().writeValueAsString(objectNode);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return serializedPacket;
    }
}
