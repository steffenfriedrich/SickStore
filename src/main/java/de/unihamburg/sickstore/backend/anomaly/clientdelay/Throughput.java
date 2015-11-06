package de.unihamburg.sickstore.backend.anomaly.clientdelay;

import de.unihamburg.sickstore.database.Node;

import java.util.Map;

/**
 * Created by Steffen Friedrich on 05.11.2015.
 */
public class Throughput {

    private double maxThroughput = 0;

    private long outstanding = 0;

    private long lastOPReceivedAt = 0;

    private int hickupEvery = 0;

    private long hickupDuration = 0;


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
    }

    public Throughput(double maxThroughput, int hickupEvery, int hickupDuration) {
        this.maxThroughput = maxThroughput / 1000;
        this.hickupEvery = hickupEvery;
        this.hickupDuration = hickupDuration;
    }

    public double getQueueingLatency(long receivedAt) {
        if (maxThroughput > 0) {
            if (outstanding == 0) {
                outstanding++;
                return 0.0;
            } else {
                long idleTime = receivedAt - lastOPReceivedAt;
                long consumedOPs = (long) Math.floor(maxThroughput * idleTime);
                outstanding = Math.max(0, outstanding - consumedOPs);
                lastOPReceivedAt = receivedAt;
                double latency =  outstanding / maxThroughput;
                return latency;
            }
        } else {
            return 0.0;
        }
    }


//    public long correctDelay(long receivedAt, long delay){
//            operations += 1;
//            double actualThroughput = 1000.0 * operations / (time + delay);
//            long correctedDelay = delay;
//            if (maxThroughput > 0 && operations > 1 && actualThroughput > maxThroughput) {
//                correctedDelay = ((int) Math.ceil(1000.0 * operations / maxThroughput)) - time;
//            }
//            long actualTimeDelay = correctedDelay;
//            long timeBetweenRequests = 1;
//            if(lastOpTimestamp > 0)  {
//               timeBetweenRequests = receivedAt - lastOpTimestamp;
//                if (timeBetweenRequests > correctedDelay) {
//                    actualTimeDelay = timeBetweenRequests;
//                }
//            }
//
//            // hickup handling ToDo Hickup
//            if(hickupEvery > 0 && ((hickuptime + timeBetweenRequests) > hickupEvery) && hickupDurationCountdown > 0) {
//                if(hickupDurationCountdown == hickupDuration) {
//                    hickupoperations = operations - 1;
//                }
//                System.out.println("hickup mode");
//
//                correctedDelay = correctedDelay + hickupDurationCountdown;
//                hickuptime += actualTimeDelay + hickupDurationCountdown;
//                time += actualTimeDelay + hickupDurationCountdown;
//                hickupDurationCountdown -=  timeBetweenRequests;
//                throughput = 1000.0 * hickupoperations / time;
//
//                if(hickupDurationCountdown <= 0)
//                {
//                    hickuptime = 0;
//                    hickupDurationCountdown = hickupDuration;
//                }
//            } else {
//                time += actualTimeDelay;
//                hickuptime += actualTimeDelay;
//                throughput = 1000.0 * operations / time;
//            }
//
//            lastOpTimestamp = receivedAt;
//            // client should wait corrected delay, even if the time between requests  determines the current time
//            return correctedDelay;
//    }


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
