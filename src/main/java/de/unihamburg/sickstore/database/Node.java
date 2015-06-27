package de.unihamburg.sickstore.database;

public class Node {

    private String name;

    public Node() {
        this.name = super.toString();
    }

    public Node(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return "Node " + name;
    }
}
