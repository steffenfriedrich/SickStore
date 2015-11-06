package de.unihamburg.sickstore.backend.anomaly.clientdelay;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * Created by Friedrich on 05.11.2015.
 */
public class ThroughputTest {

    @Test
    public void testRecomputeActualThroughput() throws Exception {



        Random rnd = new Random();

        Throughput th3 = new Throughput(400, 20, 20);

        long time = 0;
        long lastTime = 0;
        for (int i = 0; i < 100; i++) {

            long now = System.currentTimeMillis();
            int r = rnd.nextInt(4);
            long latency  = (int) Math.ceil(th3.getQueueingLatency(now));

            double throughput = 0;
            if(lastTime > 0) {
                time += now - lastTime;
                throughput  = 1000.0 * i / time;
            }
            System.out.println("delay:" + r + ", latency:" + latency + ",  throughput:" + throughput);
            lastTime = now;
            Thread.sleep(latency + r);
        }

    }
}