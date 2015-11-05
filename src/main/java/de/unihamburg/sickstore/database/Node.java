package de.unihamburg.sickstore.database;

import de.unihamburg.sickstore.backend.anomaly.clientdelay.Throughput;

import java.util.*;

public class Node {

    private String name = "primary";
    private Set<String> tags = new HashSet<>();
    private boolean primary = false;
    private long  clientLatency = 0;
    private Throughput throughput = new Throughput();

    @SuppressWarnings("unused")
    public static Node newInstanceFromConfig(Map<String, Object> config) {
        Node node = new Node();

        if (config.get("name") != null) {
            node.setName((String) config.get("name"));
        }
        if (config.get("primary") != null) {
            node.setPrimary((Boolean) config.get("primary"));
        }
        if (config.get("clientLatency") != null) {
            node.setClientLatency(((int) config.get("clientLatency")));
        }
        if (config.get("throughput") != null) {
            Map throughputConfig = (LinkedHashMap) config.get("throughput");
            node.setThroughput(Throughput.newInstanceFromConfig(throughputConfig));
         }
        if (config.get("tags") != null) {
            List<String> tags = (List<String>) config.get("tags");
            node.setTags(new HashSet<>(tags));
        }
        return node;
    }

    public Node() {
        this.name = super.toString();
    }

    public Node(String name) {
        this.name = name;
    }

    public Node(String name, Set<String> tags) {
        this.name = name;
        this.tags = tags;
    }

    public Node(String name, Set<String> tags, boolean primary) {
        this.name = name;
        this.tags = tags;
        this.primary = primary;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public String toString() {
        return "Node " + name;
    }

    public long getClientLatency() {
        return clientLatency;
    }

    public void setClientLatency(long clientLatency) {
        this.clientLatency = clientLatency;
    }

    public void setThroughput(Throughput throughput) {
        this.throughput = throughput;
    }
    public Throughput getThroughput() {
        return throughput;
    }
}
