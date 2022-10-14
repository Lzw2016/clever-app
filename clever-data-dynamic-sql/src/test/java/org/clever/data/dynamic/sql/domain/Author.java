package org.clever.data.dynamic.sql.domain;

import lombok.Data;

import java.io.Serializable;

@Data
public class Author implements Serializable {

    protected int id;
    protected String username;
    protected String password;
    protected String email;
    protected String bio;
    protected Section favouriteSection;

    public Author(Integer id, String username, String password, String email, String bio, Section section) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.bio = bio;
        this.favouriteSection = section;
    }

    public Author(int id) {
        this(id, null, null, null, null, null);
    }

    public Author() {
        this(-1, null, null, null, null, null);
    }

    public void setLongId(Long id) {
        this.id = id.intValue();
    }
}