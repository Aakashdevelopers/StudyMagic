package com.amstudio.studymagic.models;

import java.io.Serializable;

public class Topic implements Serializable, Listable {
    private String id;
    private String name;

    public Topic(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }
}