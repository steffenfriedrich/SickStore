package de.unihamburg.sickstore.backend.anomaly.clientdelay;

import java.util.Map;

/**
 * Created by Steffen Friedrich on 05.11.2015.
 */
public class Throughput {
    // actual ops per second
    private double throughput = 0;

    private int time = 0;

    private int nextHickupTime = 0;

    private int operations = 0;

    private long lastOpsTimestamp = 0;


    // max ops per second (0 = unlimited)
    private double maxThroughput = 0;

    private double hickupMaxThroughput = 0;

    private int hickupEvery = 0;

    private int hickupDuration = 0;




    @SuppressWarnings("unused")
    public static Throughput newInstanceFromConfig(Map<String, Object> config) {
        Throughput throughput = new Throughput();
        if (config.get("max") != null) {
            throughput.setMaxThroughput((double) config.get("max"));
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
        this.hickupMaxThroughput = maxThroughput;
    }

    public Throughput(double maxThroughput, int hickupEvery, int hickupDuration ) {
        this.maxThroughput = maxThroughput;
        this.hickupEvery = hickupEvery;
        this.hickupDuration = hickupDuration;
        this.hickupMaxThroughput = maxThroughput;
    }


    public long correctDelay(long delay){
        long currentTime = System.currentTimeMillis();
        operations += 1;
        double actualThroughput = 1000.0 * operations / (time + delay);
        long correctedDelay = delay;
        if(actualThroughput > hickupMaxThroughput)
        {
            correctedDelay = ((int) Math.ceil(1000.0 * operations / hickupMaxThroughput)) - time;
        }
        long realTimeDelay = correctedDelay;
        long timeBetweenOps = currentTime - lastOpsTimestamp;
        if(lastOpsTimestamp > 0 && timeBetweenOps > correctedDelay) {
            realTimeDelay = timeBetweenOps;
        }
        time += realTimeDelay;
        throughput = 1000.0 * operations / time;
        lastOpsTimestamp = currentTime;

        // TODO hickup(realTimeDelay);
        return correctedDelay;
    }

    public void hickup(long delay) {
        if(hickupEvery > 0 && (nextHickupTime + delay) > hickupEvery) {
            System.out.println("hickup");
            nextHickupTime = 0;
        } else {
            nextHickupTime += delay;
        }
    }

    public double getThroughput() {
        return throughput;
    }

    public void setMaxThroughput(double maxThroughput) {
        this.maxThroughput = maxThroughput;
    }

    public double getMaxThroughput() {
        return maxThroughput;
    }

    public int getOperations() {
        return operations;
    }

    public int getMilliseconds() {
        return time;
    }

    public int getHickupDuration() {
        return hickupDuration;
    }

    public void setHickupDuration(int hickupDuration) {
        this.hickupDuration = hickupDuration;
    }

    public int getHickupEvery() {
        return hickupEvery;
    }

    public void setHickupEvery(int hickupEvery) {
        this.hickupEvery = hickupEvery;
    }
}
