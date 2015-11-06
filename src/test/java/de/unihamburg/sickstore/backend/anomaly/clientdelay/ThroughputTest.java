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

        Throughput th1 = new Throughput(2000);

        for (int i = 0; i < 100; i++) {
            th1.correctDelay(5);
        }
        assertEquals(th1.getThroughput(), 200.0, 0.0);

        for (int i = 0; i < 50; i++) {
            th1.correctDelay(5);
        }
        assertEquals(th1.getThroughput(), 200.0, 0.0);

        th1.correctDelay(500);
        assertEquals(th1.getThroughput(), 1000.0 * 151 / 1250, 0.0);


        Throughput th2 = new Throughput(100);
        long cor1 = th2.correctDelay(5);
        long cor2 = th2.correctDelay(5);
        assertEquals(th2.getThroughput(), 100, 0.0);

        long cor3 = th2.correctDelay(20);
        long cor4 = th2.correctDelay(3);

        Random rnd = new Random();
        for (int i = 0; i < 1000; i++) {
            th2.correctDelay(rnd.nextInt(3));
            Thread.sleep(rnd.nextInt(8));
        }
        assertEquals(th2.getThroughput(), 100, 0.0);

        for (int i = 0; i < 100; i++) {
            th2.correctDelay(0);
        }
        assertEquals(th2.getThroughput(), 100, 0.0);


        Throughput th3 = new Throughput(400, 40, 10);
        for (int i = 0; i < 100 ; i++) {
            int r = rnd.nextInt(4);
            long cor = th3.correctDelay(r);
            int r2 = rnd.nextInt(2);
            System.out.println("delay:" + r + ", cor delay:" + cor +  ",  throughput:" + th3.getThroughput());
            Thread.sleep(r2);
        }

    }
}