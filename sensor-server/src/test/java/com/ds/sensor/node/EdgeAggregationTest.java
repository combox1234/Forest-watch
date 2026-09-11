package com.ds.sensor.node;

import com.ds.sensor.node.config.NodeConfig;
import com.ds.sensor.node.service.CommunicationService;
import com.ds.sensor.node.service.EdgeAggregationService;
import com.ds.sensor.node.service.VectorClockService;
import com.ds.sensor.shared.message.AggregatedData;
import com.ds.sensor.shared.message.SensorReadingData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EdgeAggregationTest {

    @Test
    public void testAggregationCalculations() {
        NodeConfig config = new NodeConfig("S2", "C1", 8102, "http://localhost:8101", "http://localhost:8000", true);
        CommunicationService comm = new CommunicationService(config, null) {
            @Override
            public void sendAggregatedDataToCentral(AggregatedData data) {}
        };
        VectorClockService vcService = new VectorClockService(config);
        EdgeAggregationService edgeAggregationService = new EdgeAggregationService(config, comm, vcService);

        // Feed 3 samples
        SensorReadingData r1 = new SensorReadingData("S1", "C1", 20.0, 50.0, false, 95.0, 100.0, null);
        SensorReadingData r2 = new SensorReadingData("S2", "C1", 26.0, 60.0, true, 90.0, 100.0, null);
        SensorReadingData r3 = new SensorReadingData("S1", "C1", 23.0, 55.0, false, 94.0, 100.0, null);

        edgeAggregationService.recordReading(r1);
        edgeAggregationService.recordReading(r2);
        edgeAggregationService.recordReading(r3);

        AggregatedData agg = edgeAggregationService.computeAndSendAggregation(null);

        assertNotNull(agg);
        assertEquals(3, agg.getSampleCount());
        assertEquals(23.0, agg.getAvgTemperature(), 0.01);
        assertEquals(20.0, agg.getMinTemperature(), 0.01);
        assertEquals(26.0, agg.getMaxTemperature(), 0.01);
        assertEquals(55.0, agg.getAvgHumidity(), 0.01);
        assertEquals(2, agg.getActiveSensors()); // S1 and S2
        assertEquals(1, agg.getEventCount());
    }
}
