package de.unihamburg.sickstore.database;

public class Node {

    private String name;
    private boolean primary = false;

    public Node() {
        this.name = super.toString();
    }

    public Node(String name) {
        this.name = name;
    }

    public Node(String name, boolean primary) {
        this.name = name;
        this.primary = primary;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
