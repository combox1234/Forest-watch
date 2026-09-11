package com.ds.sensor.node;

import com.ds.sensor.node.config.NodeConfig;
import com.ds.sensor.node.service.CommunicationService;
import com.ds.sensor.node.service.ElectionService;
import com.ds.sensor.node.service.VectorClockService;
import com.ds.sensor.shared.message.Message;
import com.ds.sensor.shared.model.NodeRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BullyElectionTest {

    @Test
    public void testBullyPriorityOrdering() {
        NodeConfig config = new NodeConfig("S1", "C1", 8101, "http://localhost:8102", "http://localhost:8000", false);
        CommunicationService comm = new CommunicationService(config, null) {
            @Override
            public void sendElectionEventToCentral(String c, String i, String l, String e, String d) {}
        };
        VectorClockService vcService = new VectorClockService(config);
        ElectionService electionService = new ElectionService(config, comm, vcService);

        assertEquals(4, electionService.getNodePriority("S4"));
        assertEquals(3, electionService.getNodePriority("S3"));
        assertEquals(2, electionService.getNodePriority("S2"));
        assertEquals(1, electionService.getNodePriority("S1"));
    }

    @Test
    public void testS1WinsElectionWhenS2IsDown() {
        NodeConfig config = new NodeConfig("S1", "C1", 8101, "http://localhost:8102", "http://localhost:8000", false);
        // S2 is down: sendToPeer returns null
        CommunicationService comm = new CommunicationService(config, null) {
            @Override
            public Message sendToPeer(String peerUrl, Message message) {
                return null; // simulated offline
            }
            @Override
            public void sendElectionEventToCentral(String c, String i, String l, String e, String d) {}
            @Override
            public void notifyCentralClusterHead(String c, String h, int p) {}
        };
        VectorClockService vcService = new VectorClockService(config);
        ElectionService electionService = new ElectionService(config, comm, vcService);

        assertEquals(NodeRole.SENSOR, electionService.getCurrentRole());

        // Start election
        electionService.startElection("Peer S2 heartbeat timeout");

        // S1 must become coordinator
        assertEquals(NodeRole.CLUSTER_HEAD, electionService.getCurrentRole());
        assertEquals("S1", electionService.getCurrentLeaderId());
        assertTrue(electionService.isClusterHead());
    }
}
