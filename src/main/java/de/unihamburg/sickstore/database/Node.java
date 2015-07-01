package de.unihamburg.sickstore.database;

import java.util.HashSet;
import java.util.Set;

public class Node {

    private String name;
    private Set<String> tags = new HashSet<>();
    private boolean primary = false;

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
}
