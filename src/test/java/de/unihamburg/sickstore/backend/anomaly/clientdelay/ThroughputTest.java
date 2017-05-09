package de.unihamburg.sickstore.backend.anomaly.clientdelay;

import de.unihamburg.sickstore.backend.measurement.Measurements;
import de.unihamburg.sickstore.backend.timer.FakeTimeHandler;

import de.unihamburg.sickstore.backend.timer.TimeHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


import static org.junit.Assert.*;

/**
 * Created by Steffen Friedrich on 05.11.2015.
 */
public class ThroughputTest {

    @Test
    public void testMaxThroughput() throws Exception {
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
        for (int i = 0; i < 1000; i++) {
            timeHandler.sleep((int) Math.ceil(latency));
            now = timeHandler.getCurrentTime();
            latency = th.getQueueingLatency(now);
            ops++;
            throughput = 1000.0 * ops / (now - startTime);
        }
        assertEquals(throughput, 2.0, 0.01);
    }

    @Test
    public void testHickup() throws Exception {
        long lastRecoveryTime = 1000;
        for (int k = 0; k < 10; k++) {
            int maxThroughput = 1000 + 100 * k;

            Measurements measurements = new Measurements(false);
            File file = new File("results/queueing_latency_test/timeseries-queueing_latency-" + maxThroughput + ".dat");
            FileUtils.forceMkdir(file.getParentFile());

            List<String> timeseries = new ArrayList<String>();
            timeseries.add("time;latency");

            TimeHandler timeHandler = new FakeTimeHandler();
            Throughput th = new Throughput(maxThroughput, 100, 100);
            long startTime = 1447071367828l;
            long hickupStarTime = 0;
            boolean firstHickupOp = true;
            timeHandler.sleep(startTime);
            long now = timeHandler.getCurrentTime();
            double latency = th.getQueueingLatency(now);

            long recoveryTime = 0;

            timeHandler.sleep(2);
            for (int i = 0; i < 200; i++) {
                timeHandler.sleep(2);
                now = timeHandler.getCurrentTime();
                latency = th.getQueueingLatency(now);

                if (firstHickupOp && latency > 0.0) {
                    firstHickupOp = false;
                    hickupStarTime = timeHandler.getCurrentTime();
                } else if (!firstHickupOp && latency <= 0.0) {
                    firstHickupOp = true;
                    recoveryTime = timeHandler.getCurrentTime() - hickupStarTime;
                }

                measurements.measure("queueing_latency", (int) Math.ceil(latency));

                timeseries.add((now - startTime) + ";" + latency);
            }
            FileUtils.writeLines(file, timeseries);

            measurements.finishMeasurement("queueing_latency_test", "" + maxThroughput);

            // higher max throughput -> lower recovery time after hickup
            assertTrue((lastRecoveryTime > recoveryTime));
            lastRecoveryTime = recoveryTime;
        }

    }


    @Test
    public void testExponential() throws Exception {
        ExponentialDistribution exp = new ExponentialDistribution(4);
        for (int i = 0; i < 1000 ; i++) {

        }
    }
}