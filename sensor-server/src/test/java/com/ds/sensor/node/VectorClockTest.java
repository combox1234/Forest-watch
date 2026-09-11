package com.ds.sensor.node;

import com.ds.sensor.shared.message.VectorClockData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VectorClockTest {

    @Test
    public void testVectorClockIncrement() {
        VectorClockData vc = new VectorClockData();
        assertArrayEquals(new int[]{0, 0, 0, 0}, vc.getClock());

        vc.increment("S1");
        assertArrayEquals(new int[]{1, 0, 0, 0}, vc.getClock());

        vc.increment("S2");
        assertArrayEquals(new int[]{1, 1, 0, 0}, vc.getClock());

        vc.increment("S4");
        assertArrayEquals(new int[]{1, 1, 0, 1}, vc.getClock());
    }

    @Test
    public void testVectorClockMerge() {
        VectorClockData vc1 = new VectorClockData(2, 0, 1, 0);
        VectorClockData vc2 = new VectorClockData(1, 3, 0, 4);

        vc1.merge(vc2);
        assertArrayEquals(new int[]{2, 3, 1, 4}, vc1.getClock());
    }

    @Test
    public void testCausalOrderingAndConcurrency() {
        VectorClockData vc1 = new VectorClockData(1, 0, 0, 0);
        VectorClockData vc2 = new VectorClockData(2, 0, 0, 0);
        VectorClockData vc3 = new VectorClockData(0, 1, 0, 0);

        // vc1 -> vc2 (happens-before)
        assertTrue(vc1.happensBefore(vc2));
        assertFalse(vc2.happensBefore(vc1));

        // vc1 and vc3 are concurrent
        assertFalse(vc1.happensBefore(vc3));
        assertFalse(vc3.happensBefore(vc1));
        assertTrue(vc1.isConcurrent(vc3));
        assertTrue(vc3.isConcurrent(vc1));
    }
}
