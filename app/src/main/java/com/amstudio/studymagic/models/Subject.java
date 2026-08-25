package com.amstudio.studymagic.models;

import java.io.Serializable;

public class Subject implements Serializable, Listable {
    private String id;
    private String name;

    public Subject(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }
}