package de.unihamburg.sickstore.backend.anomaly.clientdelay;

import de.unihamburg.sickstore.backend.timer.SystemTimeHandler;
import de.unihamburg.sickstore.backend.timer.TimeHandler;
import de.unihamburg.sickstore.database.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by Steffen Friedrich on 05.11.2015.
 */
public class Throughput {

    private double maxThroughput = 0;

    private long outstanding = 0;

    private long lastOPReceivedAt = 0;

    private int hickupEvery = 0;

    private int hickupTime = 0;

    private long hickupDuration = 0;


    @SuppressWarnings("unused")
    public static Throughput newInstanceFromConfig(Map<String, Object> config) {
        Throughput throughput = new Throughput();
        if (config.get("max") != null) {
            throughput.setMaxThroughput(((double) config.get("max")) / 1000);
        }
        if (config.get("hickupEvery") != null) {
            throughput.setHickupEvery((int) config.get("hickupEvery"));
        }
        if (config.get("hickupDuration") != null) {
            throughput.setHickupDuration((int) config.get("hickupDuration"));
        }
        return throughput;
    }

    public Throughput() {

    }

    public Throughput(double maxThroughput) {
        this.maxThroughput = maxThroughput;
    }

    public Throughput(double maxThroughput, int hickupEvery, int hickupDuration) {
        this.maxThroughput = maxThroughput / 1000;
        this.hickupEvery = hickupEvery;
        this.hickupDuration = hickupDuration;
    }

    private static final Logger log = LoggerFactory.getLogger("sickstore");

    public double getQueueingLatency(long receivedAt) {
        if (maxThroughput > 0) {
            if (outstanding == 0) {
                outstanding++;
                lastOPReceivedAt = receivedAt;
                return 0.0;
            } else {
                long idleTime = receivedAt - lastOPReceivedAt;
                hickupTime += idleTime;
                if(hickupEvery > 0 && hickupTime > hickupEvery) {
                    outstanding += (maxThroughput * hickupDuration);
                    hickupEvery = 0;
                    hickupTime = 0;
                }
                long consumedOPs = (long) Math.floor(maxThroughput * idleTime);
                outstanding = Math.max(0, outstanding - consumedOPs);
                double latency =  outstanding / maxThroughput;
                if(consumedOPs > 0) {
                    lastOPReceivedAt = receivedAt;
                }
                outstanding++;
                return latency;
            }
        } else {
            return 0.0;
        }
    }


    public void setMaxThroughput(double maxThroughput) {
        this.maxThroughput = maxThroughput;
    }

    public double getMaxThroughput() {
        return maxThroughput;
    }


    public long getHickupDuration() {
        return hickupDuration;
    }

    public void setHickupDuration(long hickupDuration) {
        this.hickupDuration = hickupDuration;
    }

    public int getHickupEvery() {
        return hickupEvery;
    }

    public void setHickupEvery(int hickupEvery) {
        this.hickupEvery = hickupEvery;
    }

}
