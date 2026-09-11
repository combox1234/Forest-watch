package com.ds.sensor.node;

import com.ds.sensor.node.config.NodeConfig;
import com.ds.sensor.node.service.CommunicationService;
import com.ds.sensor.node.service.MutualExclusionService;
import com.ds.sensor.node.service.VectorClockService;
import com.ds.sensor.shared.message.AlertData;
import com.ds.sensor.shared.message.Message;
import com.ds.sensor.shared.message.MessageType;
import com.ds.sensor.shared.message.VectorClockData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MutualExclusionTest {

    @Test
    public void testAlertDeduplicationOnClusterHead() {
        NodeConfig config = new NodeConfig("S2", "C1", 8102, "http://localhost:8101", "http://localhost:8000", true);
        CommunicationService comm = new CommunicationService(config, null) {
            @Override
            public void sendAlertToCentral(AlertData alertData) {}
        };
        VectorClockService vcService = new VectorClockService(config);
        MutualExclusionService mutualExclusionService = new MutualExclusionService(config, comm, vcService);

        Message req1 = new Message();
        req1.setSenderId("S1");
        req1.setClusterId("C1");
        req1.setMessageType(MessageType.ALERT_REQUEST);
        req1.setPayload("ANIMAL_DETECTED");
        req1.setVectorClock(new VectorClockData(1, 0, 0, 0));

        // First alert request should be granted
        Message resp1 = mutualExclusionService.handleAlertRequest(req1);
        assertEquals(MessageType.ALERT_GRANTED, resp1.getMessageType());

        // Concurrent duplicate request from S2 or another sensor in same window
        Message req2 = new Message();
        req2.setSenderId("S2");
        req2.setClusterId("C1");
        req2.setMessageType(MessageType.ALERT_REQUEST);
        req2.setPayload("ANIMAL_DETECTED");
        req2.setVectorClock(new VectorClockData(0, 1, 0, 0));

        // Second request should be deduplicated (REJECTED)
        Message resp2 = mutualExclusionService.handleAlertRequest(req2);
        assertEquals(MessageType.ALERT_REJECTED, resp2.getMessageType());
    }
}
