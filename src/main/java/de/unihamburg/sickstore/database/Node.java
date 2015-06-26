package de.unihamburg.sickstore.database;

public class Node {

    private int id;
    private String name;

    public Node() {
    }

    public Node(int id) {
        this.id = id;
    }

    public Node(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
