package de.unihamburg.sickstore.backend.anomaly.clientdelay;

import de.unihamburg.sickstore.backend.measurement.Measurements;
import de.unihamburg.sickstore.backend.timer.FakeTimeHandler;
import de.unihamburg.sickstore.backend.timer.SystemTimeHandler;
import de.unihamburg.sickstore.backend.timer.TimeHandler;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * Created by Friedrich on 05.11.2015.
 */
public class ThroughputTest {

    @Test
    public void testRecomputeActualThroughput() throws Exception {
        TimeHandler timeHandler = new FakeTimeHandler();
        Throughput th = new Throughput(2, 0, 0);
        long startTime = 1447071367828l;

        timeHandler.sleep(startTime);
        long ops = 0;
        long now = timeHandler.getCurrentTime();
        double latency = 0.0;
        latency = th.getQueueingLatency(now);
        ops++;
        double throughput = 1000.0 * ops / (now - startTime);

        timeHandler.sleep(250);
        for (int i = 0; i < 1000 ; i++) {
            timeHandler.sleep((int) Math.ceil(latency));
            now = timeHandler.getCurrentTime();
            latency = th.getQueueingLatency(now);
            ops++;
            throughput = 1000.0 * ops / (now - startTime);
            //System.out.println("now - startTime:" + (now - startTime)  + ", latency:" + latency + ", throughput:" + throughput);
        }
        assertEquals(throughput, 2.0, 0.01);
    }

    @Test
    public void testHickup() throws Exception {
        Measurements measurements = Measurements.getMeasurements();
        TimeHandler timeHandler = new FakeTimeHandler();



        Throughput th = new Throughput(20, 20000, 10000);
        long startTime = 1447071367828l;
        timeHandler.sleep(startTime);
        long ops = 0;
        long now = timeHandler.getCurrentTime();
        double latency = 0.0;
        latency = th.getQueueingLatency(now);
        ops++;
        double throughput = 1000.0 * ops / (now - startTime);

        timeHandler.sleep(200);
        for (int i = 0; i < 1000 ; i++) {
            timeHandler.sleep(200);
            now = timeHandler.getCurrentTime();
            latency = th.getQueueingLatency(now);
            measurements.measure("INSERT", Math.max(1000, (int) Math.ceil(latency)));
            ops++;
            throughput = 1000.0 * ops / (now - startTime);
            System.out.println((now - startTime)  + ";" + latency + ";" + throughput);
        }

        // coordinated omission !
        assertEquals(2,2);
    }
}