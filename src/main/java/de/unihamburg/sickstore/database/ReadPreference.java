package de.unihamburg.sickstore.database;

import java.util.ArrayList;
import java.util.List;

public class ReadPreference {
    public static final String PRIMARY = "primary";
    public static final String SECONDARY = "secondary";
    private String name = PRIMARY;

    private List<String> tagSetList = null;

    public ReadPreference() {
    }

    public ReadPreference(String name) {
        this.name = name;
    }

    public ReadPreference(String name, String tagSet) {
        this.name = name;
        this.tagSetList = new ArrayList();
        this.tagSetList.add(tagSet);
    }

    public ReadPreference(String name, List<String> tagSetList) {
        this.name = name;
        this.tagSetList = tagSetList;
    }

    public boolean isSlaveOk() {
        if(name.equals(SECONDARY))
        {
            return true;
        }
        return false;
    }

    public List<String> getTagSetList() {
        return this.tagSetList;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

}
