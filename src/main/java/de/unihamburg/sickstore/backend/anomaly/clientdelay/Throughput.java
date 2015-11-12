package de.unihamburg.sickstore.backend.anomaly.clientdelay;
import java.util.Map;

/**
 * Created by Steffen Friedrich on 05.11.2015.
 */
public class Throughput {

    private double maxThroughput = 0;

    private int hickupAfter = 0;

    private int hickupTime = 0;

    private long hickupDuration = 0;


    @SuppressWarnings("unused")
    public static Throughput newInstanceFromConfig(Map<String, Object> config) {
        Throughput throughput = new Throughput();
        if (config.get("max") != null) {
            throughput.setMaxThroughput(((double) config.get("max")) / 1000);
        }
        if (config.get("hickupAfter") != null) {
            throughput.setHickupAfter((int) config.get("hickupAfter"));
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
        this.hickupAfter = hickupEvery;
        this.hickupDuration = hickupDuration;
    }

    private double outstanding = 0;

    private long lastOPReceivedAt = 0;

    public double getQueueingLatency(long receivedAt) {
        if (maxThroughput > 0) {
            if (outstanding == 0) {
                outstanding++;
                lastOPReceivedAt = receivedAt;
                return 0.0;
            } else {
                long idleTime = receivedAt - lastOPReceivedAt;
                hickupTime += idleTime;
                if(hickupAfter > 0 && hickupTime > hickupAfter) {
                    outstanding += maxThroughput * hickupDuration;
                    hickupAfter = 0;
                    hickupTime = 0;
                }
                double consumedOPs =  maxThroughput * idleTime;
                if(consumedOPs > 0.0) {
                    lastOPReceivedAt = receivedAt;
                    outstanding = Math.max(0, outstanding - consumedOPs);
                }
                double latency = outstanding / maxThroughput;

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

    public int getHickupAfter() {
        return hickupAfter;
    }

    public void setHickupAfter(int hickupEvery) {
        this.hickupAfter = hickupEvery;
    }

}
