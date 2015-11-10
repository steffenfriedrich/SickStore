package de.unihamburg.sickstore.backend.anomaly.clientdelay;

import de.unihamburg.sickstore.backend.measurement.Measurements;
import de.unihamburg.sickstore.backend.timer.FakeTimeHandler;

import de.unihamburg.sickstore.backend.timer.TimeHandler;
import org.junit.Test;

import java.io.InputStream;


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



        int maxThroughput = 1200;
        Throughput th = new Throughput(maxThroughput, 100, 100);
        long startTime = 1447071367828l;
        long hickupStarTime = 0;
        boolean firstHickupOp = true;
        timeHandler.sleep(startTime);
        long ops = 0;
        long now = timeHandler.getCurrentTime();
        double latency = th.getQueueingLatency(now);
        ops++;

        timeHandler.sleep(2);
        for (int i = 0; i < 200 ; i++) {
            timeHandler.sleep(2);
            now = timeHandler.getCurrentTime();
            latency = th.getQueueingLatency(now);

            if(firstHickupOp && latency > 0.0) {
                firstHickupOp = false;
                hickupStarTime = timeHandler.getCurrentTime();
            } else if(!firstHickupOp && latency == 0.0) {
                firstHickupOp = true;
                System.out.println(timeHandler.getCurrentTime() - hickupStarTime);
            }

            measurements.measure("TEST", (int) Math.ceil(latency));
            ops++;

            //System.out.println((now - startTime)  + ";" + latency);
        }


        // measurement output
        measurements.finishMeasurement(maxThroughput + "");

        // coordinated omission !
        assertEquals(2,2);
    }
}